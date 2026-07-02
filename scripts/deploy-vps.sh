#!/usr/bin/env bash
set -euo pipefail

if [ -z "${APP_IMAGE:-}" ]; then
  echo "APP_IMAGE is missing."
  exit 1
fi

if [ ! -f .env ]; then
  echo "Missing .env on VPS. Create it from CICD_ONLY_SETUP.md before deploying."
  exit 1
fi

if [ -n "${GHCR_USERNAME:-}" ] && [ -n "${GHCR_TOKEN:-}" ]; then
  echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin
else
  echo "GHCR_USERNAME/GHCR_TOKEN not provided. Pull will work only if the image is public or the VPS is already logged in."
fi

export APP_IMAGE

docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d --remove-orphans
docker image prune -f

echo "Deploy completed. Running containers:"
docker compose -f docker-compose.prod.yml ps
