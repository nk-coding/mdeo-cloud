#!/usr/bin/env bash
#
# End-to-end test of the manager against a fake stack, on any Docker host:
#
#   infra/manager/test/integration.sh            (from the repository root or anywhere)
#
# What runs: a local registry (registry:2) holding stand-in images under every mdeo image name
# (test/fake: busybox answering the health checks), the real Postgres and HAProxy images, the
# simulator image given in SIM_IMAGE (default: a fake; set e.g. SIM_IMAGE=palladio-sim-service:local
# to put the real service behind the load balancer), and the manager image built from this
# checkout, started with the real docker-compose.manager.yaml. Everything is named mdeoit*, uses
# ports REGISTRY_PORT, MANAGER_PORT, WORKBENCH_PORT, and is removed at the end (KEEP=1 keeps it).
#
set -Eeuo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$HERE/../../.." && pwd)"
REGISTRY_PORT="${REGISTRY_PORT:-15055}"
MANAGER_PORT="${MANAGER_PORT:-18190}"
WORKBENCH_PORT="${WORKBENCH_PORT:-18180}"
WORK="${WORK_DIR:-$(mktemp -d)}"
SIM_IMAGE="${SIM_IMAGE:-}"
# Abort when the host gets short of memory (the test itself needs little; the host may not).
MIN_AVAILABLE_GB="${MIN_AVAILABLE_GB:-3}"
PROJECT=mdeoit
MPROJECT=mdeoit-manager
REG="localhost:$REGISTRY_PORT/nk-coding"
USER_NAME=ops
PASSWORD='integration-test-password'
SERVICES=(backend workbench service-metamodel service-model service-script service-model-transformation
    service-config service-config-optimization service-config-mdeo service-csv service-model-csv
    script-execution model-transformation-execution optimizer-execution)

pass=0
fail=0
ok() { echo "  PASS $*"; pass=$((pass + 1)); }
bad() { echo "  FAIL $*"; fail=$((fail + 1)); }
check() { local what="$1"; shift; if "$@"; then ok "$what"; else bad "$what"; fi; }

cleanup() {
    [ -n "${KEEP:-}" ] && { echo "KEEP=1: leaving everything in place ($WORK)"; return; }
    echo "== cleanup"
    docker compose -p "$MPROJECT" -f "$WORK/docker-compose.manager.yaml" down --remove-orphans >/dev/null 2>&1 || true
    docker compose -p "$PROJECT" down --remove-orphans --volumes >/dev/null 2>&1 || true
    docker rm -f -v mdeoit-registry >/dev/null 2>&1 || true
    docker volume ls -q --filter "label=com.docker.compose.project=$PROJECT" | xargs -r docker volume rm >/dev/null 2>&1 || true
    docker images --format '{{.Repository}}:{{.Tag}}' | grep -E "^(localhost:$REGISTRY_PORT/|mdeoit-fake:|ghcr.io/nk-coding/mdeo-manager:it-test$)" | xargs -r docker rmi -f >/dev/null 2>&1 || true
    docker run --rm -v "$WORK:/w" busybox:1.37 rm -rf /w/state /w/docker-config >/dev/null 2>&1 || true
    rm -rf "$WORK"
}
trap cleanup EXIT

curl_() { curl -sS -u "$USER_NAME:$PASSWORD" "$@"; }
status() { curl_ "http://127.0.0.1:$MANAGER_PORT/status.json"; }
jq_() { python3 -c "import json,sys; d=json.load(sys.stdin); print(eval(sys.argv[1]))" "$1"; }
csrf() { curl_ "http://127.0.0.1:$MANAGER_PORT/" | sed -n 's/.*name="csrf" value="\([0-9a-f]*\)".*/\1/p' | head -1; }

# post PATH field=value... : submits a form with token and confirmation; prints the redirect.
post() {
    local path="$1"; shift
    local args=(--data-urlencode "csrf=$(csrf)" --data-urlencode "confirm=yes")
    for kv in "$@"; do args+=(--data-urlencode "$kv"); done
    curl_ -o /dev/null -w '%{redirect_url}' "${args[@]}" "http://127.0.0.1:$MANAGER_PORT$path"
}

# wait_done: waits until no action runs and prints "<ok> <message>" of the last result.
wait_done() {
    local deadline=$((SECONDS + ${1:-400}))
    sleep 2
    while ((SECONDS < deadline)); do
        local s
        s="$(status 2>/dev/null || true)"
        if [ -n "$s" ] && [ "$(jq_ 'd["busy"]' <<<"$s")" = False ]; then
            jq_ 'str(d["lastResult"]["ok"]) + " " + d["lastResult"]["message"]' <<<"$s"
            return 0
        fi
        memcheck
        sleep 2
    done
    echo "TIMEOUT"
}

memcheck() {
    local avail
    avail=$(awk '/^MemAvailable:/ {print int($2 / 1048576)}' /proc/meminfo)
    if [ "$avail" -lt "$MIN_AVAILABLE_GB" ]; then
        echo "ABORT: only ${avail} GB of memory available (MIN_AVAILABLE_GB=$MIN_AVAILABLE_GB)"
        exit 2
    fi
}
container_ids() { docker ps -q --filter "label=com.docker.compose.project=$PROJECT" | sort | tr '\n' ' '; }
image_of() { docker inspect -f '{{.Config.Image}}' "$PROJECT-$1-1" 2>/dev/null; }
count_service() { docker ps --filter "label=com.docker.compose.project=$PROJECT" --format '{{.Label "com.docker.compose.service"}}' | grep -c "^$1" || true; }
current_has() { status | python3 -c "import json,sys; d=json.load(sys.stdin); c=d.get('current') or {}; sys.exit(0 if any(sys.argv[1] in l for l in c.get('log') or []) else 1)" "$1"; }
via_lb() { docker exec "$PROJECT-workbench-1" wget -q -O - http://palladio-sim:3022/ 2>/dev/null; }

echo "== work dir $WORK"
# A state directory left by an earlier run would make the manager resume that run.
if [ -n "$(ls -A "$WORK" 2>/dev/null)" ]; then echo "work dir $WORK is not empty"; exit 1; fi
for p in "$REGISTRY_PORT" "$MANAGER_PORT" "$WORKBENCH_PORT"; do
    if ss -ltn | grep -q ":$p "; then echo "port $p is in use"; exit 1; fi
done

echo "== registry and stand-in images"
docker run -d --name mdeoit-registry -p "127.0.0.1:$REGISTRY_PORT:5000" registry:2 >/dev/null
docker build -q -t mdeoit-fake:good "$HERE/fake" >/dev/null
docker build -q -t mdeoit-fake:sick --build-arg SICK=1 "$HERE/fake" >/dev/null
for s in "${SERVICES[@]}"; do
    for t in t1 t2; do docker tag mdeoit-fake:good "$REG/mdeo-$s:$t"; done
    docker tag mdeoit-fake:sick "$REG/mdeo-$s:sick"
done
for t in t1 t2; do docker tag "${SIM_IMAGE:-mdeoit-fake:good}" "$REG/palladio-sim-service:$t"; done
docker tag mdeoit-fake:sick "$REG/palladio-sim-service:sick"
docker images --format '{{.Repository}}:{{.Tag}}' | grep "^$REG/" | while read -r i; do docker push -q "$i" >/dev/null; done
# Pushed; the local copies go, so the manager has to pull.
docker images --format '{{.Repository}}:{{.Tag}}' | grep "^$REG/" | xargs -r docker rmi >/dev/null

echo "== manager image"
docker build -q -f "$REPO/infra/manager/Dockerfile" --build-arg SERVICE_VERSION=it-test -t ghcr.io/nk-coding/mdeo-manager:it-test "$REPO" >/dev/null
HASH="$(printf '%s\n' "$PASSWORD" | docker run --rm -i ghcr.io/nk-coding/mdeo-manager:it-test hash-password 2>/dev/null)"

mkdir -p "$WORK/config"
cp "$REPO/infra/manager/docker-compose.manager.yaml" "$WORK/"
cat > "$WORK/.env" <<EOF
MANAGER_TAG=it-test
MANAGER_PORT=$MANAGER_PORT
MANAGER_BIND=127.0.0.1
MANAGER_USER=$USER_NAME
MANAGER_PASSWORD_HASH=$HASH
INITIAL_MDEO_TAG=t1
INITIAL_SIM_TAG=t1
STACK_PROJECT=$PROJECT
IMAGE_PREFIX=$REG
HEALTH_TIMEOUT_SECONDS=100
HEAL_INTERVAL_SECONDS=5
HEAL_GRACE_SECONDS=15
HEAL_BACKOFF_BASE_SECONDS=10
EOF
cat > "$WORK/config/stack.env" <<EOF
DATABASE_PASSWORD=it-db-password
ADMIN_PASSWORD=it-admin-password
CORS_ALLOWED_HOSTS=mdeo.example.org
WORKBENCH_PORT=$WORKBENCH_PORT
WORKBENCH_BIND=127.0.0.1
EOF
manager() { docker compose -p "$MPROJECT" -f "$WORK/docker-compose.manager.yaml" "$@"; }
manager up -d
for _ in $(seq 60); do curl -s -o /dev/null "http://127.0.0.1:$MANAGER_PORT/" && break; sleep 1; done
wait_done >/dev/null

memcheck; echo "== 1. basic auth on every route"
for r in "GET /" "GET /status.json" "GET /nope" "POST /action/up" "POST /action/down" "POST /apply/versions"; do
    set -- $r
    code=$(curl -s -o /dev/null -w '%{http_code}' -X "$1" "http://127.0.0.1:$MANAGER_PORT$2")
    check "$r without credentials -> 401 ($code)" [ "$code" = 401 ]
    code=$(curl -s -o /dev/null -w '%{http_code}' -u "$USER_NAME:wrong" -X "$1" "http://127.0.0.1:$MANAGER_PORT$2")
    check "$r with a wrong password -> 401 ($code)" [ "$code" = 401 ]
done
code=$(curl_ -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$MANAGER_PORT/")
check "GET / with credentials -> 200" [ "$code" = 200 ]
loc=$(curl_ -s -o /dev/null -w '%{redirect_url}' -X POST "http://127.0.0.1:$MANAGER_PORT/action/up")
check "POST without form token is refused" grep -q "err=" <<<"$loc"
check "a fresh manager keeps the stack down" [ "$(status | jq_ 'd["running"]')" = False ]

memcheck; echo "== 2. up (pulls everything, default 2 optimizers, 1 simulator)"
post /action/up >/dev/null
r="$(wait_done)"; echo "     $r"
check "up succeeds" grep -q '^True' <<<"$r"
check "stack healthy" [ "$(status | jq_ 'd["healthy"]')" = True ]
check "21 services" [ "$(docker ps -q --filter "label=com.docker.compose.project=$PROJECT" | wc -l)" = 21 ]
check "simulator reachable through the load balancer" bash -c "docker exec $PROJECT-workbench-1 wget -q -O /dev/null http://palladio-sim:3022/"
docker exec "$PROJECT-postgres-backend-1" psql -U mdeo -d mdeo -qc "create table it_marker(v text); insert into it_marker values ('kept');" >/dev/null
check "manager-generated JWT key reaches the backend" bash -c "docker exec $PROJECT-backend-1 sh -c 'test \${#JWT_PRIVATE_KEY} -gt 1000'"

memcheck; echo "== 3. optimizer scale up to 3 x 6 threads"
post /apply/optimizer optimizerInstances=3 optimizerThreads=6 optimizerHeapGiB=2 >/dev/null
r="$(wait_done)"; echo "     $r"
check "scale up succeeds" grep -q '^True' <<<"$r"
check "3 optimizer nodes" [ "$(count_service optimizer-execution-)" = 3 ]
envs="$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' $PROJECT-optimizer-execution-2-1)"
check "node 2 PEERS lists the other two" grep -qx 'PEERS=http://optimizer-execution-0:8080,http://optimizer-execution-1:8080' <<<"$envs"
check "node 2 NODE_URL" grep -qx 'NODE_URL=http://optimizer-execution-2:8080' <<<"$envs"
check "node 2 WORKER_THREADS=6" grep -qx 'WORKER_THREADS=6' <<<"$envs"
check "node 2 heap and processor count" grep -qx 'JAVA_TOOL_OPTIONS=-Xmx2g -XX:ActiveProcessorCount=6 -XX:+ExitOnOutOfMemoryError' <<<"$envs"

memcheck; echo "== 4. optimizer scale down to 1, simulator to 2 instances"
post /apply/optimizer optimizerInstances=1 optimizerThreads=6 optimizerHeapGiB=2 >/dev/null
r="$(wait_done)"; echo "     $r"
check "scale down succeeds" grep -q '^True' <<<"$r"
check "1 optimizer node" [ "$(count_service optimizer-execution-)" = 1 ]
check "node 0 has no peers" bash -c "docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' $PROJECT-optimizer-execution-0-1 | grep -qx 'PEERS='"
post /apply/simulator simInstances=2 simThreads=4 simMaxSessions=64 simResultCacheSize=100 simCacheMB=64 simTimeoutSeconds=120 >/dev/null
r="$(wait_done)"; echo "     $r"
check "simulator change succeeds" grep -q '^True' <<<"$r"
check "2 simulator instances" [ "$(count_service palladio-sim-[0-9])" = 2 ]
check "SIM_THREADS=4 on instance 1" bash -c "docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' $PROJECT-palladio-sim-1-1 | grep -qx 'SIM_THREADS=4'"
sleep 12
okcount=0; for _ in 1 2 3 4 5 6; do via_lb >/dev/null && okcount=$((okcount + 1)); done
check "load balancer answers with 2 instances ($okcount/6)" [ "$okcount" = 6 ]
docker stop "$PROJECT-palladio-sim-0-1" >/dev/null
sleep 20
okcount=0; for _ in 1 2 3 4 5 6; do via_lb >/dev/null && okcount=$((okcount + 1)); done
check "load balancer routes around a stopped instance ($okcount/6)" [ "$okcount" = 6 ]
docker start "$PROJECT-palladio-sim-0-1" >/dev/null

memcheck; echo "== 5. version change to a tag that does not exist: nothing changes"
before="$(container_ids)"
post /apply/versions mdeoTag=nope simTag=t1 >/dev/null
r="$(wait_done)"; echo "     $r"
check "fails" grep -q '^False' <<<"$r"
check "says nothing was changed" grep -q 'Nothing was changed' <<<"$r"
check "no container was touched" [ "$before" = "$(container_ids)" ]
check "desired version unchanged" [ "$(status | jq_ 'd["desired"]["mdeoTag"]')" = t1 ]

memcheck; echo "== 6. version change to a tag that never becomes healthy: automatic rollback"
post /apply/versions mdeoTag=sick simTag=t1 >/dev/null
r="$(wait_done 600)"; echo "     $r"
check "fails" grep -q '^False' <<<"$r"
check "was rolled back and is healthy again" grep -q 'healthy again' <<<"$r"
check "backend runs t1 again" [ "$(image_of backend)" = "$REG/mdeo-backend:t1" ]
check "stack healthy" [ "$(status | jq_ 'd["healthy"]')" = True ]

memcheck; echo "== 7. version change to t2"
post /apply/versions mdeoTag=t2 simTag=t2 >/dev/null
r="$(wait_done)"; echo "     $r"
check "succeeds" grep -q '^True' <<<"$r"
check "backend runs t2" [ "$(image_of backend)" = "$REG/mdeo-backend:t2" ]
check "simulator runs t2" [ "$(image_of palladio-sim-1)" = "$REG/palladio-sim-service:t2" ]
check "last known good is t2" [ "$(status | jq_ 'd["lastKnownGood"]["mdeoTag"]')" = t2 ]

memcheck; echo "== 8. restart"
before="$(container_ids)"
post /action/restart >/dev/null
r="$(wait_done)"; echo "     $r"
check "succeeds" grep -q '^True' <<<"$r"
check "every container is new" bash -c "[ -z \"\$(comm -12 <(tr ' ' '\n' <<<'$before' | sort) <(tr ' ' '\n' <<<'$(container_ids)' | sort) | grep .)\" ]"

memcheck; echo "== 9. down and up: volumes and data survive"
post /action/down >/dev/null
r="$(wait_done)"; echo "     $r"
check "down succeeds" grep -q '^True' <<<"$r"
check "no containers" [ -z "$(container_ids | tr -d ' ')" ]
check "4 database volumes kept" [ "$(docker volume ls -q --filter "label=com.docker.compose.project=$PROJECT" | wc -l)" = 4 ]
check "manager still runs" [ "$(docker inspect -f '{{.State.Running}}' $MPROJECT-manager-1)" = true ]
manager restart >/dev/null
for _ in $(seq 30); do curl -s -o /dev/null "http://127.0.0.1:$MANAGER_PORT/" && break; sleep 1; done
r="$(wait_done)"
check "a restarted manager keeps the stack down" [ -z "$(container_ids | tr -d ' ')" ]
post /action/up >/dev/null
r="$(wait_done)"; echo "     $r"
check "up succeeds" grep -q '^True' <<<"$r"
check "database content survived" bash -c "docker exec $PROJECT-postgres-backend-1 psql -U mdeo -d mdeo -tAc 'select v from it_marker' | grep -qx kept"

memcheck; echo "== 10. manager killed during a change, restarted: finishes it"
post /apply/versions mdeoTag=t1 simTag=t1 >/dev/null
for _ in $(seq 120); do current_has 'bringing the stack' && break; sleep 0.5; done
docker kill "$MPROJECT-manager-1" >/dev/null
sleep 2
journal="$(docker run --rm -v "$WORK/state:/s:ro" busybox:1.37 cat /s/state.json | jq_ 'd.get("journal", {}).get("phase")')"
check "journal left in phase apply" [ "$journal" = apply ]
docker start "$MPROJECT-manager-1" >/dev/null 2>&1 || true
for _ in $(seq 30); do curl -s -o /dev/null "http://127.0.0.1:$MANAGER_PORT/" && break; sleep 1; done
r="$(wait_done)"; echo "     $r"
check "recovered the interrupted change" grep -q 'Recovered the interrupted' <<<"$r"
check "backend runs t1" [ "$(image_of backend)" = "$REG/mdeo-backend:t1" ]
check "stack healthy" [ "$(status | jq_ 'd["healthy"]')" = True ]

memcheck; echo "== 11. manager killed during a version change that fails: rolls back on start"
post /apply/versions mdeoTag=sick simTag=t1 >/dev/null
for _ in $(seq 120); do current_has 'bringing the stack' && break; sleep 0.5; done
docker kill "$MPROJECT-manager-1" >/dev/null
docker start "$MPROJECT-manager-1" >/dev/null 2>&1 || true
for _ in $(seq 30); do curl -s -o /dev/null "http://127.0.0.1:$MANAGER_PORT/" && break; sleep 1; done
r="$(wait_done 600)"; echo "     $r"
check "rolled back" grep -q 'rolled back' <<<"$r"
check "backend runs t1" [ "$(image_of backend)" = "$REG/mdeo-backend:t1" ]

memcheck; echo "== 12. the generated compose file is valid as written"
check "docker compose config" docker exec "$MPROJECT-manager-1" docker compose -p "$PROJECT" -f /state/stack/compose.yaml --env-file /state/secrets.env --env-file /config/stack.env config -q

memcheck; echo "== 13. auto-heal: an unhealthy container is restarted, a removed one recreated"
healcount() { status | jq_ 'd["healCount"]'; }
before_start="$(docker inspect -f '{{.State.StartedAt}}' $PROJECT-service-csv-1)"
n0="$(healcount)"
# The stand-in keeps running but stops answering: its health check fails from now on.
docker exec "$PROJECT-service-csv-1" sh -c 'pkill httpd || killall httpd'
healed=""
for _ in $(seq 90); do
    if status | jq_ '[h for h in d.get("heals") or [] if h["service"]=="service-csv" and h["action"]=="restart" and h["ok"]]' | grep -q service-csv; then healed=1; break; fi
    sleep 2
done
check "unhealthy service-csv restarted by the heal loop" [ -n "$healed" ]
check "same container, started again" [ "$(docker inspect -f '{{.State.StartedAt}}' $PROJECT-service-csv-1)" != "$before_start" ]
for _ in $(seq 45); do [ "$(status | jq_ 'd["healthy"]')" = True ] && break; sleep 2; done
check "stack healthy again" [ "$(status | jq_ 'd["healthy"]')" = True ]
docker rm -f "$PROJECT-service-model-1" >/dev/null
for _ in $(seq 45); do docker inspect "$PROJECT-service-model-1" >/dev/null 2>&1 && break; sleep 2; done
check "removed service-model recreated by the heal loop" docker inspect -f '{{.Id}}' "$PROJECT-service-model-1"
for _ in $(seq 45); do [ "$(status | jq_ 'd["healthy"]')" = True ] && break; sleep 2; done
check "stack healthy again" [ "$(status | jq_ 'd["healthy"]')" = True ]
# The compose up of a heal returns (and is recorded) once compose has seen the service healthy.
for _ in $(seq 30); do [ "$(healcount)" -ge $((n0 + 2)) ] && break; sleep 2; done
check "heals recorded ($n0 -> $(healcount))" [ "$(healcount)" -ge $((n0 + 2)) ]
check "a compose up heal is in the list" [ "$(status | jq_ 'len([h for h in d.get("heals") or [] if h["action"]=="up" and h["ok"]])')" -ge 1 ]
check "volumes untouched by healing" [ "$(docker volume ls -q --filter "label=com.docker.compose.project=$PROJECT" | wc -l)" = 4 ]
check "no heal ever touched the manager" [ "$(status | jq_ 'len([h for h in d.get("heals") or [] if "manager" in h.get("container", "")])')" = 0 ]

echo
echo "passed $pass, failed $fail"
[ "$fail" = 0 ]
