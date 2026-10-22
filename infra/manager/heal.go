package main

import (
	"context"
	"fmt"
	"sort"
	"strings"
	"time"
)

// HealPolicy says when the heal loop repairs the stack on its own.
//
// Docker restarts a container whose process exits (restart policy), but not one that keeps
// running and fails its health check. The heal loop does: while the stack should be up and no
// action runs, it checks the containers every Interval. A container that has been unhealthy
// without interruption for Grace is restarted (docker restart: the same container and volumes,
// nothing removed). Its health check's start period never counts, because Docker reports
// "starting" then, not "unhealthy". After every repair of a service the next one waits
// BackoffBase, doubled per repair up to BackoffMax, so a service that keeps failing is not
// restarted in a tight loop; the count starts over once it has been healthy for BackoffMax.
// A service whose container is missing, stopped or on the wrong image is repaired with
// `docker compose up -d` for the desired settings, under the same backoff.
type HealPolicy struct {
	Enabled     bool
	Interval    time.Duration
	Grace       time.Duration
	BackoffBase time.Duration
	BackoffMax  time.Duration
}

// DefaultHealPolicy: check every 30 s, restart after 5 min unhealthy, back off 1 min doubling to
// 30 min.
func DefaultHealPolicy() HealPolicy {
	return HealPolicy{Enabled: true, Interval: 30 * time.Second, Grace: 5 * time.Minute,
		BackoffBase: time.Minute, BackoffMax: 30 * time.Minute}
}

// Backoff is the wait after the n-th repair (n ≥ 1).
func (p HealPolicy) Backoff(n int) time.Duration {
	d := p.BackoffBase
	for i := 1; i < n && d < p.BackoffMax; i++ {
		d *= 2
	}
	if d > p.BackoffMax {
		d = p.BackoffMax
	}
	return d
}

// healTrack is what the loop remembers about one service between checks.
type healTrack struct {
	unhealthySince time.Time
	healthySince   time.Time
	attempts       int
	next           time.Time
}

// stackKey tracks repairs of the whole stack (compose up).
const stackKey = "(stack)"

const maxHealEvents = 30

func (e *Engine) track(key string) *healTrack {
	t := e.tracks[key]
	if t == nil {
		t = &healTrack{}
		e.tracks[key] = t
	}
	return t
}

// RunHealLoop checks the stack every Interval until ctx ends.
func (e *Engine) RunHealLoop(ctx context.Context) {
	if !e.Heal.Enabled {
		return
	}
	for {
		select {
		case <-ctx.Done():
			return
		case <-time.After(e.Heal.Interval):
		}
		e.HealOnce(ctx)
	}
}

// HealOnce runs one check and at most one kind of repair. It takes the action lock, so it never
// runs during an operator's action (or the start-up reconcile), and returns at once if the lock
// is taken, the stack should be down, or an action was cut off (the next start handles that).
// It reports the repairs it made.
func (e *Engine) HealOnce(ctx context.Context) []HealEvent {
	e.mu.Lock()
	if e.busy || !e.state.Running || e.state.Journal != nil {
		e.mu.Unlock()
		return nil
	}
	e.busy, e.healing = true, true
	e.current = &Result{Action: "auto-heal", Started: time.Now().UTC()}
	desired := e.state.Desired
	e.mu.Unlock()
	defer func() {
		e.mu.Lock()
		e.busy, e.healing, e.current = false, false, nil
		e.mu.Unlock()
	}()
	a := action{e}

	pctx, cancel := context.WithTimeout(ctx, time.Minute)
	cs, err := e.Docker.Ps(pctx, e.ComposePath())
	cancel()
	if err != nil {
		e.Logf("auto-heal: docker compose ps failed: %v", err)
		return nil
	}
	now := e.now()
	expected := ExpectedServices(desired, e.Cfg)

	// Structure first: every service has its container, running, on the right image, and no
	// container is left over. Anything else is compose's job.
	if reason := structuralProblem(cs, expected); reason != "" {
		t := e.track(stackKey)
		if now.Before(t.next) {
			return nil
		}
		t.attempts++
		t.next = now.Add(e.Heal.Backoff(t.attempts))
		ev := HealEvent{Time: now.UTC(), Service: stackKey, Action: "up", Reason: reason, Attempt: t.attempts}
		e.Logf("auto-heal: %s; docker compose up (attempt %d)", reason, t.attempts)
		if err := e.up(ctx, a, desired, false); err != nil {
			ev.Error = err.Error()
		} else {
			ev.OK = true
		}
		e.recordHeals([]HealEvent{ev})
		return []HealEvent{ev}
	}
	if t := e.tracks[stackKey]; t != nil && now.Sub(t.next) >= e.Heal.BackoffMax {
		t.attempts = 0
	}

	sort.Slice(cs, func(i, j int) bool { return cs[i].Service < cs[j].Service })
	var events []HealEvent
	for _, c := range cs {
		if _, ok := expected[c.Service]; !ok {
			continue
		}
		t := e.track(c.Service)
		if c.Health != "unhealthy" {
			t.unhealthySince = time.Time{}
			if c.Health == "healthy" || c.Health == "" {
				if t.healthySince.IsZero() {
					t.healthySince = now
				}
				if t.attempts > 0 && now.Sub(t.healthySince) >= e.Heal.BackoffMax {
					t.attempts = 0
				}
			} else {
				t.healthySince = time.Time{}
			}
			continue
		}
		t.healthySince = time.Time{}
		if t.unhealthySince.IsZero() {
			t.unhealthySince = now
		}
		if now.Sub(t.unhealthySince) < e.Heal.Grace || now.Before(t.next) {
			continue
		}
		t.attempts++
		t.next = now.Add(e.Heal.Backoff(t.attempts))
		t.unhealthySince = time.Time{}
		ev := HealEvent{Time: now.UTC(), Service: c.Service, Container: c.Name, Action: "restart",
			Reason: fmt.Sprintf("unhealthy for more than %s", e.Heal.Grace), Attempt: t.attempts}
		e.Logf("auto-heal: %s unhealthy for more than %s; docker restart %s (attempt %d, next no earlier than %s)",
			c.Service, e.Heal.Grace, c.Name, t.attempts, e.Heal.Backoff(t.attempts))
		rctx, cancel := context.WithTimeout(ctx, 2*time.Minute)
		err := e.Docker.Restart(rctx, c.Name)
		cancel()
		if err != nil {
			ev.Error = err.Error()
		} else {
			ev.OK = true
		}
		events = append(events, ev)
	}
	if len(events) > 0 {
		e.recordHeals(events)
	}
	return events
}

// structuralProblem describes what `compose up` would have to fix, or "" when nothing.
func structuralProblem(cs []Container, expected map[string]string) string {
	var problems []string
	seen := map[string]bool{}
	for _, c := range cs {
		img, ok := expected[c.Service]
		switch {
		case !ok:
			problems = append(problems, c.Service+" should not exist")
		case c.State != "running" && c.State != "restarting":
			problems = append(problems, c.Service+" is "+c.State)
		case c.Image != img:
			problems = append(problems, c.Service+" runs "+c.Image)
		}
		seen[c.Service] = true
	}
	for svc := range expected {
		if !seen[svc] {
			problems = append(problems, svc+" has no container")
		}
	}
	sort.Strings(problems)
	return strings.Join(problems, ", ")
}

func (e *Engine) recordHeals(events []HealEvent) {
	a := action{e}
	if err := a.update(func(s *State) {
		for _, ev := range events {
			s.Heals = append([]HealEvent{ev}, s.Heals...)
			s.HealCount++
		}
		if len(s.Heals) > maxHealEvents {
			s.Heals = s.Heals[:maxHealEvents]
		}
	}); err != nil {
		e.Logf("auto-heal: %v", err)
	}
}
