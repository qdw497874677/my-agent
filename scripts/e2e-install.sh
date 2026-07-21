#!/usr/bin/env bash
set -euo pipefail

ARGS=()
for arg in "$@"; do
  case "${arg}" in
    --with-deps=false) ;;
    *) ARGS+=("${arg}") ;;
  esac
done

node node_modules/@playwright/test/cli.js install chromium firefox webkit ${ARGS+"${ARGS[@]}"}
