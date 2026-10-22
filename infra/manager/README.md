# mdeo-manager

A small web page that operates one mdeo deployment on one Docker host, for when nobody can log in
to the machine. It can do exactly this, and nothing else:

| Form / button | Effect |
| --- | --- |
| **Optimizer** | number of `optimizer-execution` nodes (1–32), worker threads per node (1–256), JVM heap per optimizer JVM (1–512 GiB) |
| **Palladio simulator** | number of `palladio-sim-service` instances (1–32), `SIM_THREADS`, `MAX_SESSIONS`, `SIM_RESULT_CACHE_SIZE`, `PCM_CACHE_MB`, `SIM_TIMEOUT_SECONDS` |
| **Versions** | image tag of all mdeo services, and separately of the simulator |
| **CPU placement** | pin optimizer and simulator instances round robin to the host's NUMA nodes (off by default) |
| **Restart** | recreate every container with the current settings |
| **Down** / **Up** | remove all containers (volumes stay) / start them again |

Plus a read-only status view: desired state, per-container state/health/image, the current
settings and the last known good ones, the steps and outcome of the last action, host CPUs,
memory and NUMA nodes. No shell, no logs, no file access, no arbitrary compose.

Go, standard library only (single static binary), in an image with the docker CLI and its
compose plugin. It drives the host's Docker daemon through the mounted socket.

## How the stack is made

The manager generates the complete compose file of the stack (`stack.yaml.tmpl` + settings →
`state/stack/compose.yaml`, deterministically) and runs it as the compose project `mdeo`. The
manager itself is the separate project `mdeo-manager` (`docker-compose.manager.yaml`), so taking
the stack down never stops it, and it never touches its own container.

- Images: `${IMAGE_PREFIX}/mdeo-<service>:<mdeo tag>` and `${IMAGE_PREFIX}/palladio-sim-service:<sim tag>`
  (`IMAGE_PREFIX=ghcr.io/nk-coding`); the databases use `postgres:17-alpine` and the simulator load
  balancer `haproxy:3.2.24-alpine` (official Docker Hub images).
- **Optimizer nodes** `optimizer-execution-0…N-1`: each gets `NODE_ID=i`,
  `NODE_URL=http://optimizer-execution-i:8080`, `PEERS` = every other node's URL,
  `WORKER_THREADS`, and `JAVA_TOOL_OPTIONS=-Xmx<heap>g -XX:ActiveProcessorCount=<threads> -XX:+ExitOnOutOfMemoryError`
  (inherited by the worker subprocess each run starts). There is no discovery: the peer list is
  the mesh. `service-config-mdeo` sends every run to node 0, which orchestrates it over its peers
  (the run's `resources` block caps how many nodes and threads it takes).
- **Simulator instances** `palladio-sim-0…S-1` behind one HAProxy service `palladio-sim`. Register
  the plugin once in the workbench (**Settings → Plugins**) as **`http://palladio-sim:3022/`**. The
  backend hands that URL to every execution that opens a session; each session is one WebSocket,
  so the load balancer balances connections (TCP, least connections first) and the sessions
  spread over the instances. Any instance verifies session tokens itself (JWKS of the backend).
  HTTP health checks take an instance out of rotation while it restarts; a session on an instance
  that goes away is reopened by the execution and lands on another one. Each instance has its own
  result cache.
- Every service has a health check and `restart: unless-stopped`, logs are capped (json-file,
  5 × 50 MB), optimizer and simulator containers get `oom_score_adj: 500` (the kernel kills them
  before the databases), the databases stop with a 60 s grace period, `postgres-optimizer` gets
  `max_connections` for the node count.
- `state/secrets.env` is generated once (JWT key pair, session cookie key), so tokens and logins
  survive restarts. `config/stack.env` (yours) can override it.

## Reliability

- **One action at a time** (in-process lock, plus a file lock on the state directory against a
  second manager). The page shows a running action and refuses others.
- **Fail as late as possible.** A change is validated (ranges, tag syntax), the compose file is
  generated and checked with `docker compose config`, and every image that is not present is
  pulled — all before anything running is touched. A bad tag or an invalid configuration leaves
  the deployment exactly as it was.
- **Apply and verify.** `docker compose up -d --remove-orphans`, then wait (`HEALTH_TIMEOUT_SECONDS`,
  default 600) until every expected service runs the expected image and is healthy, and nothing
  else runs.
- **Automatic rollback.** A change (versions, scaling, restart, up) that is not healthy in time
  goes back to the **last known good** settings — the last ones the whole stack was healthy with
  — and reports it. If there is none (the very first deploy), it reports and leaves things as
  they are.
- **Crash-safe.** `state/state.json` (desired settings, up/down, last known good, last result and
  a journal of the running action) is written atomically after every step. When the manager
  starts (`restart: always`, also after a reboot) it finishes an interrupted change or rolls it
  back, finishes an interrupted down, and then makes the stack match the desired state (up and
  healthy, or down). After a reboot Docker has already restarted the containers and this changes
  nothing.
- **Never** `down -v`, never prunes images, never deletes volumes, never updates itself.

## Bootstrap (once, with SSH)

Prerequisites on the server: Docker Engine with the compose plugin, `systemctl enable docker`,
and the images published (see *Images* below).

```bash
sudo mkdir -p /opt/mdeo/config && cd /opt/mdeo
# copy from a checkout of this repository (e.g. scp infra/manager/{docker-compose.manager.yaml,manager.env.example,stack.env.example} server:/tmp/)
sudo cp /tmp/docker-compose.manager.yaml .
sudo cp /tmp/manager.env.example .env
sudo cp /tmp/stack.env.example config/stack.env
sudo chmod 600 .env config/stack.env

# the basic-auth password hash (type the password, Enter)
docker run --rm -i ghcr.io/nk-coding/mdeo-manager:<tag> hash-password

sudo nano .env               # MANAGER_TAG, MANAGER_PASSWORD_HASH, INITIAL_MDEO_TAG, INITIAL_SIM_TAG
sudo nano config/stack.env   # DATABASE_PASSWORD, ADMIN_PASSWORD, CORS_ALLOWED_HOSTS (, WORKBENCH_PORT, COOKIE_SECURE)

sudo docker compose -f docker-compose.manager.yaml up -d
```

Then open `http://<server>:8090/`, set the optimizer/simulator sizes, tick and press **Up**, and
wait until it reports healthy (first start pulls all images). In the workbench, log in as the
admin from `stack.env`, add the plugin `http://palladio-sim:3022/` under **Settings → Plugins**,
and enable it in the project.

If the ghcr.io packages are private, the manager needs credentials:

```bash
sudo mkdir -p /opt/mdeo/docker-config
printf '{"auths":{"ghcr.io":{"auth":"%s"}}}\n' "$(printf '%s:%s' nk-coding "$GHCR_READ_TOKEN" | base64 -w0)" \
  | sudo tee /opt/mdeo/docker-config/config.json >/dev/null
sudo chmod 600 /opt/mdeo/docker-config/config.json
```

(and `docker login ghcr.io` on the host for pulling the manager image itself).

**Security:** the page is plain HTTP with basic auth (PBKDF2-SHA256 hash, failed attempts slowed
to about one per second, form tokens and a same-origin check against cross-site posts). The
container has the Docker socket, i.e. root on the host. Bind it to a private interface
(`MANAGER_BIND`) or firewall the port where possible.

**Updating the manager** (needs SSH): change `MANAGER_TAG` in `.env`, `docker compose -f
docker-compose.manager.yaml up -d`. The stack keeps running meanwhile.

## Sizing

Every optimizer worker thread opens its own simulator session, and a session runs one simulation
at a time. So `optimizer instances × threads` is the number of simulations that can be asked for
at once, and `simulator instances × SIM_THREADS` the number that run at once; the page warns when
the sessions (`instances × MAX_SESSIONS`) cannot hold all worker threads. The simulations do the
CPU work; the optimizer threads mostly wait for them. A starting point for a 4-socket host with
C cores: 4 or 8 simulator instances with `SIM_THREADS` ≈ 0.9·C / instances, 8 optimizer
instances with threads ≈ C / 8, heap 16 GiB, NUMA pinning on (instance counts multiples of 4).

Changing optimizer settings recreates all optimizer nodes (their peer lists change), and changing
simulator settings recreates the simulators: runs in progress fail.

## Development

```bash
docker run --rm -v "$PWD/infra/manager:/src" -w /src -e CGO_ENABLED=0 golang:1.25.14-alpine go test ./...
infra/manager/test/integration.sh   # end to end against a fake stack (local registry, stand-in images)
docker run --rm ghcr.io/nk-coding/mdeo-manager:<tag> render -mdeo-tag 1.0.0 -sim-tag 0.2.0 -optimizerInstances 8   # print a generated stack
```

| File | Contents |
| --- | --- |
| `settings.go` | the settings, their ranges and sizing warnings |
| `generate.go`, `stack.yaml.tmpl` | the compose generator and its template |
| `engine.go`, `state.go` | actions, health check, rollback, journal and recovery, atomic state |
| `docker.go` | the docker CLI calls (`compose config/up/down/ps`, `pull`, `image inspect`) |
| `web.go`, `page.html.tmpl`, `auth.go` | the page, the forms, basic auth |
| `host.go` | CPUs, memory and NUMA nodes from `/sys` and `/proc` |
| `main.go` | `serve`, `hash-password`, `healthcheck`, `render` |
