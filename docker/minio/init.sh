#!/bin/sh
set -eu

until mc alias set blogminio http://minio:9000 minioadmin minioadmin; do
  sleep 2
done

mc mb --ignore-existing blogminio/personal-blog
mc anonymous set download blogminio/personal-blog
