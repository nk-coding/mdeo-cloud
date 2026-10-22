package main

import (
	"context"
	"strings"
	"testing"
	"time"
)

// healEngine is a running stack with a controllable clock and a 5-minute grace period.
func healEngine(t *testing.T) (*Engine, *fakeDocker, *time.Time) {
	e, d := runningEngine(t)
	clock := time.Date(2026, 10, 23, 8, 0, 0, 0, time.UTC)
	e.now = func() time.Time { return clock }
	e.Heal = HealPolicy{Enabled: true, Interval: 30 * time.Second, Grace: 5 * time.Minute,
		BackoffBase: time.Minute, BackoffMax: 30 * time.Minute}
	return e, d, &clock
}

func TestHealWaitsForTheGracePeriod(t *testing.T) {
	e, d, clock := healEngine(t)
	ctx := context.Background()
	d.setHealth("backend", "unhealthy")
	for i := 0; i < 10; i++ { // 4.5 min of checks every 30 s
		if ev := e.HealOnce(ctx); len(ev) != 0 {
			t.Fatalf("restarted after %d checks", i)
		}
		*clock = clock.Add(30 * time.Second)
	}
	// Healthy in between starts the grace period over.
	d.setHealth("backend", "healthy")
	e.HealOnce(ctx)
	d.setHealth("backend", "unhealthy")
	e.HealOnce(ctx)
	*clock = clock.Add(4 * time.Minute)
	if ev := e.HealOnce(ctx); len(ev) != 0 {
		t.Fatal("restarted before 5 minutes of continuous unhealth")
	}
	*clock = clock.Add(61 * time.Second)
	ev := e.HealOnce(ctx)
	if len(ev) != 1 || ev[0].Service != "backend" || ev[0].Action != "restart" || !ev[0].OK || d.restartCount() != 1 {
		t.Fatalf("%+v %v", ev, d.restarts)
	}
	st, _, _ := e.Snapshot()
	if st.HealCount != 1 || len(st.Heals) != 1 || st.Heals[0].Container != "mdeo-backend-1" {
		t.Fatalf("heal not recorded: %+v", st)
	}
	if st.LastResult == nil || st.LastResult.Action != "up" {
		t.Fatal("a heal must not replace the last action's result")
	}
}

func TestStartingNeverCountsAsUnhealthy(t *testing.T) {
	e, d, clock := healEngine(t)
	d.setHealth("backend", "starting")
	for i := 0; i < 30; i++ {
		e.HealOnce(context.Background())
		*clock = clock.Add(time.Minute)
	}
	if d.restartCount() != 0 {
		t.Fatal("a container in its start period was restarted")
	}
}

func TestHealBacksOffExponentially(t *testing.T) {
	e, d, clock := healEngine(t)
	ctx := context.Background()
	d.afterRestart = "unhealthy" // crash loop: never recovers
	d.setHealth("service-model", "unhealthy")
	var at []time.Duration
	start := *clock
	for i := 0; i < 400; i++ { // 200 min, a check every 30 s
		if ev := e.HealOnce(ctx); len(ev) > 0 {
			at = append(at, clock.Sub(start))
		}
		*clock = clock.Add(30 * time.Second)
	}
	// Restarts need 5 min of unhealth each, and then the backoff of the previous attempt:
	// 1, 2, 4, 8, 16, 30, 30 ... minutes.
	if len(at) < 6 {
		t.Fatalf("restarts at %v", at)
	}
	for i := 1; i < len(at); i++ {
		gap := at[i] - at[i-1]
		want := e.Heal.Backoff(i)
		if want < e.Heal.Grace {
			want = e.Heal.Grace
		}
		if gap < want || gap > want+time.Minute {
			t.Fatalf("gap %d is %s, want about %s (restarts at %v)", i, gap, want, at)
		}
	}
	if e.Heal.Backoff(20) != 30*time.Minute || e.Heal.Backoff(1) != time.Minute || e.Heal.Backoff(3) != 4*time.Minute {
		t.Fatal("backoff sequence")
	}
}

func TestBackoffResetsAfterBeingHealthyLongEnough(t *testing.T) {
	e, d, clock := healEngine(t)
	ctx := context.Background()
	d.setHealth("backend", "unhealthy")
	e.HealOnce(ctx)
	*clock = clock.Add(6 * time.Minute)
	e.HealOnce(ctx)
	if e.tracks["backend"].attempts != 1 {
		t.Fatal("no first attempt")
	}
	d.setHealth("backend", "healthy")
	e.HealOnce(ctx)
	*clock = clock.Add(31 * time.Minute)
	e.HealOnce(ctx)
	if e.tracks["backend"].attempts != 0 {
		t.Fatal("attempts not reset after 30 healthy minutes")
	}
}

func TestHealRespectsTheLockAndTheDesiredState(t *testing.T) {
	e, d, clock := healEngine(t)
	ctx := context.Background()
	d.setHealth("backend", "unhealthy")
	e.HealOnce(ctx)
	*clock = clock.Add(10 * time.Minute)
	// An action holds the lock: no heal.
	d.blockUp = make(chan struct{})
	if err := e.Restart(); err != nil {
		t.Fatal(err)
	}
	time.Sleep(20 * time.Millisecond)
	if ev := e.HealOnce(ctx); ev != nil || d.restartCount() != 0 {
		t.Fatal("healed during an action")
	}
	close(d.blockUp)
	e.Wait()
	d.blockUp = nil
	// Stack down: no heal.
	_ = e.Down()
	e.Wait()
	d.running = map[string]Container{"backend": {Name: "mdeo-backend-1", Service: "backend", State: "running", Health: "unhealthy", Image: "x"}}
	*clock = clock.Add(time.Hour)
	if ev := e.HealOnce(ctx); ev != nil || d.ups != 2 {
		t.Fatalf("healed a stack that should be down: %+v", ev)
	}
}

func TestPostgresIsRestartedNeverRemoved(t *testing.T) {
	e, d, clock := healEngine(t)
	ctx := context.Background()
	downs, ups := d.downs, d.ups
	d.setHealth("postgres-optimizer", "unhealthy")
	e.HealOnce(ctx)
	*clock = clock.Add(6 * time.Minute)
	ev := e.HealOnce(ctx)
	if len(ev) != 1 || ev[0].Service != "postgres-optimizer" || ev[0].Action != "restart" {
		t.Fatalf("%+v", ev)
	}
	if d.downs != downs || d.ups != ups {
		t.Fatal("a database heal took the stack down or recreated it")
	}
}

func TestMissingContainerIsRecreatedWithUp(t *testing.T) {
	e, d, clock := healEngine(t)
	ctx := context.Background()
	d.mu.Lock()
	delete(d.running, "service-csv")
	c := d.running["palladio-sim-0"]
	c.State = "exited"
	d.running["palladio-sim-0"] = c
	d.mu.Unlock()
	ups := d.ups
	ev := e.HealOnce(ctx)
	if len(ev) != 1 || ev[0].Action != "up" || !strings.Contains(ev[0].Reason, "service-csv has no container") ||
		!strings.Contains(ev[0].Reason, "palladio-sim-0 is exited") || d.ups != ups+1 {
		t.Fatalf("%+v", ev)
	}
	if _, ok := d.images()["service-csv"]; !ok {
		t.Fatal("not recreated")
	}
	// Backoff applies to the stack repair as well.
	d.mu.Lock()
	delete(d.running, "service-csv")
	d.mu.Unlock()
	if ev := e.HealOnce(ctx); ev != nil {
		t.Fatal("second compose up without backoff")
	}
	*clock = clock.Add(61 * time.Second)
	if ev := e.HealOnce(ctx); len(ev) != 1 {
		t.Fatal("no compose up after the backoff")
	}
}

func TestOperatorActionWaitsForAHealCheck(t *testing.T) {
	e, _, _ := healEngine(t)
	e.mu.Lock()
	e.busy, e.healing = true, true
	e.mu.Unlock()
	go func() {
		time.Sleep(100 * time.Millisecond)
		e.mu.Lock()
		e.busy, e.healing = false, false
		e.mu.Unlock()
	}()
	if err := e.Restart(); err != nil {
		t.Fatalf("an action was refused during a heal check: %v", err)
	}
	e.Wait()
}

func TestHealPolicyFromEnv(t *testing.T) {
	t.Setenv("HEAL_GRACE_SECONDS", "120")
	p, err := healPolicyFromEnv()
	if err != nil || p.Grace != 2*time.Minute || !p.Enabled || p.Interval != 30*time.Second {
		t.Fatalf("%+v %v", p, err)
	}
	t.Setenv("AUTO_HEAL", "false")
	if p, _ := healPolicyFromEnv(); p.Enabled {
		t.Fatal("AUTO_HEAL=false ignored")
	}
	t.Setenv("HEAL_GRACE_SECONDS", "1")
	if _, err := healPolicyFromEnv(); err == nil {
		t.Fatal("1 s grace accepted")
	}
}
