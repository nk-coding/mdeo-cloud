package main

import (
	"crypto/hmac"
	"crypto/pbkdf2"
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/base64"
	"errors"
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"sync"
	"time"
)

// Password hashes are "pbkdf2-sha256:<iterations>:<salt>:<key>" with unpadded URL-safe base64
// salt and key. The format has no '$', so it survives env files that interpolate.
const hashScheme = "pbkdf2-sha256"

// HashPassword returns the stored form of a password.
func HashPassword(password string, iterations int) (string, error) {
	salt := make([]byte, 16)
	if _, err := rand.Read(salt); err != nil {
		return "", err
	}
	key, err := pbkdf2.Key(sha256.New, password, salt, iterations, 32)
	if err != nil {
		return "", err
	}
	enc := base64.RawURLEncoding
	return fmt.Sprintf("%s:%d:%s:%s", hashScheme, iterations, enc.EncodeToString(salt), enc.EncodeToString(key)), nil
}

type passwordHash struct {
	iterations int
	salt, key  []byte
}

// parseHash checks the stored form and splits it.
func parseHash(h string) (passwordHash, error) {
	parts := strings.Split(strings.TrimSpace(h), ":")
	if len(parts) != 4 || parts[0] != hashScheme {
		return passwordHash{}, errors.New("expected pbkdf2-sha256:<iterations>:<salt>:<key> (generate it with `mdeo-manager hash-password`)")
	}
	it, err := strconv.Atoi(parts[1])
	if err != nil || it < 10000 || it > 10000000 {
		return passwordHash{}, errors.New("iterations must be between 10000 and 10000000")
	}
	enc := base64.RawURLEncoding
	salt, err1 := enc.DecodeString(parts[2])
	key, err2 := enc.DecodeString(parts[3])
	if err1 != nil || err2 != nil || len(salt) < 8 || len(key) != 32 {
		return passwordHash{}, errors.New("salt or key is not valid base64url (or has the wrong length)")
	}
	return passwordHash{it, salt, key}, nil
}

func (p passwordHash) verify(password string) bool {
	key, err := pbkdf2.Key(sha256.New, password, p.salt, p.iterations, len(p.key))
	return err == nil && subtle.ConstantTimeCompare(key, p.key) == 1
}

// BasicAuth protects every route. Verified credentials are remembered (as a keyed digest, for
// the life of the process) so the key derivation runs once per credential, not per request.
// Failures are serialized and delayed, which bounds guessing to about one attempt per second.
type BasicAuth struct {
	User  string
	hash  passwordHash
	key   []byte
	mu    sync.Mutex
	good  map[string]bool
	fails sync.Mutex
	// FailDelay is the pause after a wrong password.
	FailDelay time.Duration
}

// NewBasicAuth checks the hash and returns the middleware.
func NewBasicAuth(user, hash string) (*BasicAuth, error) {
	if user == "" {
		return nil, errors.New("the user name is empty")
	}
	p, err := parseHash(hash)
	if err != nil {
		return nil, err
	}
	key := make([]byte, 32)
	if _, err := rand.Read(key); err != nil {
		return nil, err
	}
	return &BasicAuth{User: user, hash: p, key: key, good: map[string]bool{}, FailDelay: time.Second}, nil
}

func (b *BasicAuth) digest(user, pass string) string {
	m := hmac.New(sha256.New, b.key)
	m.Write([]byte(user + "\x00" + pass))
	return string(m.Sum(nil))
}

func (b *BasicAuth) check(user, pass string) bool {
	d := b.digest(user, pass)
	b.mu.Lock()
	ok := b.good[d]
	b.mu.Unlock()
	if ok {
		return true
	}
	b.fails.Lock()
	defer b.fails.Unlock()
	userOK := subtle.ConstantTimeCompare([]byte(user), []byte(b.User)) == 1
	passOK := b.hash.verify(pass)
	if userOK && passOK {
		b.mu.Lock()
		if len(b.good) > 64 {
			b.good = map[string]bool{}
		}
		b.good[d] = true
		b.mu.Unlock()
		return true
	}
	time.Sleep(b.FailDelay)
	return false
}

// Wrap requires the credentials on every request to next.
func (b *BasicAuth) Wrap(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		user, pass, ok := r.BasicAuth()
		if !ok || !b.check(user, pass) {
			w.Header().Set("WWW-Authenticate", `Basic realm="mdeo manager", charset="UTF-8"`)
			http.Error(w, "authentication required", http.StatusUnauthorized)
			return
		}
		next.ServeHTTP(w, r)
	})
}
