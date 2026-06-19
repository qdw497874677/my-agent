## 09-02 Deferred Items

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability -am test` currently fails in upstream `pi-agent-app` tests because some existing tool-gateway test fixtures still construct non-W3C trace IDs after the Phase 09 trace-id normalization work. This is outside Plan 09-02's observability module scope. Plan-local verification passed with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability test`.
