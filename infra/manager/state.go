package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"time"
)

// State is everything the manager keeps on disk. It is written atomically after every step
// that changes it, so a manager that is killed at any point finds a consistent state on start.
type State struct {
	// Desired is what the stack should run.
	Desired Settings `json:"desired"`
	// Running says whether the stack should be up (Up) or down (Down).
	Running bool `json:"running"`
	// LastKnownGood is the last set of settings the whole stack was healthy with. A change that
	// does not become healthy goes back to it.
	LastKnownGood *Settings `json:"lastKnownGood,omitempty"`
	// Journal is the action in progress; it is present only while one runs (or was cut off).
	Journal *Journal `json:"journal,omitempty"`
	// LastResult is the outcome of the last finished action.
	LastResult *Result `json:"lastResult,omitempty"`
}

// Journal records an action in progress, enough to finish or undo it after a restart.
type Journal struct {
	Action string `json:"action"`
	// Phase: "prepare" (validating, pulling: nothing touched yet), "apply" (the stack is being
	// changed to Target), "rollback" (going back to the last known good settings).
	Phase           string    `json:"phase"`
	Target          Settings  `json:"target"`
	Previous        Settings  `json:"previous"`
	PreviousRunning bool      `json:"previousRunning"`
	Started         time.Time `json:"started"`
}

// Result is what the status page shows about an action.
type Result struct {
	Action   string    `json:"action"`
	Started  time.Time `json:"started"`
	Finished time.Time `json:"finished,omitempty"`
	OK       bool      `json:"ok"`
	Message  string    `json:"message"`
	Log      []string  `json:"log"`
}

const stateFile = "state.json"

// LoadState reads the state, or reports os.ErrNotExist when there is none yet.
func LoadState(dir string) (State, error) {
	var s State
	b, err := os.ReadFile(filepath.Join(dir, stateFile))
	if err != nil {
		return s, err
	}
	if err := json.Unmarshal(b, &s); err != nil {
		return s, fmt.Errorf("%s is corrupt: %w", filepath.Join(dir, stateFile), err)
	}
	return s, nil
}

// SaveState writes the state atomically.
func SaveState(dir string, s State) error {
	b, err := json.MarshalIndent(s, "", "  ")
	if err != nil {
		return err
	}
	return WriteFileAtomic(filepath.Join(dir, stateFile), append(b, '\n'), 0o600)
}

// WriteFileAtomic replaces path with data so that a reader (or a crash) sees either the old or
// the new content, never a mix: write a temporary file in the same directory, flush it to disk,
// rename it over the target, flush the directory.
func WriteFileAtomic(path string, data []byte, perm os.FileMode) error {
	dir := filepath.Dir(path)
	tmp, err := os.CreateTemp(dir, "."+filepath.Base(path)+".tmp-*")
	if err != nil {
		return err
	}
	name := tmp.Name()
	ok := false
	defer func() {
		if !ok {
			tmp.Close()
			os.Remove(name)
		}
	}()
	if _, err := tmp.Write(data); err != nil {
		return err
	}
	if err := tmp.Chmod(perm); err != nil {
		return err
	}
	if err := tmp.Sync(); err != nil {
		return err
	}
	if err := tmp.Close(); err != nil {
		return err
	}
	if err := os.Rename(name, path); err != nil {
		return err
	}
	ok = true
	d, err := os.Open(dir)
	if err != nil {
		return err
	}
	defer d.Close()
	if err := d.Sync(); err != nil && !errors.Is(err, os.ErrInvalid) {
		return err
	}
	return nil
}
