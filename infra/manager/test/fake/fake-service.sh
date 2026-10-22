#!/bin/sh
# Serves /www on every port a real service would use, until stopped.
trap 'exit 0' TERM INT
if [ -e /sick ]; then
    while :; do sleep 1; done
fi
for p in 80 3000 3022 8080; do httpd -f -p "$p" -h /www & done
while :; do sleep 1; done
