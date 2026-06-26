# Local Startup Profiles

## Local in-memory mode

Use this mode when you want to start the Web Console/Admin locally without a PostgreSQL instance or external model provider.

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
mvn -pl pi-agent-adapter-web -DskipTests compile

CP="pi-agent-adapter-web/target/classes:pi-agent-client/target/classes:pi-agent-domain/target/classes:pi-agent-app/target/classes:pi-agent-extension-api/target/classes:pi-agent-infrastructure/target/classes:pi-agent-infrastructure-extension/target/classes:pi-agent-infrastructure-plugin/target/classes:pi-agent-infrastructure-observability/target/classes:pi-agent-infrastructure-mcp/target/classes:pi-agent-spring-boot-starter/target/classes:pi-agent-infrastructure-model-openai/target/classes:$(tr -d '\n' < /tmp/pi-java-4098-classpath.txt)"

JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
/usr/lib/jvm/java-21-openjdk-amd64/bin/java \
  -cp "$CP" \
  io.github.pi_java.agent.adapter.web.PiCloudServerApplication \
  --server.port=4098 \
  --spring.profiles.active=local
```

The `local` profile provides in-memory runtime repositories, an in-memory run queue, a no-op transaction manager, dev auth, and a synchronous fake `AgentRuntime`.

It also binds the embedded server to `0.0.0.0` so the app can be reached through a remote development host, container port mapping, or SSH port forwarding. Keep this profile for local/dev use only.

## Production/default mode

Omit `--spring.profiles.active=local` to use the default PostgreSQL-backed runtime configuration. The default mode still requires a real `JdbcTemplate`/DataSource and model runtime configuration.

## Health check

```bash
curl -I http://localhost:4098/actuator/health
```
