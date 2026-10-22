package main

import (
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"
	"time"
)

func TestPasswordHash(t *testing.T) {
	h, err := HashPassword("correct horse battery", 20000)
	if err != nil {
		t.Fatal(err)
	}
	if strings.ContainsAny(h, "$'\" ") {
		t.Fatalf("hash contains characters env files mangle: %s", h)
	}
	p, err := parseHash(h)
	if err != nil || !p.verify("correct horse battery") || p.verify("correct horse batterY") {
		t.Fatal("verify")
	}
	for _, bad := range []string{"", "plain", "pbkdf2-sha256:5:aa:bb", "bcrypt:1:2:3"} {
		if _, err := parseHash(bad); err == nil {
			t.Fatalf("accepted %q", bad)
		}
	}
}

func testServer(t *testing.T) (*httptest.Server, *Engine, *Web) {
	t.Helper()
	h, _ := HashPassword("secret password!", 20000)
	auth, err := NewBasicAuth("ops", h)
	if err != nil {
		t.Fatal(err)
	}
	auth.FailDelay = time.Millisecond
	d := newFakeDocker()
	e := newTestEngine(t, d, t.TempDir())
	w := NewWeb(e, "test")
	srv := httptest.NewServer(auth.Wrap(w.Handler()))
	t.Cleanup(srv.Close)
	return srv, e, w
}

func noRedirect() *http.Client {
	return &http.Client{CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}
}

func TestEveryRouteRequiresAuth(t *testing.T) {
	srv, _, _ := testServer(t)
	c := noRedirect()
	for _, r := range []struct{ method, path string }{
		{"GET", "/"}, {"GET", "/status.json"}, {"GET", "/nope"}, {"POST", "/apply/optimizer"},
		{"POST", "/apply/versions"}, {"POST", "/action/up"}, {"POST", "/action/down"}, {"POST", "/action/restart"},
		{"GET", "/favicon.ico"}, {"DELETE", "/"},
	} {
		for _, creds := range [][2]string{{}, {"ops", "wrong"}, {"admin", "secret password!"}} {
			req, _ := http.NewRequest(r.method, srv.URL+r.path, nil)
			if creds[0] != "" {
				req.SetBasicAuth(creds[0], creds[1])
			}
			resp, err := c.Do(req)
			if err != nil {
				t.Fatal(err)
			}
			resp.Body.Close()
			if resp.StatusCode != http.StatusUnauthorized {
				t.Fatalf("%s %s with %v: %d", r.method, r.path, creds, resp.StatusCode)
			}
		}
	}
	req, _ := http.NewRequest("GET", srv.URL+"/", nil)
	req.SetBasicAuth("ops", "secret password!")
	resp, err := c.Do(req)
	if err != nil || resp.StatusCode != 200 {
		t.Fatalf("%v %v", err, resp)
	}
}

func post(t *testing.T, srv *httptest.Server, path string, form url.Values) *http.Response {
	t.Helper()
	req, _ := http.NewRequest("POST", srv.URL+path, strings.NewReader(form.Encode()))
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	req.SetBasicAuth("ops", "secret password!")
	resp, err := noRedirect().Do(req)
	if err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
	return resp
}

func TestFormsNeedTokenAndConfirmation(t *testing.T) {
	srv, e, w := testServer(t)
	form := url.Values{"optimizerInstances": {"4"}, "optimizerThreads": {"8"}, "optimizerHeapGiB": {"8"}}
	resp := post(t, srv, "/apply/optimizer", form)
	if !strings.Contains(resp.Header.Get("Location"), "err=") {
		t.Fatalf("no token accepted: %s", resp.Header.Get("Location"))
	}
	form.Set("csrf", w.csrf)
	resp = post(t, srv, "/apply/optimizer", form)
	if loc := resp.Header.Get("Location"); !strings.Contains(loc, "confirmation") {
		t.Fatalf("no confirmation accepted: %s", loc)
	}
	form.Set("confirm", "yes")
	form.Set("optimizerInstances", "99")
	resp = post(t, srv, "/apply/optimizer", form)
	if loc := resp.Header.Get("Location"); !strings.Contains(loc, "between") {
		t.Fatalf("out of range accepted: %s", loc)
	}
	form.Set("optimizerInstances", "4x")
	resp = post(t, srv, "/apply/optimizer", form)
	if loc := resp.Header.Get("Location"); !strings.Contains(loc, "whole+number") {
		t.Fatalf("garbage accepted: %s", loc)
	}
	form.Set("optimizerInstances", "4")
	resp = post(t, srv, "/apply/optimizer", form)
	if loc := resp.Header.Get("Location"); !strings.Contains(loc, "msg=") {
		t.Fatalf("valid change refused: %s", loc)
	}
	e.Wait()
	st, _, _ := e.Snapshot()
	if st.Desired.OptimizerInstances != 4 {
		t.Fatalf("%+v", st.Desired)
	}
	// Cross-origin posts are refused even with a token.
	req, _ := http.NewRequest("POST", srv.URL+"/action/up", strings.NewReader(url.Values{"csrf": {w.csrf}, "confirm": {"yes"}}.Encode()))
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	req.Header.Set("Origin", "http://evil.example")
	req.SetBasicAuth("ops", "secret password!")
	r2, _ := noRedirect().Do(req)
	if !strings.Contains(r2.Header.Get("Location"), "cross-origin") {
		t.Fatalf("cross-origin accepted: %s", r2.Header.Get("Location"))
	}
}

func TestOnlyKnownFormGroupsAndActions(t *testing.T) {
	srv, _, w := testServer(t)
	for _, p := range []string{"/apply/shell", "/action/exec", "/action/prune"} {
		resp := post(t, srv, p, url.Values{"csrf": {w.csrf}, "confirm": {"yes"}})
		if resp.StatusCode != http.StatusNotFound {
			t.Fatalf("%s: %d", p, resp.StatusCode)
		}
	}
}
