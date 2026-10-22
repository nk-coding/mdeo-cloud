package main

import (
	"context"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	_ "embed"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"html/template"
	"net/http"
	"net/url"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"
)

//go:embed page.html.tmpl
var pageTemplate string

// Web serves the one status page and the forms. Nothing else: no logs, no files, no shell.
type Web struct {
	Engine  *Engine
	Version string
	csrf    string

	psMu   sync.Mutex
	psAt   time.Time
	psList []Container
	psErr  error

	page *template.Template
}

// NewWeb prepares the handlers.
func NewWeb(e *Engine, version string) *Web {
	key := make([]byte, 32)
	_, _ = rand.Read(key)
	m := hmac.New(sha256.New, key)
	m.Write([]byte("csrf-v1"))
	w := &Web{Engine: e, Version: version, csrf: hex.EncodeToString(m.Sum(nil))}
	w.page = template.Must(template.New("page").Funcs(template.FuncMap{
		"ts": func(t time.Time) string {
			if t.IsZero() {
				return ""
			}
			return t.UTC().Format("2006-01-02 15:04:05 UTC")
		},
		"gib": func(b int64) string { return fmt.Sprintf("%.0f GiB", float64(b)/(1<<30)) },
		"dur": func(d time.Duration) string { return d.String() },
		"val": func(l Limit, s Settings) int { return l.Get(s) },
		"group": func(prefix string) []Limit {
			var out []Limit
			for _, l := range Limits {
				if strings.HasPrefix(l.Key, prefix) {
					out = append(out, l)
				}
			}
			return out
		},
	}).Parse(pageTemplate))
	return w
}

// Handler returns all routes; the caller wraps them in basic auth.
func (w *Web) Handler() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /{$}", w.index)
	mux.HandleFunc("GET /status.json", w.statusJSON)
	mux.HandleFunc("POST /apply/{group}", w.apply)
	mux.HandleFunc("POST /action/{name}", w.action)
	return http.HandlerFunc(func(rw http.ResponseWriter, r *http.Request) {
		h := rw.Header()
		h.Set("X-Frame-Options", "DENY")
		h.Set("X-Content-Type-Options", "nosniff")
		h.Set("Referrer-Policy", "no-referrer")
		h.Set("Cache-Control", "no-store")
		h.Set("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'")
		mux.ServeHTTP(rw, r)
	})
}

func (w *Web) containers() ([]Container, error) {
	w.psMu.Lock()
	defer w.psMu.Unlock()
	if time.Since(w.psAt) < 3*time.Second {
		return w.psList, w.psErr
	}
	ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancel()
	w.psList, w.psErr = w.Engine.Docker.Ps(ctx, w.Engine.ComposePath())
	w.psAt = time.Now()
	sort.Slice(w.psList, func(i, j int) bool { return w.psList[i].Service < w.psList[j].Service })
	return w.psList, w.psErr
}

type statusView struct {
	State      State
	Current    *Result
	Busy       bool
	Containers []Container
	PsError    string
	Health     string
	Healthy    bool
	Host       Host
	Pinning    []NumaNode
	Warnings   []string
	Problems   []string
	Version    string
	Heal       HealPolicy
	Healing    bool
}

func (w *Web) status() statusView {
	st, cur, busy := w.Engine.Snapshot()
	cs, err := w.containers()
	v := statusView{State: st, Current: cur, Busy: busy, Containers: cs, Host: w.Engine.Host,
		Pinning: PinningNodes(st.Desired, w.Engine.Host), Warnings: st.Desired.Warnings(),
		Problems: st.Desired.Validate(), Version: w.Version, Heal: w.Engine.Heal,
		Healing: cur != nil && cur.Action == "auto-heal"}
	if err != nil {
		v.PsError = err.Error()
	}
	switch {
	case err != nil:
		v.Health = "unknown (docker compose ps failed)"
	case !st.Running && len(cs) == 0:
		v.Health = "down"
	case !st.Running:
		v.Health = "should be down, but containers exist"
	default:
		if herr := Health(cs, ExpectedServices(st.Desired, w.Engine.Cfg)); herr != nil {
			v.Health = "not healthy: " + herr.Error()
		} else {
			v.Health, v.Healthy = "healthy", true
		}
	}
	return v
}

func (w *Web) index(rw http.ResponseWriter, r *http.Request) {
	v := w.status()
	data := struct {
		statusView
		CSRF            string
		Msg, Err        string
		Limits          []Limit
		OptimizerJava   string
		SimCapacity     int
		OptimizerDemand int
	}{v, w.csrf, r.URL.Query().Get("msg"), r.URL.Query().Get("err"), Limits,
		OptimizerJavaOptions(v.State.Desired), v.State.Desired.SimInstances * v.State.Desired.SimThreads,
		v.State.Desired.OptimizerInstances * v.State.Desired.OptimizerThreads}
	rw.Header().Set("Content-Type", "text/html; charset=utf-8")
	if err := w.page.Execute(rw, data); err != nil {
		w.Engine.Logf("rendering the page: %v", err)
	}
}

func (w *Web) statusJSON(rw http.ResponseWriter, r *http.Request) {
	v := w.status()
	rw.Header().Set("Content-Type", "application/json")
	enc := json.NewEncoder(rw)
	enc.SetIndent("", "  ")
	_ = enc.Encode(map[string]interface{}{
		"running": v.State.Running, "desired": v.State.Desired, "lastKnownGood": v.State.LastKnownGood,
		"journal": v.State.Journal, "lastResult": v.State.LastResult, "current": v.Current, "busy": v.Busy,
		"health": v.Health, "healthy": v.Healthy, "containers": v.Containers, "version": v.Version,
		"heals": v.State.Heals, "healCount": v.State.HealCount,
		"autoHeal": map[string]interface{}{"enabled": v.Heal.Enabled, "intervalSeconds": v.Heal.Interval.Seconds(),
			"graceSeconds": v.Heal.Grace.Seconds(), "backoffBaseSeconds": v.Heal.BackoffBase.Seconds(),
			"backoffMaxSeconds": v.Heal.BackoffMax.Seconds()},
	})
}

// checkPost enforces the form token, a same-origin request and, for every action here, the
// confirmation box.
func (w *Web) checkPost(r *http.Request) error {
	if o := r.Header.Get("Origin"); o != "" && o != "null" {
		u, err := url.Parse(o)
		if err != nil || u.Host != r.Host {
			return errors.New("cross-origin request refused")
		}
	}
	if !hmac.Equal([]byte(r.PostFormValue("csrf")), []byte(w.csrf)) {
		return errors.New("the form is out of date (the manager restarted?); reload the page and try again")
	}
	if r.PostFormValue("confirm") != "yes" {
		return errors.New("tick the confirmation box to run this action")
	}
	return nil
}

func redirect(rw http.ResponseWriter, r *http.Request, msg string, err error) {
	q := url.Values{}
	if err != nil {
		q.Set("err", err.Error())
	} else {
		q.Set("msg", msg)
	}
	http.Redirect(rw, r, "/?"+q.Encode(), http.StatusSeeOther)
}

// formGroups names the fields each form may change.
var formGroups = map[string][]string{
	"optimizer": {"optimizerInstances", "optimizerThreads", "optimizerHeapGiB"},
	"simulator": {"simInstances", "simThreads", "simMaxSessions", "simResultCacheSize", "simCacheMB", "simTimeoutSeconds"},
	"versions":  {"mdeoTag", "simTag"},
	"placement": {"numaPinning"},
}

func (w *Web) apply(rw http.ResponseWriter, r *http.Request) {
	r.Body = http.MaxBytesReader(rw, r.Body, 64<<10)
	group := r.PathValue("group")
	fields, ok := formGroups[group]
	if !ok {
		http.NotFound(rw, r)
		return
	}
	if err := w.checkPost(r); err != nil {
		redirect(rw, r, "", err)
		return
	}
	st, _, _ := w.Engine.Snapshot()
	s := st.Desired
	for _, f := range fields {
		raw := strings.TrimSpace(r.PostFormValue(f))
		switch f {
		case "mdeoTag":
			s.MdeoTag = raw
		case "simTag":
			s.SimTag = raw
		case "numaPinning":
			s.NumaPinning = raw == "on"
		default:
			l, _ := LimitByKey(f)
			n, err := strconv.Atoi(raw)
			if err != nil {
				redirect(rw, r, "", fmt.Errorf("%s: %q is not a whole number", l.Label, raw))
				return
			}
			l.Set(&s, n)
		}
	}
	if err := w.Engine.Apply("change "+group, s); err != nil {
		redirect(rw, r, "", err)
		return
	}
	redirect(rw, r, "Started: change "+group, nil)
}

func (w *Web) action(rw http.ResponseWriter, r *http.Request) {
	r.Body = http.MaxBytesReader(rw, r.Body, 64<<10)
	var run func() error
	switch r.PathValue("name") {
	case "restart":
		run = w.Engine.Restart
	case "down":
		run = w.Engine.Down
	case "up":
		run = w.Engine.Up
	default:
		http.NotFound(rw, r)
		return
	}
	if err := w.checkPost(r); err != nil {
		redirect(rw, r, "", err)
		return
	}
	if err := run(); err != nil {
		redirect(rw, r, "", err)
		return
	}
	redirect(rw, r, "Started: "+r.PathValue("name"), nil)
}
