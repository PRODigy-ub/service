#!/bin/bash
set -x

mkdir -p infrastructure/prometheus/durable infrastructure/loki/durable
chmod a+w infrastructure/prometheus/durable infrastructure/loki/durable

docker plugin inspect loki >/dev/null 2>&1 || \
  docker plugin install grafana/loki-docker-driver:latest --alias loki --grant-all-permissions
