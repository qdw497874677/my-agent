#!/usr/bin/env bash
set -euo pipefail

PORT="${1:-18080}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CP_FILE="${TMPDIR:-/tmp}/pi-java-e2e-classpath.txt"
E2E_JAVA_HOME="${PI_E2E_JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
READY_FILE="${TMPDIR:-/tmp}/pi-java-playwright-ready-${PORT}"

cd "${ROOT_DIR}"
rm -f "${READY_FILE}"

JAVA_HOME="${E2E_JAVA_HOME}" mvn -q -DskipTests install
JAVA_HOME="${E2E_JAVA_HOME}" mvn -q -pl pi-agent-adapter-web dependency:build-classpath \
  -Dmdep.outputFile="${CP_FILE}" \
  -Dmdep.includeScope=test

"${E2E_JAVA_HOME}/bin/java" \
  -cp "pi-agent-adapter-web/target/test-classes:pi-agent-adapter-web/target/classes:$(cat "${CP_FILE}")" \
  -Dserver.port="${PORT}" \
  -Dspring.profiles.active="test,e2e" \
  -Dspring.main.allow-bean-definition-overriding=true \
  -Dspring.flyway.enabled=false \
  -Dvaadin.productionMode=true \
  -Dspring.autoconfigure.exclude="org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration" \
  -Dpi.runtime.worker.poll-delay-ms=100 \
  -Dpi.e2e.ready-file="${READY_FILE}" \
  io.github.pi_java.agent.adapter.web.PiCloudServerApplication &

SERVER_PID="$!"
trap 'kill "${SERVER_PID}" 2>/dev/null || true; wait "${SERVER_PID}" 2>/dev/null || true' EXIT INT TERM

for _ in $(seq 1 240); do
  if curl -fsS "http://127.0.0.1:${PORT}/actuator/health" >/dev/null 2>&1; then
    sleep 12
    touch "${READY_FILE}"
    break
  fi
  if ! kill -0 "${SERVER_PID}" 2>/dev/null; then
    wait "${SERVER_PID}"
  fi
  sleep 0.5
done

wait "${SERVER_PID}"
