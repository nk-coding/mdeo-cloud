package main

import (
	"context"
	"encoding/json"
	"errors"
	"os"
	"strings"
	"sync"
	"testing"
	"time"
)

// fakeDocker is an in-memory Docker: Up starts one container per expected service of the
// settings embedded in the compose file, healthy unless its image is marked sick.
type fakeDocker struct {
	mu                sync.Mutex
	present           map[string]bool
	pullFail          map[string]bool
	sick              map[string]bool
	configErr         error
	upErr             error
	pingErr           error
	running           map[string]Container
	ups, downs, pulls int
	restarts          []string
	// afterRestart is the health a restarted container reports (default "starting").
	afterRestart string
	// blockUp, when set, is waited on inside Up (to hold an action open).
	blockUp chan struct{}
}

func newFakeDocker() *fakeDocker {
	return &fakeDocker{present: map[string]bool{}, pullFail: map[string]bool{}, sick: map[string]bool{}, running: map[string]Container{}}
}

func settingsOf(file string) (Settings, error) {
	b, err := os.ReadFile(file)
	if err != nil {
		return Settings{}, err
	}
	for _, l := range strings.Split(string(b), "\n") {
		if rest, ok := strings.CutPrefix(l, "# Settings: "); ok {
			var s Settings
			err := json.Unmarshal([]byte(rest), &s)
			return s, err
		}
	}
	return Settings{}, errors.New("no settings line")
}

func (f *fakeDocker) Ping(ctx context.Context) error { return f.pingErr }

func (f *fakeDocker) Config(ctx context.Context, file string) ([]string, error) {
	if f.configErr != nil {
		return nil, f.configErr
	}
	s, err := settingsOf(file)
	if err != nil {
		return nil, err
	}
	seen := map[string]bool{}
	var imgs []string
	for _, img := range ExpectedServices(s, testCfg) {
		if !seen[img] {
			seen[img] = true
			imgs = append(imgs, img)
		}
	}
	return imgs, nil
}

func (f *fakeDocker) ImagePresent(ctx context.Context, image string) (bool, error) {
	f.mu.Lock()
	defer f.mu.Unlock()
	return f.present[image], nil
}

func (f *fakeDocker) Pull(ctx context.Context, image string) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	f.pulls++
	if f.pullFail[image] {
		return errors.New("manifest unknown")
	}
	f.present[image] = true
	return nil
}

func (f *fakeDocker) Up(ctx context.Context, file string, recreate bool) error {
	if f.blockUp != nil {
		<-f.blockUp
	}
	s, err := settingsOf(file)
	if err != nil {
		return err
	}
	f.mu.Lock()
	defer f.mu.Unlock()
	f.ups++
	if f.upErr != nil {
		return f.upErr
	}
	next := map[string]Container{}
	for svc, img := range ExpectedServices(s, testCfg) {
		h := "healthy"
		if f.sick[img] {
			h = "unhealthy"
		}
		next[svc] = Container{Name: "mdeo-" + svc + "-1", Service: svc, State: "running", Health: h, Image: img}
	}
	f.running = next
	return nil
}

func (f *fakeDocker) Down(ctx context.Context, file string) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	f.downs++
	f.running = map[string]Container{}
	return nil
}

func (f *fakeDocker) Ps(ctx context.Context, file string) ([]Container, error) {
	f.mu.Lock()
	defer f.mu.Unlock()
	var cs []Container
	for _, c := range f.running {
		cs = append(cs, c)
	}
	return cs, nil
}

func (f *fakeDocker) images() map[string]string {
	f.mu.Lock()
	defer f.mu.Unlock()
	m := map[string]string{}
	for s, c := range f.running {
		m[s] = c.Image
	}
	return m
}

func newTestEngine(t *testing.T, d *fakeDocker, dir string) *Engine {
	t.Helper()
	e, err := NewEngine(dir, d, testCfg, Host{}, testSettings())
	if err != nil {
		t.Fatal(err)
	}
	e.HealthTimeout, e.PollInterval = 200*time.Millisecond, 5*time.Millisecond
	return e
}

func lastResult(t *testing.T, e *Engine) Result {
	t.Helper()
	e.Wait()
	st, _, _ := e.Snapshot()
	if st.LastResult == nil {
		t.Fatal("no result")
	}
	return *st.LastResult
}

// running brings a fresh engine's stack up with the default test settings.
func runningEngine(t *testing.T) (*Engine, *fakeDocker) {
	d := newFakeDocker()
	e := newTestEngine(t, d, t.TempDir())
	if err := e.Up(); err != nil {
		t.Fatal(err)
	}
	if r := lastResult(t, e); !r.OK {
		t.Fatalf("up failed: %+v", r)
	}
	return e, d
}

func TestUpRecordsLastKnownGood(t *testing.T) {
	e, d := runningEngine(t)
	st, _, _ := e.Snapshot()
	if !st.Running || st.LastKnownGood == nil || *st.LastKnownGood != testSettings() || st.Journal != nil {
		t.Fatalf("%+v", st)
	}
	if len(d.images()) != len(ExpectedServices(testSettings(), testCfg)) {
		t.Fatal("not all services up")
	}
}

func TestScaleUpAndDown(t *testing.T) {
	e, d := runningEngine(t)
	for _, n := range []int{8, 3, 1} {
		s := testSettings()
		s.OptimizerInstances, s.OptimizerThreads = n, 4*n
		if err := e.Apply("change optimizer", s); err != nil {
			t.Fatal(err)
		}
		if r := lastResult(t, e); !r.OK {
			t.Fatalf("n=%d: %+v", n, r)
		}
		imgs := d.images()
		for i := 0; i < 10; i++ {
			_, ok := imgs[OptimizerName(i)]
			if ok != (i < n) {
				t.Fatalf("n=%d: optimizer %d present=%v", n, i, ok)
			}
		}
		b, _ := os.ReadFile(e.ComposePath())
		if !strings.Contains(string(b), `WORKER_THREADS: "`+itoa(4*n)+`"`) {
			t.Fatalf("threads not in the compose file")
		}
		st, _, _ := e.Snapshot()
		if *st.LastKnownGood != s {
			t.Fatal("last known good not updated")
		}
	}
}

func itoa(i int) string { b, _ := json.Marshal(i); return string(b) }

func TestPullFailureChangesNothing(t *testing.T) {
	e, d := runningEngine(t)
	before, ups := d.images(), d.ups
	s := testSettings()
	s.MdeoTag = "does-not-exist"
	d.pullFail["ghcr.io/nk-coding/mdeo-backend:does-not-exist"] = true
	if err := e.Apply("change versions", s); err != nil {
		t.Fatal(err)
	}
	r := lastResult(t, e)
	if r.OK || !strings.Contains(r.Message, "Nothing was changed") || !strings.Contains(r.Message, "pulling") {
		t.Fatalf("%+v", r)
	}
	st, _, _ := e.Snapshot()
	if st.Desired != testSettings() || st.Journal != nil || d.ups != ups {
		t.Fatalf("state changed: %+v ups %d→%d", st, ups, d.ups)
	}
	for k, v := range before {
		if d.images()[k] != v {
			t.Fatal("containers changed")
		}
	}
}

func TestInvalidConfigChangesNothing(t *testing.T) {
	e, d := runningEngine(t)
	ups := d.ups
	d.configErr = errors.New("required variable DATABASE_PASSWORD is missing a value")
	s := testSettings()
	s.SimThreads = 32
	_ = e.Apply("change simulator", s)
	r := lastResult(t, e)
	if r.OK || !strings.Contains(r.Message, "invalid") || d.ups != ups {
		t.Fatalf("%+v", r)
	}
}

func TestUnhealthyVersionRollsBack(t *testing.T) {
	e, d := runningEngine(t)
	s := testSettings()
	s.MdeoTag = "sick"
	d.sick["ghcr.io/nk-coding/mdeo-backend:sick"] = true
	_ = e.Apply("change versions", s)
	r := lastResult(t, e)
	if r.OK || !strings.Contains(r.Message, "rolled back") || !strings.Contains(r.Message, "healthy again") {
		t.Fatalf("%+v", r)
	}
	st, _, _ := e.Snapshot()
	if st.Desired != testSettings() || *st.LastKnownGood != testSettings() || st.Journal != nil || !st.Running {
		t.Fatalf("%+v", st)
	}
	if d.images()["backend"] != "ghcr.io/nk-coding/mdeo-backend:1.2.3" {
		t.Fatalf("backend runs %s", d.images()["backend"])
	}
}

func TestFirstDeployUnhealthyHasNothingToRollBackTo(t *testing.T) {
	d := newFakeDocker()
	d.sick["ghcr.io/nk-coding/mdeo-backend:1.2.3"] = true
	e := newTestEngine(t, d, t.TempDir())
	_ = e.Up()
	r := lastResult(t, e)
	if r.OK || !strings.Contains(r.Message, "no other last known good") {
		t.Fatalf("%+v", r)
	}
}

func TestApplyWhileDownOnlySaves(t *testing.T) {
	d := newFakeDocker()
	e := newTestEngine(t, d, t.TempDir())
	s := testSettings()
	s.SimInstances = 4
	_ = e.Apply("change simulator", s)
	r := lastResult(t, e)
	st, _, _ := e.Snapshot()
	if !r.OK || d.ups != 0 || st.Desired != s || st.Running {
		t.Fatalf("%+v %+v", r, st)
	}
}

func TestDownAndUp(t *testing.T) {
	e, d := runningEngine(t)
	_ = e.Down()
	if r := lastResult(t, e); !r.OK {
		t.Fatalf("%+v", r)
	}
	st, _, _ := e.Snapshot()
	if st.Running || len(d.images()) != 0 {
		t.Fatal("still up")
	}
	if err := e.Restart(); err == nil {
		t.Fatal("restart of a stopped stack accepted")
	}
	_ = e.Up()
	if r := lastResult(t, e); !r.OK || len(d.images()) == 0 {
		t.Fatalf("%+v", r)
	}
}

func TestRestartRecreates(t *testing.T) {
	e, d := runningEngine(t)
	ups := d.ups
	_ = e.Restart()
	if r := lastResult(t, e); !r.OK || d.ups != ups+1 {
		t.Fatalf("%+v", r)
	}
}

func TestOneActionAtATime(t *testing.T) {
	e, d := runningEngine(t)
	d.blockUp = make(chan struct{})
	if err := e.Restart(); err != nil {
		t.Fatal(err)
	}
	time.Sleep(20 * time.Millisecond)
	if err := e.Down(); !errors.Is(err, ErrBusy) {
		t.Fatalf("second action: %v", err)
	}
	if err := e.Apply("x", testSettings()); !errors.Is(err, ErrBusy) {
		t.Fatalf("second action: %v", err)
	}
	close(d.blockUp)
	e.Wait()
}

func TestValidationIsImmediate(t *testing.T) {
	e, _ := runningEngine(t)
	s := testSettings()
	s.OptimizerInstances = 33
	if err := e.Apply("x", s); err == nil || !strings.Contains(err.Error(), "between 1 and 32") {
		t.Fatalf("%v", err)
	}
}

// restartManager simulates the manager being killed: a new engine on the same state directory.
func restartManager(t *testing.T, d *fakeDocker, dir string) *Engine {
	e := newTestEngine(t, d, dir)
	if err := e.Reconcile(context.Background()); err != nil {
		t.Fatal(err)
	}
	e.Wait()
	return e
}

func TestRecoverInterruptedApply(t *testing.T) {
	d := newFakeDocker()
	dir := t.TempDir()
	e := newTestEngine(t, d, dir)
	_ = e.Up()
	lastResult(t, e)
	// The manager died after starting to apply 5 optimizer nodes.
	target := testSettings()
	target.OptimizerInstances = 5
	st, _ := LoadState(dir)
	st.Desired = target
	st.Journal = &Journal{Action: "change optimizer", Phase: "apply", Target: target, Previous: testSettings(), PreviousRunning: true}
	SaveState(dir, st)
	e2 := restartManager(t, d, dir)
	r := lastResult(t, e2)
	st2, _, _ := e2.Snapshot()
	if !r.OK || st2.Journal != nil || *st2.LastKnownGood != target || len(d.images()) != len(ExpectedServices(target, testCfg)) {
		t.Fatalf("%+v %+v", r, st2)
	}
}

func TestRecoverInterruptedUnhealthyApplyRollsBack(t *testing.T) {
	d := newFakeDocker()
	dir := t.TempDir()
	e := newTestEngine(t, d, dir)
	_ = e.Up()
	lastResult(t, e)
	target := testSettings()
	target.SimTag = "sick"
	d.sick["ghcr.io/nk-coding/palladio-sim-service:sick"] = true
	st, _ := LoadState(dir)
	st.Desired = target
	st.Journal = &Journal{Action: "change versions", Phase: "apply", Target: target, Previous: testSettings(), PreviousRunning: true}
	SaveState(dir, st)
	e2 := restartManager(t, d, dir)
	r := lastResult(t, e2)
	st2, _, _ := e2.Snapshot()
	if r.OK || !strings.Contains(r.Message, "rolled back") || st2.Desired != testSettings() || d.images()["palladio-sim-0"] != "ghcr.io/nk-coding/palladio-sim-service:0.2.0" {
		t.Fatalf("%+v %+v", r, st2)
	}
}

func TestRecoverDuringPrepareKeepsOldSettings(t *testing.T) {
	d := newFakeDocker()
	dir := t.TempDir()
	e := newTestEngine(t, d, dir)
	_ = e.Up()
	lastResult(t, e)
	target := testSettings()
	target.MdeoTag = "9.9.9"
	st, _ := LoadState(dir)
	st.Journal = &Journal{Action: "change versions", Phase: "prepare", Target: target, Previous: testSettings(), PreviousRunning: true}
	SaveState(dir, st)
	d.running = map[string]Container{} // and the host rebooted
	e2 := restartManager(t, d, dir)
	r := lastResult(t, e2)
	st2, _, _ := e2.Snapshot()
	if !r.OK || st2.Desired != testSettings() || d.images()["backend"] != "ghcr.io/nk-coding/mdeo-backend:1.2.3" {
		t.Fatalf("%+v %+v", r, st2)
	}
}

func TestRecoverInterruptedDown(t *testing.T) {
	d := newFakeDocker()
	dir := t.TempDir()
	e := newTestEngine(t, d, dir)
	_ = e.Up()
	lastResult(t, e)
	st, _ := LoadState(dir)
	st.Running = false
	st.Journal = &Journal{Action: "down", Phase: "apply", Target: testSettings()}
	SaveState(dir, st)
	e2 := restartManager(t, d, dir)
	if r := lastResult(t, e2); !r.OK || len(d.images()) != 0 {
		t.Fatalf("%+v", r)
	}
}

func TestReconcileAfterRebootBringsStackUp(t *testing.T) {
	d := newFakeDocker()
	dir := t.TempDir()
	e := newTestEngine(t, d, dir)
	_ = e.Up()
	lastResult(t, e)
	d.running = map[string]Container{}
	e2 := restartManager(t, d, dir)
	if r := lastResult(t, e2); !r.OK || len(d.images()) == 0 {
		t.Fatalf("%+v", r)
	}
}

func TestReconcileKeepsDownStackDown(t *testing.T) {
	d := newFakeDocker()
	dir := t.TempDir()
	newTestEngine(t, d, dir)
	e2 := restartManager(t, d, dir)
	if r := lastResult(t, e2); !r.OK || d.ups != 0 {
		t.Fatalf("%+v", r)
	}
}

func TestStateFileIsAtomicAndPrivate(t *testing.T) {
	dir := t.TempDir()
	if err := SaveState(dir, State{Desired: testSettings(), Running: true}); err != nil {
		t.Fatal(err)
	}
	fi, err := os.Stat(dir + "/state.json")
	if err != nil || fi.Mode().Perm() != 0o600 {
		t.Fatalf("%v %v", err, fi.Mode())
	}
	entries, _ := os.ReadDir(dir)
	if len(entries) != 1 {
		t.Fatalf("temporary files left: %v", entries)
	}
	st, err := LoadState(dir)
	if err != nil || st.Desired != testSettings() || !st.Running {
		t.Fatalf("%+v %v", st, err)
	}
	os.WriteFile(dir+"/state.json", []byte("{garbage"), 0o600)
	if _, err := LoadState(dir); err == nil {
		t.Fatal("corrupt state accepted")
	}
}

func TestComposeArguments(t *testing.T) {
	c := &ComposeCLI{Project: "mdeo", EnvFiles: []string{"/state/secrets.env", "/config/stack.env"}}
	down := strings.Join(c.downArgs(), " ")
	if strings.Contains(down, "--volumes") || strings.Contains(down, " -v") || !strings.Contains(down, "--project-name mdeo") {
		t.Fatalf("down: %s", down)
	}
	up := strings.Join(c.upArgs("/state/stack/compose.yaml", false), " ")
	for _, want := range []string{"--project-name mdeo", "--env-file /state/secrets.env --env-file /config/stack.env", "up --detach --remove-orphans --pull missing"} {
		if !strings.Contains(up, want) {
			t.Fatalf("up lacks %q: %s", want, up)
		}
	}
	if strings.Contains(up, "--force-recreate") || !strings.Contains(strings.Join(c.upArgs("f", true), " "), "--force-recreate") {
		t.Fatal("recreate flag wrong")
	}
}

func TestParsePs(t *testing.T) {
	lines := `{"Name":"mdeo-backend-1","Service":"backend","State":"running","Health":"healthy","Image":"x:1"}
{"Name":"mdeo-workbench-1","Service":"workbench","State":"running","Health":"","Image":"y:1"}`
	cs, err := parsePs(lines)
	if err != nil || len(cs) != 2 || cs[0].Health != "healthy" {
		t.Fatalf("%v %+v", err, cs)
	}
	cs, err = parsePs(`[{"Name":"a","Service":"backend","State":"exited"}]`)
	if err != nil || len(cs) != 1 || cs[0].State != "exited" {
		t.Fatalf("%v %+v", err, cs)
	}
	if cs, err := parsePs(""); err != nil || len(cs) != 0 {
		t.Fatal("empty output")
	}
}

func TestHealth(t *testing.T) {
	exp := map[string]string{"a": "img:1", "b": "img:2"}
	ok := []Container{{Service: "a", State: "running", Health: "healthy", Image: "img:1"}, {Service: "b", State: "running", Image: "img:2"}}
	if err := Health(ok, exp); err != nil {
		t.Fatal(err)
	}
	cases := [][]Container{
		{ok[0]},
		{ok[0], {Service: "b", State: "running", Health: "starting", Image: "img:2"}},
		{ok[0], {Service: "b", State: "restarting", Image: "img:2"}},
		{ok[0], {Service: "b", State: "running", Image: "img:old"}},
		{ok[0], ok[1], {Service: "c", State: "running"}},
	}
	for i, c := range cases {
		if Health(c, exp) == nil {
			t.Fatalf("case %d counted as healthy", i)
		}
	}
}

func (f *fakeDocker) Restart(ctx context.Context, container string) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	for svc, c := range f.running {
		if c.Name == container {
			f.restarts = append(f.restarts, svc)
			c.Health = f.afterRestart
			if c.Health == "" {
				c.Health = "starting"
			}
			f.running[svc] = c
			return nil
		}
	}
	return errors.New("no such container " + container)
}

func (f *fakeDocker) setHealth(svc, h string) {
	f.mu.Lock()
	defer f.mu.Unlock()
	c := f.running[svc]
	c.Health = h
	f.running[svc] = c
}

func (f *fakeDocker) restartCount() int {
	f.mu.Lock()
	defer f.mu.Unlock()
	return len(f.restarts)
}
