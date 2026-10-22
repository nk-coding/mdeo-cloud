package main

import (
	"bytes"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"testing"
)

var testCfg = StackConfig{ImagePrefix: "ghcr.io/nk-coding", LBImage: "haproxy:3.2.24-alpine", PostgresImage: "postgres:17-alpine"}

func testSettings() Settings {
	s := DefaultSettings()
	s.MdeoTag, s.SimTag = "1.2.3", "0.2.0"
	return s
}

// services returns the top-level service names of a generated file, in order.
func services(t *testing.T, b []byte) []string {
	t.Helper()
	text := string(b)
	start := strings.Index(text, "\nservices:\n")
	end := strings.Index(text, "\nconfigs:\n")
	if start < 0 || end < 0 {
		t.Fatalf("no services/configs section")
	}
	re := regexp.MustCompile(`(?m)^  ([a-z0-9-]+):$`)
	var out []string
	for _, m := range re.FindAllStringSubmatch(text[start:end], -1) {
		out = append(out, m[1])
	}
	return out
}

func TestGenerateIsDeterministic(t *testing.T) {
	s := testSettings()
	s.OptimizerInstances, s.SimInstances = 8, 4
	a, err := Generate(s, testCfg, Host{})
	if err != nil {
		t.Fatal(err)
	}
	for i := 0; i < 5; i++ {
		b, _ := Generate(s, testCfg, Host{})
		if !bytes.Equal(a, b) {
			t.Fatal("two renders of the same settings differ")
		}
	}
}

func TestGenerateServicesMatchExpected(t *testing.T) {
	for _, n := range []int{1, 3, 8} {
		for _, sims := range []int{1, 4} {
			s := testSettings()
			s.OptimizerInstances, s.SimInstances = n, sims
			b, err := Generate(s, testCfg, Host{})
			if err != nil {
				t.Fatal(err)
			}
			got := services(t, b)
			exp := ExpectedServices(s, testCfg)
			if len(got) != len(exp) {
				t.Fatalf("n=%d sims=%d: %d services generated, %d expected: %v", n, sims, len(got), len(exp), got)
			}
			for _, g := range got {
				img, ok := exp[g]
				if !ok {
					t.Fatalf("unexpected service %s", g)
				}
				if !strings.Contains(string(b), fmt.Sprintf("image: %q", img)) {
					t.Fatalf("image %s of %s not in the file", img, g)
				}
			}
			if want := 4 + 2 + 9 + 2 + n + sims + 1; len(got) != want {
				t.Fatalf("n=%d sims=%d: %d services, want %d", n, sims, len(got), want)
			}
		}
	}
}

func TestOptimizerPeerMesh(t *testing.T) {
	s := testSettings()
	s.OptimizerInstances = 8
	nodes := optimizerNodes(s, nil)
	for i, n := range nodes {
		if n.ID != fmt.Sprint(i) || n.URL != fmt.Sprintf("http://optimizer-execution-%d:8080", i) {
			t.Fatalf("node %d: %+v", i, n)
		}
		peers := strings.Split(n.Peers, ",")
		if len(peers) != 7 {
			t.Fatalf("node %d has %d peers", i, len(peers))
		}
		for _, p := range peers {
			if p == n.URL {
				t.Fatalf("node %d lists itself", i)
			}
		}
	}
	s.OptimizerInstances = 1
	if p := optimizerNodes(s, nil)[0].Peers; p != "" {
		t.Fatalf("a single node has peers %q", p)
	}
	b, _ := Generate(s, testCfg, Host{})
	if !strings.Contains(string(b), `PEERS: ""`) || !strings.Contains(string(b), `OPTIMIZER_EXECUTION_SERVICE_URL: "http://optimizer-execution-0:8080"`) {
		t.Fatal("single node: PEERS or the entry URL missing")
	}
}

func TestNumaPinningRoundRobin(t *testing.T) {
	host := Host{Nodes: []NumaNode{{0, "0-3"}, {1, "4-7"}, {2, "8-11"}, {3, "12-15"}}}
	s := testSettings()
	s.OptimizerInstances, s.SimInstances, s.NumaPinning = 8, 4, true
	for i, n := range optimizerNodes(s, PinningNodes(s, host)) {
		if want := host.Nodes[i%4].CPUList; n.CPUSet != want {
			t.Fatalf("optimizer %d pinned to %q, want %q", i, n.CPUSet, want)
		}
	}
	for i, n := range simInstances(s, PinningNodes(s, host)) {
		if want := host.Nodes[i%4].CPUList; n.CPUSet != want {
			t.Fatalf("sim %d pinned to %q, want %q", i, n.CPUSet, want)
		}
	}
	b, _ := Generate(s, testCfg, host)
	if c := strings.Count(string(b), "cpuset:"); c != 12 {
		t.Fatalf("%d cpusets, want 12", c)
	}
	// Off, or a single node: no pinning at all.
	s.NumaPinning = false
	b, _ = Generate(s, testCfg, host)
	if strings.Contains(string(b), "cpuset:") {
		t.Fatal("pinning off but cpuset written")
	}
	s.NumaPinning = true
	b, _ = Generate(s, testCfg, Host{Nodes: []NumaNode{{0, "0-15"}}})
	if strings.Contains(string(b), "cpuset:") {
		t.Fatal("one NUMA node but cpuset written")
	}
}

func TestLoadBalancerConfig(t *testing.T) {
	s := testSettings()
	s.SimInstances = 3
	cfg := lbConfig(simInstances(s, nil))
	for i := 0; i < 3; i++ {
		if !strings.Contains(cfg, fmt.Sprintf("server palladio-sim-%d palladio-sim-%d:3022\n", i, i)) {
			t.Fatalf("server %d missing:\n%s", i, cfg)
		}
	}
	if strings.Contains(cfg, "palladio-sim-3") || !strings.Contains(cfg, "balance leastconn") || strings.Contains(cfg, "$") {
		t.Fatalf("unexpected config:\n%s", cfg)
	}
	a, _ := Generate(s, testCfg, Host{})
	s.SimInstances = 4
	b, _ := Generate(s, testCfg, Host{})
	hash := regexp.MustCompile(`lb-config-sha256: "([0-9a-f]+)"`)
	if hash.FindStringSubmatch(string(a))[1] == hash.FindStringSubmatch(string(b))[1] {
		t.Fatal("the LB config hash label does not change with the instance count")
	}
}

func TestGenerateRefusesInvalidSettings(t *testing.T) {
	s := testSettings()
	s.OptimizerInstances = 0
	if _, err := Generate(s, testCfg, Host{}); err == nil {
		t.Fatal("0 optimizer instances accepted")
	}
	s = testSettings()
	s.MdeoTag = "1.0\nimage: evil"
	if _, err := Generate(s, testCfg, Host{}); err == nil {
		t.Fatal("tag with a newline accepted")
	}
	bad := testCfg
	bad.ImagePrefix = "ghcr.io/x y"
	if _, err := Generate(testSettings(), bad, Host{}); err == nil {
		t.Fatal("image prefix with a space accepted")
	}
}

func TestGeneratedFileHasNoDangerousParts(t *testing.T) {
	s := testSettings()
	s.OptimizerInstances, s.SimInstances = 3, 2
	b, _ := Generate(s, testCfg, Host{})
	text := string(b)
	// No bind mounts, no privileged containers, no docker socket in the stack.
	for _, bad := range []string{"privileged", "docker.sock", "/var/run", "network_mode", "pid: host", "cap_add"} {
		if strings.Contains(text, bad) {
			t.Fatalf("generated file contains %q", bad)
		}
	}
	if c := strings.Count(text, "restart: unless-stopped"); c < 5 {
		t.Fatalf("restart policies missing (%d)", c)
	}
}

func TestReadHost(t *testing.T) {
	dir := t.TempDir()
	for id, list := range map[int]string{0: "0-3,8-11", 1: "4-7,12-15", 2: ""} {
		d := filepath.Join(dir, "sys/devices/system/node", fmt.Sprintf("node%d", id))
		os.MkdirAll(d, 0o755)
		os.WriteFile(filepath.Join(d, "cpulist"), []byte(list+"\n"), 0o644)
	}
	os.MkdirAll(filepath.Join(dir, "proc"), 0o755)
	os.WriteFile(filepath.Join(dir, "proc/meminfo"), []byte("MemTotal:       16384 kB\nMemFree: 1 kB\n"), 0o644)
	h := ReadHost(filepath.Join(dir, "sys"), filepath.Join(dir, "proc"))
	if len(h.Nodes) != 2 || h.Nodes[0].CPUList != "0-3,8-11" || h.CPUs != 16 || h.MemBytes != 16384*1024 {
		t.Fatalf("%+v", h)
	}
}
