#!/usr/bin/env bash
set -euo pipefail

PORT="${1:-18080}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CP_FILE="${TMPDIR:-/tmp}/pi-java-e2e-classpath.txt"
E2E_CLASSES_DIR="${TMPDIR:-/tmp}/pi-java-e2e-classes-${PORT}"
MAIN_CLASSES_DIR="${TMPDIR:-/tmp}/pi-java-main-classes-${PORT}"
E2E_JAVA_HOME="${PI_E2E_JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
READY_FILE="${TMPDIR:-/tmp}/pi-java-playwright-ready-${PORT}"

cd "${ROOT_DIR}"
rm -f "${READY_FILE}"
rm -rf "${E2E_CLASSES_DIR}"
rm -rf "${MAIN_CLASSES_DIR}"
mkdir -p "${E2E_CLASSES_DIR}/io/github/pi_java/agent/adapter/web"

JAVA_HOME="${E2E_JAVA_HOME}" mvn -q -DskipTests install
JAVA_HOME="${E2E_JAVA_HOME}" mvn -q -pl pi-agent-adapter-web org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath \
  -Dmdep.outputFile="${CP_FILE}" \
  -Dmdep.includeScope=test

cp "${ROOT_DIR}"/pi-agent-adapter-web/target/test-classes/io/github/pi_java/agent/adapter/web/WebConsoleE2EFixtureConfiguration*.class \
  "${E2E_CLASSES_DIR}/io/github/pi_java/agent/adapter/web/"
cp "${ROOT_DIR}"/pi-agent-adapter-web/target/test-classes/io/github/pi_java/agent/adapter/web/InMemoryCloudE2EConfiguration*.class \
  "${E2E_CLASSES_DIR}/io/github/pi_java/agent/adapter/web/"
cp -R "${ROOT_DIR}/pi-agent-adapter-web/target/classes/." "${MAIN_CLASSES_DIR}/"

cd "${ROOT_DIR}/pi-agent-adapter-web"

"${E2E_JAVA_HOME}/bin/java" \
  -cp "${E2E_CLASSES_DIR}:${MAIN_CLASSES_DIR}:$(cat "${CP_FILE}")" \
  -Dserver.port="${PORT}" \
  -Dproject.basedir="${ROOT_DIR}/pi-agent-adapter-web" \
  -Dspring.profiles.active="test,e2e" \
  -Dspring.main.allow-bean-definition-overriding=true \
  -Dspring.flyway.enabled=false \
  -Dvaadin.productionMode=false \
  -Dspring.autoconfigure.exclude="org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration" \
  -Dpi.local.db-path="target/playwright-e2e-local.db" \
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
