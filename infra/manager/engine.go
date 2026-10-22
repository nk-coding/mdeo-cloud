package main

import (
	"context"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"sync"
	"time"
)

// ErrBusy is returned when an action is requested while another one runs.
var ErrBusy = errors.New("another action is running; wait until it has finished")

// Engine runs the actions, one at a time, and owns the persistent state.
//
// Every action follows the same rule: everything that can fail without touching the running
// stack (validating, generating, `docker compose config`, pulling) happens first; only then is
// the stack changed, and a change that does not become healthy is undone by going back to the
// last known good settings. The journal in the state file says how far an action got, so a
// manager that is killed mid-action finishes or undoes it when it starts again.
type Engine struct {
	Dir           string
	Docker        Docker
	Cfg           StackConfig
	Host          Host
	HealthTimeout time.Duration
	PollInterval  time.Duration
	// PullTimeout bounds one image pull.
	PullTimeout time.Duration

	// Heal is the policy of the heal loop (heal.go).
	Heal HealPolicy

	mu      sync.Mutex
	state   State
	busy    bool
	healing bool
	current *Result
	tracks  map[string]*healTrack
	// now is the clock of the heal loop (tests replace it).
	now func() time.Time
	// Logf receives every action log line (the container log of the manager).
	Logf func(format string, args ...interface{})
}

const maxLogLines = 300

// NewEngine loads the state from dir, or starts from initial when there is none.
func NewEngine(dir string, d Docker, cfg StackConfig, host Host, initial Settings) (*Engine, error) {
	if err := os.MkdirAll(filepath.Join(dir, "stack"), 0o700); err != nil {
		return nil, err
	}
	e := &Engine{Dir: dir, Docker: d, Cfg: cfg, Host: host, HealthTimeout: 10 * time.Minute,
		PollInterval: 5 * time.Second, PullTimeout: 30 * time.Minute, Logf: func(string, ...interface{}) {},
		Heal: DefaultHealPolicy(), tracks: map[string]*healTrack{}, now: time.Now}
	st, err := LoadState(dir)
	switch {
	case err == nil:
		e.state = st
	case errors.Is(err, os.ErrNotExist):
		e.state = State{Desired: initial, Running: false}
		if err := SaveState(dir, e.state); err != nil {
			return nil, err
		}
	default:
		return nil, err
	}
	return e, nil
}

// Snapshot returns a copy of the state, the action in progress (if any) and whether one runs.
func (e *Engine) Snapshot() (State, *Result, bool) {
	e.mu.Lock()
	defer e.mu.Unlock()
	st := e.state
	var cur *Result
	if e.current != nil {
		c := *e.current
		c.Log = append([]string(nil), e.current.Log...)
		cur = &c
	}
	return st, cur, e.busy
}

// ComposePath is the compose file of the stack as last applied.
func (e *Engine) ComposePath() string { return filepath.Join(e.Dir, "stack", "compose.yaml") }

func (e *Engine) stagedPath() string { return filepath.Join(e.Dir, "stack", "compose.next.yaml") }

// action is the context of one running action.
type action struct {
	e *Engine
}

func (a action) log(format string, args ...interface{}) {
	line := fmt.Sprintf(format, args...)
	a.e.Logf("%s", line)
	a.e.mu.Lock()
	defer a.e.mu.Unlock()
	if a.e.current == nil {
		return
	}
	a.e.current.Log = append(a.e.current.Log, time.Now().UTC().Format("15:04:05")+" "+line)
	if n := len(a.e.current.Log); n > maxLogLines {
		a.e.current.Log = append([]string{"…"}, a.e.current.Log[n-maxLogLines+1:]...)
	}
}

// update changes the state and writes it before returning. A state that cannot be written stops
// the action: continuing would leave disk and stack disagreeing after a restart.
func (a action) update(f func(*State)) error {
	a.e.mu.Lock()
	defer a.e.mu.Unlock()
	next := a.e.state
	f(&next)
	if err := SaveState(a.e.Dir, next); err != nil {
		return fmt.Errorf("cannot write the state: %w", err)
	}
	a.e.state = next
	return nil
}

func (a action) get() State {
	a.e.mu.Lock()
	defer a.e.mu.Unlock()
	return a.e.state
}

// start runs fn as the one action, in the background. It fails with ErrBusy while another runs.
func (e *Engine) start(name string, fn func(ctx context.Context, a action) (bool, string)) error {
	e.mu.Lock()
	// The heal loop holds the lock only briefly (one check, at most one restart); an operator's
	// action waits for it instead of being refused.
	for deadline := time.Now().Add(90 * time.Second); e.busy && e.healing && time.Now().Before(deadline); {
		e.mu.Unlock()
		time.Sleep(50 * time.Millisecond)
		e.mu.Lock()
	}
	if e.busy {
		e.mu.Unlock()
		return ErrBusy
	}
	e.busy = true
	e.current = &Result{Action: name, Started: time.Now().UTC()}
	e.mu.Unlock()
	go e.finish(name, fn)
	return nil
}

func (e *Engine) finish(name string, fn func(ctx context.Context, a action) (bool, string)) {
	a := action{e}
	ok, msg := false, ""
	func() {
		defer func() {
			if r := recover(); r != nil {
				ok, msg = false, fmt.Sprintf("internal error: %v", r)
			}
		}()
		ok, msg = fn(context.Background(), a)
	}()
	a.log("%s: %s", map[bool]string{true: "done", false: "FAILED"}[ok], msg)
	e.mu.Lock()
	res := *e.current
	e.mu.Unlock()
	res.Finished, res.OK, res.Message = time.Now().UTC(), ok, msg
	if err := a.update(func(s *State) { s.LastResult = &res }); err != nil {
		e.Logf("%v", err)
	}
	e.mu.Lock()
	e.busy, e.current = false, nil
	e.mu.Unlock()
}

// Wait blocks until no action runs (tests, shutdown).
func (e *Engine) Wait() {
	for {
		e.mu.Lock()
		b := e.busy
		e.mu.Unlock()
		if !b {
			return
		}
		time.Sleep(20 * time.Millisecond)
	}
}

// Apply changes the settings. Validation errors are returned at once; everything else runs in
// the background and ends up in the last result.
func (e *Engine) Apply(name string, target Settings) error {
	if p := target.Validate(); len(p) > 0 {
		return errors.New(strings.Join(p, "; "))
	}
	return e.start(name, func(ctx context.Context, a action) (bool, string) {
		return e.apply(ctx, a, name, target)
	})
}

func (e *Engine) apply(ctx context.Context, a action, name string, target Settings) (bool, string) {
	st := a.get()
	if err := a.update(func(s *State) {
		s.Journal = &Journal{Action: name, Phase: "prepare", Target: target, Previous: st.Desired,
			PreviousRunning: st.Running, Started: time.Now().UTC()}
	}); err != nil {
		return false, err.Error()
	}
	if err := e.prepare(ctx, a, target); err != nil {
		_ = a.update(func(s *State) { s.Journal = nil })
		return false, "Nothing was changed: " + err.Error()
	}
	if !st.Running {
		if err := a.update(func(s *State) { s.Desired, s.Journal = target, nil }); err != nil {
			return false, err.Error()
		}
		return true, "Settings saved (images present, configuration valid). The stack is down; they take effect with Up."
	}
	return e.applyAndVerify(ctx, a, target)
}

// prepare does everything that cannot affect the running stack: generate the compose file,
// have compose validate it, and pull the images that are not present yet.
func (e *Engine) prepare(ctx context.Context, a action, target Settings) error {
	b, err := Generate(target, e.Cfg, e.Host)
	if err != nil {
		return err
	}
	if err := WriteFileAtomic(e.stagedPath(), b, 0o600); err != nil {
		return err
	}
	a.log("checking the configuration (docker compose config)")
	cctx, cancel := context.WithTimeout(ctx, 2*time.Minute)
	images, err := e.Docker.Config(cctx, e.stagedPath())
	cancel()
	if err != nil {
		return fmt.Errorf("the configuration is invalid: %w", err)
	}
	for _, img := range images {
		cctx, cancel := context.WithTimeout(ctx, 2*time.Minute)
		present, err := e.Docker.ImagePresent(cctx, img)
		cancel()
		if err != nil {
			return fmt.Errorf("cannot inspect %s: %w", img, err)
		}
		if present {
			continue
		}
		a.log("pulling %s", img)
		pctx, cancel := context.WithTimeout(ctx, e.PullTimeout)
		err = e.Docker.Pull(pctx, img)
		cancel()
		if err != nil {
			return fmt.Errorf("pulling %s failed: %w", img, err)
		}
	}
	a.log("configuration valid, %d images present", len(images))
	return nil
}

// up writes the compose file of s and brings the stack to it.
func (e *Engine) up(ctx context.Context, a action, s Settings, recreate bool) error {
	b, err := Generate(s, e.Cfg, e.Host)
	if err != nil {
		return err
	}
	if err := WriteFileAtomic(e.ComposePath(), b, 0o600); err != nil {
		return err
	}
	a.log("bringing the stack to the settings (docker compose up%s)", map[bool]string{true: " --force-recreate", false: ""}[recreate])
	uctx, cancel := context.WithTimeout(ctx, e.HealthTimeout+5*time.Minute)
	defer cancel()
	return e.Docker.Up(uctx, e.ComposePath(), recreate)
}

// applyAndVerify changes the running stack to target, waits for it to be healthy and records it
// as last known good, or goes back to the last known good settings.
func (e *Engine) applyAndVerify(ctx context.Context, a action, target Settings) (bool, string) {
	if err := a.update(func(s *State) {
		s.Desired, s.Running = target, true
		if s.Journal == nil {
			s.Journal = &Journal{Action: "apply", Started: time.Now().UTC()}
		}
		s.Journal.Phase, s.Journal.Target = "apply", target
	}); err != nil {
		return false, err.Error()
	}
	err := e.up(ctx, a, target, false)
	if err == nil {
		err = e.waitHealthy(ctx, a, target)
	}
	if err == nil {
		if err := a.update(func(s *State) { s.LastKnownGood, s.Journal = &target, nil }); err != nil {
			return false, err.Error()
		}
		return true, "Applied; every service is healthy."
	}
	a.log("the change did not become healthy: %v", err)
	return e.rollback(ctx, a, target, err)
}

// rollback goes back to the last known good settings after failed did not become healthy.
func (e *Engine) rollback(ctx context.Context, a action, failed Settings, cause error) (bool, string) {
	lkg := a.get().LastKnownGood
	if lkg == nil || *lkg == failed {
		_ = a.update(func(s *State) { s.Journal = nil })
		return false, fmt.Sprintf("The stack is not healthy (%v). There are no other last known good settings to go back to, so it stays as it is.", cause)
	}
	good := *lkg
	if err := a.update(func(s *State) {
		s.Desired, s.Running = good, true
		if s.Journal == nil {
			s.Journal = &Journal{Action: "rollback", Started: time.Now().UTC()}
		}
		s.Journal.Phase = "rollback"
	}); err != nil {
		return false, err.Error()
	}
	a.log("rolling back to the last known good settings (mdeo %s, simulator %s)", good.MdeoTag, good.SimTag)
	err := e.up(ctx, a, good, false)
	if err == nil {
		err = e.waitHealthy(ctx, a, good)
	}
	_ = a.update(func(s *State) { s.Journal = nil })
	if err != nil {
		return false, fmt.Sprintf("The change failed (%v) and was rolled back to the last known good settings, but those are not healthy either: %v", cause, err)
	}
	return false, fmt.Sprintf("The change failed (%v); rolled back to the last known good settings, which are healthy again.", cause)
}

// Health compares the stack's containers with what settings s generate. It returns nil when
// every expected service has a running, healthy container with the expected image and nothing
// else runs, or a description of what is not there yet.
func Health(cs []Container, expected map[string]string) error {
	var problems []string
	byService := map[string][]Container{}
	for _, c := range cs {
		byService[c.Service] = append(byService[c.Service], c)
	}
	names := make([]string, 0, len(expected))
	for n := range expected {
		names = append(names, n)
	}
	sort.Strings(names)
	for _, n := range names {
		list := byService[n]
		if len(list) == 0 {
			problems = append(problems, n+": no container")
			continue
		}
		for _, c := range list {
			switch {
			case c.Image != expected[n]:
				problems = append(problems, fmt.Sprintf("%s: runs %s, expected %s", n, c.Image, expected[n]))
			case c.State != "running":
				problems = append(problems, fmt.Sprintf("%s: %s", n, c.State))
			case c.Health != "" && c.Health != "healthy":
				problems = append(problems, fmt.Sprintf("%s: %s", n, c.Health))
			}
		}
	}
	var extra []string
	for s := range byService {
		if _, ok := expected[s]; !ok {
			extra = append(extra, s)
		}
	}
	sort.Strings(extra)
	for _, s := range extra {
		problems = append(problems, s+": should not exist")
	}
	if len(problems) > 0 {
		return errors.New(strings.Join(problems, ", "))
	}
	return nil
}

func (e *Engine) waitHealthy(ctx context.Context, a action, s Settings) error {
	expected := ExpectedServices(s, e.Cfg)
	deadline := time.Now().Add(e.HealthTimeout)
	a.log("waiting up to %s for %d services to be healthy", e.HealthTimeout, len(expected))
	lastReport := time.Now()
	for {
		pctx, cancel := context.WithTimeout(ctx, time.Minute)
		cs, err := e.Docker.Ps(pctx, e.ComposePath())
		cancel()
		if err == nil {
			err = Health(cs, expected)
			if err == nil {
				a.log("all %d services healthy", len(expected))
				return nil
			}
		}
		if time.Now().After(deadline) {
			return fmt.Errorf("not healthy after %s: %v", e.HealthTimeout, err)
		}
		if time.Since(lastReport) >= 30*time.Second {
			a.log("still waiting: %v", err)
			lastReport = time.Now()
		}
		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-time.After(e.PollInterval):
		}
	}
}

// Restart recreates every container of the stack with the current settings.
func (e *Engine) Restart() error {
	if st, _, _ := e.Snapshot(); !st.Running {
		return errors.New("the stack is down; use Up")
	}
	return e.start("restart", func(ctx context.Context, a action) (bool, string) {
		st := a.get()
		if err := a.update(func(s *State) {
			s.Journal = &Journal{Action: "restart", Phase: "apply", Target: st.Desired, Previous: st.Desired,
				PreviousRunning: true, Started: time.Now().UTC()}
		}); err != nil {
			return false, err.Error()
		}
		err := e.up(ctx, a, st.Desired, true)
		if err == nil {
			err = e.waitHealthy(ctx, a, st.Desired)
		}
		if err != nil {
			return e.rollback(ctx, a, st.Desired, err)
		}
		_ = a.update(func(s *State) { s.Journal, s.LastKnownGood = nil, &st.Desired })
		return true, "Restarted; every service is healthy."
	})
}

// Down stops and removes the stack's containers. Volumes are kept.
func (e *Engine) Down() error {
	return e.start("down", func(ctx context.Context, a action) (bool, string) {
		st := a.get()
		if err := a.update(func(s *State) {
			s.Running = false
			s.Journal = &Journal{Action: "down", Phase: "apply", Target: st.Desired, Previous: st.Desired,
				PreviousRunning: st.Running, Started: time.Now().UTC()}
		}); err != nil {
			return false, err.Error()
		}
		return e.down(ctx, a)
	})
}

func (e *Engine) down(ctx context.Context, a action) (bool, string) {
	a.log("taking the stack down (docker compose down; volumes are kept)")
	dctx, cancel := context.WithTimeout(ctx, 10*time.Minute)
	err := e.Docker.Down(dctx, e.ComposePath())
	cancel()
	_ = a.update(func(s *State) { s.Journal = nil })
	if err != nil {
		return false, "docker compose down failed: " + err.Error()
	}
	return true, "The stack is down. The databases' volumes are kept."
}

// Up brings the stack up with the current settings.
func (e *Engine) Up() error {
	st, _, _ := e.Snapshot()
	if p := st.Desired.Validate(); len(p) > 0 {
		return errors.New("the settings are incomplete: " + strings.Join(p, "; "))
	}
	return e.start("up", func(ctx context.Context, a action) (bool, string) {
		st := a.get()
		if err := a.update(func(s *State) {
			s.Journal = &Journal{Action: "up", Phase: "prepare", Target: st.Desired, Previous: st.Desired,
				PreviousRunning: st.Running, Started: time.Now().UTC()}
		}); err != nil {
			return false, err.Error()
		}
		if err := e.prepare(ctx, a, st.Desired); err != nil {
			_ = a.update(func(s *State) { s.Journal = nil })
			return false, "Nothing was changed: " + err.Error()
		}
		return e.applyAndVerify(ctx, a, st.Desired)
	})
}

// Reconcile runs once when the manager starts: it waits for Docker, finishes or undoes an
// action that was cut off, and makes the stack match the saved desired state (after a reboot,
// Docker has already restarted the containers and this changes nothing).
func (e *Engine) Reconcile(ctx context.Context) error {
	return e.start("reconcile (manager start)", func(ctx context.Context, a action) (bool, string) {
		for i := 0; ; i++ {
			pctx, cancel := context.WithTimeout(ctx, 20*time.Second)
			err := e.Docker.Ping(pctx)
			cancel()
			if err == nil {
				break
			}
			if i == 0 {
				a.log("waiting for the Docker daemon: %v", err)
			}
			select {
			case <-ctx.Done():
				return false, ctx.Err().Error()
			case <-time.After(5 * time.Second):
			}
		}
		st := a.get()
		if j := st.Journal; j != nil {
			a.log("the manager stopped during %q (phase %s); recovering", j.Action, j.Phase)
			switch {
			case j.Phase == "prepare":
				// Nothing had been touched yet.
				_ = a.update(func(s *State) { s.Journal = nil })
				a.log("it had not changed anything yet; dropped")
			case j.Action == "down":
				return e.down(ctx, a)
			case j.Phase == "rollback":
				lkg := st.LastKnownGood
				if lkg == nil {
					_ = a.update(func(s *State) { s.Journal = nil })
					break
				}
				return e.rollback(ctx, a, Settings{}, errors.New("interrupted rollback"))
			default:
				ok, msg := e.applyAndVerify(ctx, a, j.Target)
				return ok, "Recovered the interrupted " + j.Action + ": " + msg
			}
			st = a.get()
		}
		if !st.Running {
			a.log("desired state: down")
			ok, msg := e.down(ctx, a)
			return ok, msg
		}
		if err := e.up(ctx, a, st.Desired, false); err != nil {
			return false, "docker compose up failed: " + err.Error()
		}
		if err := e.waitHealthy(ctx, a, st.Desired); err != nil {
			return false, "The stack is up but not healthy: " + err.Error()
		}
		return true, "The stack is up and healthy."
	})
}
