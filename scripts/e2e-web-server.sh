#!/usr/bin/env bash
set -euo pipefail

PORT="${1:-18080}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CP_FILE="${TMPDIR:-/tmp}/pi-java-e2e-classpath.txt"

cd "${ROOT_DIR}"

JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}" mvn -q -DskipTests install
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}" mvn -q -pl pi-agent-adapter-web dependency:build-classpath \
  -Dmdep.outputFile="${CP_FILE}" \
  -Dmdep.includeScope=test

exec "${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}/bin/java" \
  -cp "pi-agent-adapter-web/target/test-classes:pi-agent-adapter-web/target/classes:$(cat "${CP_FILE}")" \
  -Dserver.port="${PORT}" \
  -Dspring.profiles.active="test,e2e" \
  -Dspring.main.allow-bean-definition-overriding=true \
  -Dspring.flyway.enabled=false \
  -Dspring.autoconfigure.exclude="org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration" \
  -Dpi.runtime.worker.poll-delay-ms=100 \
  io.github.pi_java.agent.adapter.web.PiCloudServerApplication
