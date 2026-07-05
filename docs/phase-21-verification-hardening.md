# Phase 21 Verification, Security, and Regression Hardening

Phase 21 collects the final v1.2 release hardening gates for Console conversation semantics, security boundaries, and regression coverage. The checks are intentionally no-key where possible and preserve the Java/Vaadin/COLA architecture decisions from earlier phases.

## VER-01 Regression Gate

Run the named Phase 21 conversation regression gate with Java 21:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=Phase21ConversationRegressionGateTest test
```

This gate must run without provider keys, Docker, or external network. It uses deterministic Vaadin component seams, fake provider/model configuration, an in-process loopback provider-error server, and reducer-level streaming assertions instead of real model credentials or remote provider calls.

| Behavior | Test method |
|----------|-------------|
| `noKeyFallback` | `noKeyFallbackBlocksSendBeforeSessionRunOrUserBubble` |
| `configuredProviderPath` | `configuredProviderPathUsesSelectedProviderModelSnapshotWithoutNetworkCredentials` |
| `recentSessionRestore` | `recentSessionRestoreHydratesPriorUserAndAssistantTurns` |
| `sameSessionContinuation` | `sameSessionContinuationKeepsRestoredSessionIdForFollowUpRun` |
| `streamingCoalescing` | `streamingCoalescingKeepsMultipleDeltasInOneAssistantBubbleForSameRun` |
| `cancellationAndErrorStates` | `cancellationAndErrorStatesStopLateDeltasAndRenderSafeTerminalBubbles` |
| `providerErrors` | `providerErrorsAreActionableAndDoNotExposeSecretLookingValues` |

The configured-provider path seeds a fake ready provider/model selection and asserts the run request carries safe snapshot metadata such as `selectedModelRef`, `resolvedProviderId`, `resolvedModelId`, `fallbackMode`, and `readinessState`. It must not require real provider credentials.

Release owners should treat this command as the focused VER-01 go/no-go gate before running broader ownership, architecture, browser, and slow-stream Phase 21 checks.

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

## VER-02 Ownership Gates

Run the always-runnable local SQLite ownership gate with Java 21:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=ConversationOwnershipLeakageMatrixTest,RunProviderModelMetadataPersistenceTest test
```

This local gate is the Docker-free substitute for developer machines and no-key CI lanes. `ConversationOwnershipLeakageMatrixTest` seeds an allowed tenant/user/session/run plus negative `foreignTenant`, `foreignUser`, `foreignSession`, and `foreignRun` rows through the local profile repositories, then verifies `DefaultConversationQueryService.listRecentSessions(...)` and `getTranscript(...)` only expose allowed rows. `RunProviderModelMetadataPersistenceTest` remains paired with it to keep provider/model metadata persistence covered under the same SQLite local profile gate.

Run the production JDBC ownership proof in Docker/Testcontainers-enabled CI only:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure -am -Dtest=JdbcConversationReadModelIntegrationTest test
```

`JdbcConversationReadModelIntegrationTest` requires Docker/Testcontainers for PostgreSQL. When Docker is unavailable, treat this command as a CI-ENV SKIP rather than a product failure, and rely on the local SQLite gate above for always-runnable VER-02 coverage.
