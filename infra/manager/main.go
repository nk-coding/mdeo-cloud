// mdeo-manager operates one mdeo deployment (docker compose) through a small web page: the
// number and size of the optimizer and simulator instances, the image versions, restart, down
// and up. Nothing else. See README.md.
package main

import (
	"bufio"
	"context"
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"encoding/base64"
	"encoding/hex"
	"errors"
	"flag"
	"fmt"
	"log"
	"net"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"syscall"
	"time"
)

// version is set at build time (-ldflags "-X main.version=...").
var version = "dev"

func main() {
	cmd := "serve"
	if len(os.Args) > 1 {
		cmd = os.Args[1]
	}
	var err error
	switch cmd {
	case "serve":
		err = serve()
	case "hash-password":
		err = hashPasswordCmd()
	case "healthcheck":
		err = healthcheck()
	case "render":
		err = render(os.Args[2:])
	default:
		err = fmt.Errorf("unknown command %q (serve, hash-password, healthcheck, render)", cmd)
	}
	if err != nil {
		fmt.Fprintln(os.Stderr, "mdeo-manager:", err)
		os.Exit(1)
	}
}

func env(name, def string) string {
	if v, ok := os.LookupEnv(name); ok && strings.TrimSpace(v) != "" {
		return strings.TrimSpace(v)
	}
	return def
}

func listenAddr() string { return env("MANAGER_LISTEN", ":8090") }

var projectPattern = regexp.MustCompile(`^[a-z0-9][a-z0-9_-]{0,62}$`)

func stackConfig() StackConfig {
	return StackConfig{
		ImagePrefix:   env("IMAGE_PREFIX", "ghcr.io/nk-coding"),
		LBImage:       env("LB_IMAGE", "haproxy:3.2.24-alpine"),
		PostgresImage: env("POSTGRES_IMAGE", "postgres:17-alpine"),
	}
}

func serve() error {
	logger := log.New(os.Stderr, "", log.LstdFlags|log.LUTC)
	auth, err := NewBasicAuth(env("MANAGER_USER", "admin"), os.Getenv("MANAGER_PASSWORD_HASH"))
	if err != nil {
		return fmt.Errorf("MANAGER_PASSWORD_HASH: %w", err)
	}
	stateDir := env("STATE_DIR", "/state")
	project := env("STACK_PROJECT", "mdeo")
	if !projectPattern.MatchString(project) || project == "mdeo-manager" {
		return fmt.Errorf("STACK_PROJECT %q is not a usable compose project name (and must not be the manager's own)", project)
	}
	timeout, err := strconv.Atoi(env("HEALTH_TIMEOUT_SECONDS", "600"))
	if err != nil || timeout < 10 || timeout > 7200 {
		return errors.New("HEALTH_TIMEOUT_SECONDS must be between 10 and 7200")
	}
	if err := os.MkdirAll(stateDir, 0o700); err != nil {
		return err
	}
	// One manager per state directory, even if a second one is started by mistake.
	lock, err := os.OpenFile(filepath.Join(stateDir, "manager.lock"), os.O_CREATE|os.O_RDWR, 0o600)
	if err != nil {
		return err
	}
	if err := syscall.Flock(int(lock.Fd()), syscall.LOCK_EX|syscall.LOCK_NB); err != nil {
		return fmt.Errorf("another manager holds %s: %w", lock.Name(), err)
	}
	secrets := filepath.Join(stateDir, "secrets.env")
	if err := ensureSecrets(secrets); err != nil {
		return err
	}
	stackEnv := env("STACK_ENV_FILE", "/config/stack.env")
	if _, err := os.Stat(stackEnv); err != nil {
		logger.Printf("WARNING: %s is missing; every action will fail until it exists (%v)", stackEnv, err)
	}
	cfg := stackConfig()
	host := ReadHost(env("HOST_SYS", "/sys"), env("HOST_PROC", "/proc"))
	initial := DefaultSettings()
	initial.MdeoTag = env("INITIAL_MDEO_TAG", "")
	initial.SimTag = env("INITIAL_SIM_TAG", "")
	// The secrets come first, so the operator's stack.env can override them.
	docker := &ComposeCLI{Project: project, EnvFiles: []string{secrets, stackEnv}}
	engine, err := NewEngine(stateDir, docker, cfg, host, initial)
	if err != nil {
		return err
	}
	engine.HealthTimeout = time.Duration(timeout) * time.Second
	engine.Logf = logger.Printf
	docker.Log = func(s string) { logger.Print(s) }
	logger.Printf("mdeo-manager %s: project %q, state %s, images %s, %d CPUs, %d NUMA nodes", version, project, stateDir, cfg.ImagePrefix, host.CPUs, len(host.Nodes))

	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGTERM, syscall.SIGINT)
	defer stop()
	if err := engine.Reconcile(ctx); err != nil {
		return err
	}
	web := NewWeb(engine, version)
	srv := &http.Server{
		Addr:              listenAddr(),
		Handler:           auth.Wrap(web.Handler()),
		ReadHeaderTimeout: 10 * time.Second,
		ReadTimeout:       30 * time.Second,
		WriteTimeout:      60 * time.Second,
		IdleTimeout:       120 * time.Second,
		MaxHeaderBytes:    32 << 10,
	}
	errc := make(chan error, 1)
	go func() { errc <- srv.ListenAndServe() }()
	logger.Printf("listening on %s", srv.Addr)
	select {
	case err := <-errc:
		return err
	case <-ctx.Done():
		// A running action is cut off here; its journal lets the next start finish or undo it.
		sctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
		defer cancel()
		_ = srv.Shutdown(sctx)
		logger.Printf("stopped")
		return nil
	}
}

// ensureSecrets writes the secrets the backend needs to keep across restarts (JWT signing key,
// session cookie key), once. Without them the backend would make new ones on every start and
// invalidate every token and login.
func ensureSecrets(path string) error {
	if _, err := os.Stat(path); err == nil {
		return nil
	}
	key, err := rsa.GenerateKey(rand.Reader, 2048)
	if err != nil {
		return err
	}
	priv, err := x509.MarshalPKCS8PrivateKey(key)
	if err != nil {
		return err
	}
	pub, err := x509.MarshalPKIXPublicKey(&key.PublicKey)
	if err != nil {
		return err
	}
	session := make([]byte, 32)
	if _, err := rand.Read(session); err != nil {
		return err
	}
	content := fmt.Sprintf("# Generated once by mdeo-manager; stack.env overrides these.\nJWT_PRIVATE_KEY='%s'\nJWT_PUBLIC_KEY='%s'\nSESSION_ENCRYPTION_KEY='%s'\n",
		base64.StdEncoding.EncodeToString(priv), base64.StdEncoding.EncodeToString(pub), hex.EncodeToString(session))
	return WriteFileAtomic(path, []byte(content), 0o600)
}

func hashPasswordCmd() error {
	fmt.Fprintln(os.Stderr, "Password (one line on stdin):")
	line, err := bufio.NewReader(os.Stdin).ReadString('\n')
	if err != nil && line == "" {
		return errors.New("no password given")
	}
	pw := strings.TrimRight(line, "\r\n")
	if len(pw) < 12 {
		return errors.New("use at least 12 characters")
	}
	h, err := HashPassword(pw, 600000)
	if err != nil {
		return err
	}
	fmt.Println(h)
	return nil
}

// healthcheck is the container's health check: the server answers, and refuses a request
// without credentials.
func healthcheck() error {
	addr := listenAddr()
	host, port, err := net.SplitHostPort(addr)
	if err != nil {
		return err
	}
	if host == "" || host == "0.0.0.0" || host == "::" {
		host = "127.0.0.1"
	}
	c := http.Client{Timeout: 5 * time.Second}
	resp, err := c.Get("http://" + net.JoinHostPort(host, port) + "/")
	if err != nil {
		return err
	}
	resp.Body.Close()
	if resp.StatusCode != http.StatusUnauthorized {
		return fmt.Errorf("expected 401, got %d", resp.StatusCode)
	}
	return nil
}

// render prints the compose file generated from the defaults and the given flags, for checking
// it (docker compose config) without a manager.
func render(args []string) error {
	fs := flag.NewFlagSet("render", flag.ContinueOnError)
	s := DefaultSettings()
	fs.StringVar(&s.MdeoTag, "mdeo-tag", "latest", "mdeo image tag")
	fs.StringVar(&s.SimTag, "sim-tag", "latest", "simulator image tag")
	fs.BoolVar(&s.NumaPinning, "numa", false, "pin to NUMA nodes")
	ints := map[string]*int{}
	for _, l := range Limits {
		v := l.Get(s)
		ints[l.Key] = fs.Int(l.Key, v, l.Label)
	}
	nodes := fs.String("numa-nodes", "", "fake topology for -numa, e.g. '0-3;4-7' (default: this host's)")
	if err := fs.Parse(args); err != nil {
		return err
	}
	for _, l := range Limits {
		l.Set(&s, *ints[l.Key])
	}
	host := ReadHost(env("HOST_SYS", "/sys"), env("HOST_PROC", "/proc"))
	if *nodes != "" {
		host.Nodes = nil
		for i, list := range strings.Split(*nodes, ";") {
			host.Nodes = append(host.Nodes, NumaNode{ID: i, CPUList: list})
		}
	}
	b, err := Generate(s, stackConfig(), host)
	if err != nil {
		return err
	}
	_, err = os.Stdout.Write(b)
	return err
}
