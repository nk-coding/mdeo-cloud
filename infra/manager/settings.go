package main

import (
	"fmt"
	"regexp"
)

// Settings is everything the manager lets an operator change. The deployment is generated from
// it (generate.go) and nothing else, so two equal Settings always give the same compose file.
type Settings struct {
	// Image tag of every mdeo service (ghcr.io/<owner>/mdeo-<service>:<tag>).
	MdeoTag string `json:"mdeoTag"`
	// Image tag of the Palladio simulation service (ghcr.io/<owner>/palladio-sim-service:<tag>).
	SimTag string `json:"simTag"`

	// optimizer-execution nodes, all peers of each other.
	OptimizerInstances int `json:"optimizerInstances"`
	// WORKER_THREADS of every optimizer node.
	OptimizerThreads int `json:"optimizerThreads"`
	// -Xmx of every optimizer JVM (the service and the worker subprocess it starts per run).
	OptimizerHeapGiB int `json:"optimizerHeapGiB"`

	// palladio-sim-service instances behind the load balancer.
	SimInstances int `json:"simInstances"`
	// SIM_THREADS: simulations at once per instance.
	SimThreads int `json:"simThreads"`
	// MAX_SESSIONS: script sessions per instance (every optimizer worker thread holds one).
	SimMaxSessions int `json:"simMaxSessions"`
	// SIM_RESULT_CACHE_SIZE: cached evaluations per instance.
	SimResultCacheSize int `json:"simResultCacheSize"`
	// PCM_CACHE_MB: memory bound of the cached evaluations per instance.
	SimCacheMB int `json:"simCacheMB"`
	// SIM_TIMEOUT_SECONDS: hard wall-clock limit of one simulation (0: none).
	SimTimeoutSeconds int `json:"simTimeoutSeconds"`

	// Pin optimizer and simulator instances round robin to the host's NUMA nodes.
	NumaPinning bool `json:"numaPinning"`
}

// DefaultSettings are deliberately small: the stack starts on any machine, and the operator
// scales it up in the UI.
func DefaultSettings() Settings {
	return Settings{
		OptimizerInstances: 2,
		OptimizerThreads:   8,
		OptimizerHeapGiB:   8,
		SimInstances:       1,
		SimThreads:         16,
		SimMaxSessions:     256,
		SimResultCacheSize: 4096,
		SimCacheMB:         1024,
		SimTimeoutSeconds:  600,
	}
}

// Limit is the accepted range of one numeric setting.
type Limit struct {
	Key, Label string
	Min, Max   int
	get        func(*Settings) *int
}

// Limits lists every numeric setting with its range. The UI renders its fields from it, and
// Validate checks against it, so the two cannot disagree.
var Limits = []Limit{
	{"optimizerInstances", "Optimizer instances", 1, 32, func(s *Settings) *int { return &s.OptimizerInstances }},
	{"optimizerThreads", "Worker threads per optimizer instance", 1, 256, func(s *Settings) *int { return &s.OptimizerThreads }},
	{"optimizerHeapGiB", "JVM heap per optimizer JVM (GiB)", 1, 512, func(s *Settings) *int { return &s.OptimizerHeapGiB }},
	{"simInstances", "Simulator instances", 1, 32, func(s *Settings) *int { return &s.SimInstances }},
	{"simThreads", "Simulation threads per instance (SIM_THREADS)", 1, 512, func(s *Settings) *int { return &s.SimThreads }},
	{"simMaxSessions", "Sessions per instance (MAX_SESSIONS)", 1, 4096, func(s *Settings) *int { return &s.SimMaxSessions }},
	{"simResultCacheSize", "Cached evaluations per instance (SIM_RESULT_CACHE_SIZE)", 1, 1000000, func(s *Settings) *int { return &s.SimResultCacheSize }},
	{"simCacheMB", "Evaluation cache memory per instance, MB (PCM_CACHE_MB)", 16, 262144, func(s *Settings) *int { return &s.SimCacheMB }},
	{"simTimeoutSeconds", "Wall-clock limit per simulation, s (SIM_TIMEOUT_SECONDS, 0 = none)", 0, 86400, func(s *Settings) *int { return &s.SimTimeoutSeconds }},
}

// LimitByKey finds a numeric setting by its form/JSON key.
func LimitByKey(key string) (Limit, bool) {
	for _, l := range Limits {
		if l.Key == key {
			return l, true
		}
	}
	return Limit{}, false
}

// Get returns the setting's value in s.
func (l Limit) Get(s Settings) int { return *l.get(&s) }

// Set stores v in s without checking it.
func (l Limit) Set(s *Settings, v int) { *l.get(s) = v }

// tagPattern is Docker's tag grammar.
var tagPattern = regexp.MustCompile(`^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$`)

// ValidTag reports whether t can be used as an image tag.
func ValidTag(t string) bool { return tagPattern.MatchString(t) }

// Validate checks every setting against its range. It returns all problems at once.
func (s Settings) Validate() []string {
	var problems []string
	if !ValidTag(s.MdeoTag) {
		problems = append(problems, fmt.Sprintf("mdeo version %q is not a valid image tag (letters, digits, '_', '.', '-'; at most 128)", s.MdeoTag))
	}
	if !ValidTag(s.SimTag) {
		problems = append(problems, fmt.Sprintf("simulator version %q is not a valid image tag (letters, digits, '_', '.', '-'; at most 128)", s.SimTag))
	}
	for _, l := range Limits {
		v := l.Get(s)
		if v < l.Min || v > l.Max {
			problems = append(problems, fmt.Sprintf("%s must be between %d and %d (got %d)", l.Label, l.Min, l.Max, v))
		}
	}
	return problems
}

// Warnings are sizing hints that do not block a change.
func (s Settings) Warnings() []string {
	var w []string
	demand := s.OptimizerInstances * s.OptimizerThreads
	if s.SimInstances*s.SimMaxSessions < demand {
		w = append(w, fmt.Sprintf("Every optimizer worker thread holds one simulator session: %d threads, but only %d × %d sessions are allowed. Runs using all threads would be refused sessions.",
			demand, s.SimInstances, s.SimMaxSessions))
	}
	if s.SimInstances*s.SimThreads < demand {
		w = append(w, fmt.Sprintf("%d optimizer worker threads can ask for %d simulations at once, the simulators run %d × %d = %d: simulations will queue.",
			demand, demand, s.SimInstances, s.SimThreads, s.SimInstances*s.SimThreads))
	}
	return w
}
