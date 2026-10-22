# A stand-in for every mdeo service in the manager's integration test: answers HTTP on the ports
# the real services use (80, 3000, 3022, 8080), so their health checks pass. Built with
# --build-arg SICK=1 it starts but never answers (never becomes healthy).
FROM busybox:1.37
ARG SICK=""
COPY fake-service.sh /fake-service.sh
COPY curl.sh /usr/bin/curl
RUN chmod +x /fake-service.sh /usr/bin/curl \
    && mkdir -p /www && echo ok > /www/index.html && echo '{"status":"running"}' > /www/health \
    && if [ -n "$SICK" ]; then touch /sick; fi
ENTRYPOINT ["/fake-service.sh"]
