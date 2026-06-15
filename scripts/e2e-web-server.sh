#!/usr/bin/env bash
set -euo pipefail

PORT="${1:-18080}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CP_FILE="${TMPDIR:-/tmp}/pi-java-e2e-classpath.txt"
E2E_JAVA_HOME="${PI_E2E_JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"

cd "${ROOT_DIR}"

JAVA_HOME="${E2E_JAVA_HOME}" mvn -q -DskipTests install
JAVA_HOME="${E2E_JAVA_HOME}" mvn -q -pl pi-agent-adapter-web dependency:build-classpath \
  -Dmdep.outputFile="${CP_FILE}" \
  -Dmdep.includeScope=test

exec "${E2E_JAVA_HOME}/bin/java" \
  -cp "pi-agent-adapter-web/target/test-classes:pi-agent-adapter-web/target/classes:$(cat "${CP_FILE}")" \
  -Dserver.port="${PORT}" \
  -Dspring.profiles.active="test,e2e" \
  -Dspring.main.allow-bean-definition-overriding=true \
  -Dspring.flyway.enabled=false \
  -Dspring.autoconfigure.exclude="org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration" \
  -Dpi.runtime.worker.poll-delay-ms=100 \
  io.github.pi_java.agent.adapter.web.PiCloudServerApplication
