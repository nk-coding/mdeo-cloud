#!/bin/sh
# The health checks' `curl -fsS -o /dev/null URL`, with busybox wget.
for a in "$@"; do url="$a"; done
exec wget -q -O /dev/null "$url"
