package main

import (
	"bufio"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strconv"
	"strings"
)

// NumaNode is one NUMA node of the host and the CPUs it holds, as the kernel lists them
// ("0-23,96-119").
type NumaNode struct {
	ID      int
	CPUList string
}

// Host is what the manager knows about the machine, for pinning and for the status page.
type Host struct {
	CPUs     int
	MemBytes int64
	Nodes    []NumaNode
}

var cpuListPattern = regexp.MustCompile(`^[0-9]+(-[0-9]+)?(,[0-9]+(-[0-9]+)?)*$`)

// ReadHost reads the topology from sysfs and procfs. /sys and /proc inside a container describe
// the host (the container's cgroup limits aside), which is what pinning needs. Anything that
// cannot be read is left empty; pinning is then skipped.
func ReadHost(sysRoot, procRoot string) Host {
	h := Host{}
	dirs, _ := filepath.Glob(filepath.Join(sysRoot, "devices/system/node/node[0-9]*"))
	for _, d := range dirs {
		id, err := strconv.Atoi(strings.TrimPrefix(filepath.Base(d), "node"))
		if err != nil {
			continue
		}
		b, err := os.ReadFile(filepath.Join(d, "cpulist"))
		if err != nil {
			continue
		}
		list := strings.TrimSpace(string(b))
		if list == "" || !cpuListPattern.MatchString(list) {
			// A node without CPUs (memory only) cannot run anything.
			continue
		}
		h.Nodes = append(h.Nodes, NumaNode{ID: id, CPUList: list})
		h.CPUs += countCPUs(list)
	}
	sort.Slice(h.Nodes, func(i, j int) bool { return h.Nodes[i].ID < h.Nodes[j].ID })
	if h.CPUs == 0 {
		if b, err := os.ReadFile(filepath.Join(procRoot, "cpuinfo")); err == nil {
			h.CPUs = strings.Count(string(b), "\nprocessor") + boolInt(strings.HasPrefix(string(b), "processor"))
		}
	}
	if f, err := os.Open(filepath.Join(procRoot, "meminfo")); err == nil {
		defer f.Close()
		sc := bufio.NewScanner(f)
		for sc.Scan() {
			fields := strings.Fields(sc.Text())
			if len(fields) >= 2 && fields[0] == "MemTotal:" {
				kb, _ := strconv.ParseInt(fields[1], 10, 64)
				h.MemBytes = kb * 1024
			}
		}
	}
	return h
}

func boolInt(b bool) int {
	if b {
		return 1
	}
	return 0
}

func countCPUs(list string) int {
	n := 0
	for _, part := range strings.Split(list, ",") {
		lo, hi, found := strings.Cut(part, "-")
		a, _ := strconv.Atoi(lo)
		if !found {
			n++
			continue
		}
		b, _ := strconv.Atoi(hi)
		if b >= a {
			n += b - a + 1
		}
	}
	return n
}

// PinningNodes returns the nodes instances are pinned to, or nil when pinning is off or the host
// has fewer than two NUMA nodes with CPUs (pinning would change nothing).
func PinningNodes(s Settings, h Host) []NumaNode {
	if !s.NumaPinning || len(h.Nodes) < 2 {
		return nil
	}
	return h.Nodes
}
