package main

import (
	"bufio"
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"os/exec"
	"strings"
	"time"
)

// Container is one container of the stack as `docker compose ps` reports it.
type Container struct {
	Name    string `json:"Name"`
	Service string `json:"Service"`
	State   string `json:"State"`
	Health  string `json:"Health"`
	Image   string `json:"Image"`
	Status  string `json:"Status"`
}

// Docker is everything the manager does with Docker. The engine only talks to this interface,
// so its tests run against a fake.
type Docker interface {
	// Ping checks that the daemon answers.
	Ping(ctx context.Context) error
	// Config validates a compose file and returns the images it names.
	Config(ctx context.Context, file string) ([]string, error)
	// ImagePresent reports whether an image is in the local store.
	ImagePresent(ctx context.Context, image string) (bool, error)
	// Pull pulls one image.
	Pull(ctx context.Context, image string) error
	// Up creates or updates the stack (up -d --remove-orphans); recreate forces new containers.
	Up(ctx context.Context, file string, recreate bool) error
	// Down removes the stack's containers and networks. Never its volumes.
	Down(ctx context.Context, file string) error
	// Ps lists the stack's containers.
	Ps(ctx context.Context, file string) ([]Container, error)
}

// ComposeCLI implements Docker with the docker CLI and its compose plugin.
type ComposeCLI struct {
	Project  string
	EnvFiles []string
	// Log receives a short line per command (and its error output) for the action log.
	Log func(string)
}

func (c *ComposeCLI) log(s string) {
	if c.Log != nil {
		c.Log(s)
	}
}

func run(ctx context.Context, name string, args ...string) (string, error) {
	cmd := exec.CommandContext(ctx, name, args...)
	// An empty working directory: compose must never pick up a compose file by accident.
	cmd.Dir = "/"
	cmd.WaitDelay = 10 * time.Second
	var stdout, stderr bytes.Buffer
	cmd.Stdout, cmd.Stderr = &stdout, &stderr
	err := cmd.Run()
	if err != nil {
		msg := strings.TrimSpace(stderr.String())
		if msg == "" {
			msg = strings.TrimSpace(stdout.String())
		}
		if ctx.Err() != nil {
			return stdout.String(), fmt.Errorf("%s %s: %w (%s)", name, strings.Join(args, " "), ctx.Err(), tail(msg, 2000))
		}
		return stdout.String(), fmt.Errorf("%s: %s", err, tail(msg, 2000))
	}
	return stdout.String(), nil
}

func tail(s string, n int) string {
	if len(s) <= n {
		return s
	}
	return "…" + s[len(s)-n:]
}

func (c *ComposeCLI) compose(file string, args ...string) []string {
	a := []string{"compose", "--project-name", c.Project, "--file", file, "--progress", "quiet"}
	for _, e := range c.EnvFiles {
		a = append(a, "--env-file", e)
	}
	return append(a, args...)
}

func (c *ComposeCLI) Ping(ctx context.Context) error {
	_, err := run(ctx, "docker", "version", "--format", "{{.Server.Version}}")
	return err
}

func (c *ComposeCLI) Config(ctx context.Context, file string) ([]string, error) {
	if _, err := run(ctx, "docker", c.compose(file, "config", "--quiet")...); err != nil {
		return nil, err
	}
	out, err := run(ctx, "docker", c.compose(file, "config", "--images")...)
	if err != nil {
		return nil, err
	}
	var images []string
	seen := map[string]bool{}
	for _, l := range strings.Split(out, "\n") {
		l = strings.TrimSpace(l)
		if l != "" && !seen[l] {
			seen[l] = true
			images = append(images, l)
		}
	}
	return images, nil
}

func (c *ComposeCLI) ImagePresent(ctx context.Context, image string) (bool, error) {
	_, err := run(ctx, "docker", "image", "inspect", "--format", "{{.Id}}", image)
	if err == nil {
		return true, nil
	}
	if strings.Contains(strings.ToLower(err.Error()), "no such image") {
		return false, nil
	}
	return false, err
}

func (c *ComposeCLI) Pull(ctx context.Context, image string) error {
	c.log("docker pull " + image)
	_, err := run(ctx, "docker", "pull", "--quiet", image)
	return err
}

func (c *ComposeCLI) upArgs(file string, recreate bool) []string {
	args := []string{"up", "--detach", "--remove-orphans", "--pull", "missing"}
	if recreate {
		args = append(args, "--force-recreate")
	}
	return c.compose(file, args...)
}

func (c *ComposeCLI) Up(ctx context.Context, file string, recreate bool) error {
	c.log(fmt.Sprintf("docker compose up --detach --remove-orphans (recreate: %v)", recreate))
	_, err := run(ctx, "docker", c.upArgs(file, recreate)...)
	return err
}

// projectOnly addresses the project by name alone, without a compose file: compose finds its
// containers by their labels. Down and Ps use it so that they work even when the compose file or
// the env files would not load.
func (c *ComposeCLI) projectOnly(args ...string) []string {
	return append([]string{"compose", "--project-name", c.Project, "--progress", "quiet"}, args...)
}

// downArgs never contains --volumes (-v): the databases live in named volumes and must survive
// every action.
func (c *ComposeCLI) downArgs() []string {
	return c.projectOnly("down", "--remove-orphans", "--timeout", "60")
}

func (c *ComposeCLI) Down(ctx context.Context, file string) error {
	c.log("docker compose down --remove-orphans")
	_, err := run(ctx, "docker", c.downArgs()...)
	return err
}

func (c *ComposeCLI) Ps(ctx context.Context, file string) ([]Container, error) {
	out, err := run(ctx, "docker", c.projectOnly("ps", "--all", "--format", "json")...)
	if err != nil {
		return nil, err
	}
	return parsePs(out)
}

// parsePs reads `docker compose ps --format json`, which is one object per line in current
// compose versions and a single array in older ones.
func parsePs(out string) ([]Container, error) {
	out = strings.TrimSpace(out)
	if out == "" {
		return nil, nil
	}
	if strings.HasPrefix(out, "[") {
		var cs []Container
		err := json.Unmarshal([]byte(out), &cs)
		return cs, err
	}
	var cs []Container
	sc := bufio.NewScanner(strings.NewReader(out))
	sc.Buffer(make([]byte, 1<<20), 1<<20)
	for sc.Scan() {
		line := strings.TrimSpace(sc.Text())
		if line == "" {
			continue
		}
		var c Container
		if err := json.Unmarshal([]byte(line), &c); err != nil {
			return nil, err
		}
		cs = append(cs, c)
	}
	return cs, sc.Err()
}
