# Phase 21 Verification, Security, and Regression Hardening

Phase 21 collects the final v1.2 release hardening gates for Console conversation semantics, security boundaries, and regression coverage. The checks are intentionally no-key where possible and preserve the Java/Vaadin/COLA architecture decisions from earlier phases.

## VER-03 Architecture Gates

Run the complete architecture boundary gate with Java 21:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-domain,pi-agent-app,pi-agent-client -am -Dtest='*ArchTest,*ArchitectureTest' test
```

The VER-03 ArchUnit coverage protects Domain, App, and Client contracts from leaking these forbidden families:

- Vaadin UI framework types.
- Spring AI framework/provider abstraction types.
- SQLite/JDBC persistence types, including `org.sqlite..` and `java.sql..`.
- Provider SDK and OpenAI infrastructure types, including `io.github.pi_java.agent.infrastructure.model.openai..`.
- Adapter packages such as `io.github.pi_java.agent.adapter..`.
- Infrastructure implementation packages such as `io.github.pi_java.agent.infrastructure..`.

Provider-specific OpenAI and Spring AI details remain infrastructure-only. Client DTO/API contracts, App orchestration contracts, and Domain/runtime contracts must stay provider-neutral and framework-neutral.
