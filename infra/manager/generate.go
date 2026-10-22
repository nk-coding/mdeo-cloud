package main

import (
	"bytes"
	"crypto/sha256"
	_ "embed"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"regexp"
	"strings"
	"text/template"
)

//go:embed stack.yaml.tmpl
var stackTemplate string

// StackConfig is what the generator needs besides the settings: where images come from. It is
// fixed per manager installation (environment of the manager container), not per action.
type StackConfig struct {
	// Registry and owner of the mdeo and simulator images, e.g. "ghcr.io/nk-coding".
	ImagePrefix   string
	LBImage       string
	PostgresImage string
}

// PluginServices are the mdeo plugin services (Node), in the order they are written.
var pluginServices = []struct {
	Name string
	Env  []kv
}{
	{"service-metamodel", nil},
	{"service-model", nil},
	{"service-script", []kv{{"SCRIPT_EXECUTION_SERVICE_URL", "http://script-execution:8080"}}},
	{"service-model-transformation", []kv{{"MODEL_TRANSFORMATION_EXECUTION_SERVICE_URL", "http://model-transformation-execution:8080"}}},
	{"service-config", nil},
	{"service-config-optimization", nil},
	// Runs are dispatched to node 0, which orchestrates them over its peers.
	{"service-config-mdeo", []kv{{"OPTIMIZER_EXECUTION_SERVICE_URL", "http://optimizer-execution-0:8080"}}},
	{"service-csv", nil},
	{"service-model-csv", nil},
}

type kv struct{ Key, Value string }

// OptimizerNode is one generated optimizer-execution service.
type OptimizerNode struct {
	Name, ID, URL, Peers string
	CPUSet               string
	NumaNode             int
}

// SimInstance is one generated palladio-sim-service service.
type SimInstance struct {
	Name     string
	CPUSet   string
	NumaNode int
}

type templateData struct {
	S                      Settings
	SettingsJSON           string
	Cfg                    StackConfig
	PostgresImage, LBImage string
	SimImage               string
	PluginServices         interface{}
	Optimizers             []OptimizerNode
	Sims                   []SimInstance
	OptimizerJavaOptions   string
	OptimizerDBConnections int
	LBConfig, LBConfigHash string
}

// Image is the image of one mdeo service at the configured tag.
func (d templateData) Image(service string) string {
	return fmt.Sprintf("%s/mdeo-%s:%s", d.Cfg.ImagePrefix, service, d.S.MdeoTag)
}

// OptimizerName is the compose service name of optimizer node i.
func OptimizerName(i int) string { return fmt.Sprintf("optimizer-execution-%d", i) }

// SimName is the compose service name of simulator instance i.
func SimName(i int) string { return fmt.Sprintf("palladio-sim-%d", i) }

// optimizerNodes builds the peer mesh: every node knows every other node's URL, and its own as
// NODE_URL, which is how the nodes find each other (there is no discovery).
func optimizerNodes(s Settings, pin []NumaNode) []OptimizerNode {
	nodes := make([]OptimizerNode, s.OptimizerInstances)
	for i := range nodes {
		var peers []string
		for j := 0; j < s.OptimizerInstances; j++ {
			if j != i {
				peers = append(peers, fmt.Sprintf("http://%s:8080", OptimizerName(j)))
			}
		}
		nodes[i] = OptimizerNode{
			Name:  OptimizerName(i),
			ID:    fmt.Sprint(i),
			URL:   fmt.Sprintf("http://%s:8080", OptimizerName(i)),
			Peers: strings.Join(peers, ","),
		}
		if len(pin) > 0 {
			n := pin[i%len(pin)]
			nodes[i].CPUSet, nodes[i].NumaNode = n.CPUList, n.ID
		}
	}
	return nodes
}

func simInstances(s Settings, pin []NumaNode) []SimInstance {
	sims := make([]SimInstance, s.SimInstances)
	for i := range sims {
		sims[i] = SimInstance{Name: SimName(i)}
		if len(pin) > 0 {
			n := pin[i%len(pin)]
			sims[i].CPUSet, sims[i].NumaNode = n.CPUList, n.ID
		}
	}
	return sims
}

// lbConfig is the HAProxy configuration of the simulator load balancer. Plain TCP: a script
// session is one long-lived WebSocket on one instance, so balancing connections by count
// (leastconn) spreads the sessions, and nothing has to be sticky beyond the connection itself.
func lbConfig(sims []SimInstance) string {
	var b strings.Builder
	b.WriteString(`global
    maxconn 20000

defaults
    mode tcp
    timeout connect 5s
    # Sessions ping every 30 s; a silent connection for this long is dead.
    timeout client 1h
    timeout server 1h
    timeout check 5s
    retries 2
    option redispatch

# Docker's DNS, so an instance that is recreated with a new address is followed.
resolvers docker
    nameserver dns 127.0.0.11:53
    resolve_retries 3
    timeout resolve 1s
    timeout retry 1s
    hold valid 10s
    hold nx 10s
    hold obsolete 30s

frontend palladio-sim
    bind :3022
    default_backend palladio-sim-instances

backend palladio-sim-instances
    balance leastconn
    option httpchk
    http-check send meth GET uri /
    http-check expect status 200
    default-server check inter 5s fall 3 rise 2 resolvers docker init-addr last,libc,none
`)
	for _, s := range sims {
		fmt.Fprintf(&b, "    server %s %s:3022\n", s.Name, s.Name)
	}
	b.WriteString(`
# Container health check: healthy while at least one instance is.
frontend health
    mode http
    bind :8405
    timeout client 5s
    monitor-uri /healthz
    monitor fail if { nbsrv(palladio-sim-instances) lt 1 }
`)
	return b.String()
}

// OptimizerJavaOptions are the JVM options of every optimizer node and its worker subprocesses.
// ActiveProcessorCount sizes the JVM's own pools (GC, JIT, common pool) for the threads it is
// given instead of for every CPU of the host.
func OptimizerJavaOptions(s Settings) string {
	cpus := s.OptimizerThreads
	if cpus < 2 {
		cpus = 2
	}
	return fmt.Sprintf("-Xmx%dg -XX:ActiveProcessorCount=%d -XX:+ExitOnOutOfMemoryError", s.OptimizerHeapGiB, cpus)
}

func templateFuncs() template.FuncMap {
	return template.FuncMap{
		// q writes any value as a double-quoted YAML scalar (JSON strings are valid YAML).
		"q": func(v interface{}) string {
			b, _ := json.Marshal(fmt.Sprint(v))
			return string(b)
		},
		"indent": func(n int, s string) string {
			pad := strings.Repeat(" ", n)
			lines := strings.Split(strings.TrimRight(s, "\n"), "\n")
			for i, l := range lines {
				if l != "" {
					lines[i] = pad + l
				}
			}
			return strings.Join(lines, "\n") + "\n"
		},
	}
}

var parsedTemplate = template.Must(template.New("stack").Funcs(templateFuncs()).Option("missingkey=error").Parse(stackTemplate))

// Generate renders the complete compose file of the stack. It is a pure function of its
// arguments: the same settings, configuration and topology give the same bytes.
func Generate(s Settings, cfg StackConfig, host Host) ([]byte, error) {
	if p := s.Validate(); len(p) > 0 {
		return nil, fmt.Errorf("invalid settings: %s", strings.Join(p, "; "))
	}
	if !imageRefPattern.MatchString(cfg.ImagePrefix) || !imageRefPattern.MatchString(cfg.LBImage) || !imageRefPattern.MatchString(cfg.PostgresImage) {
		return nil, fmt.Errorf("invalid image configuration %+v", cfg)
	}
	pin := PinningNodes(s, host)
	sims := simInstances(s, pin)
	lb := lbConfig(sims)
	sum := sha256.Sum256([]byte(lb))
	settingsJSON, _ := json.Marshal(s)
	d := templateData{
		S:                      s,
		SettingsJSON:           string(settingsJSON),
		Cfg:                    cfg,
		PostgresImage:          cfg.PostgresImage,
		LBImage:                cfg.LBImage,
		SimImage:               fmt.Sprintf("%s/palladio-sim-service:%s", cfg.ImagePrefix, s.SimTag),
		PluginServices:         pluginServices,
		Optimizers:             optimizerNodes(s, pin),
		Sims:                   sims,
		OptimizerJavaOptions:   OptimizerJavaOptions(s),
		OptimizerDBConnections: 100 + 12*s.OptimizerInstances,
		LBConfig:               lb,
		LBConfigHash:           hex.EncodeToString(sum[:]),
	}
	var out bytes.Buffer
	if err := parsedTemplate.Execute(&out, d); err != nil {
		return nil, err
	}
	return out.Bytes(), nil
}

// imageRefPattern is a conservative image reference (registry/path[:tag]); the configuration
// comes from the manager's environment, and this keeps anything odd out of the YAML.
var imageRefPattern = regexp.MustCompile(`^[a-z0-9]([a-z0-9._:/-]*[a-z0-9])?(:[A-Za-z0-9_][A-Za-z0-9_.-]{0,127})?(@sha256:[a-f0-9]{64})?$`)

// ExpectedServices lists every compose service of the generated stack with the image it must
// run, which is what "healthy" is checked against.
func ExpectedServices(s Settings, cfg StackConfig) map[string]string {
	d := templateData{S: s, Cfg: cfg}
	m := map[string]string{
		"postgres-backend":               cfg.PostgresImage,
		"postgres-script":                cfg.PostgresImage,
		"postgres-model-transformation":  cfg.PostgresImage,
		"postgres-optimizer":             cfg.PostgresImage,
		"backend":                        d.Image("backend"),
		"workbench":                      d.Image("workbench"),
		"script-execution":               d.Image("script-execution"),
		"model-transformation-execution": d.Image("model-transformation-execution"),
		"palladio-sim":                   cfg.LBImage,
	}
	for _, p := range pluginServices {
		m[p.Name] = d.Image(p.Name)
	}
	for i := 0; i < s.OptimizerInstances; i++ {
		m[OptimizerName(i)] = d.Image("optimizer-execution")
	}
	for i := 0; i < s.SimInstances; i++ {
		m[SimName(i)] = fmt.Sprintf("%s/palladio-sim-service:%s", cfg.ImagePrefix, s.SimTag)
	}
	return m
}
