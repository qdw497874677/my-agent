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

## VER-05 Incremental Slow Stream Gate

Run the controlled slow-stream incremental gate with Java 21:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=WebConsoleSlowStreamIncrementalTest test
```

`WebConsoleSlowStreamIncrementalTest` proves that fake slow-stream assistant text reaches the `ChatEventStreamPanel` primary assistant bubble before terminal completion. The test drives a deterministic reducer/panel sequence with a `model.delta` partial value, asserts that the bubble contains the partial assistant text while `data-stream-state` is still `streaming` and no terminal event has been observed, then emits `run.completed` and asserts the same bubble becomes `completed`.

This gate must fail if assistant text is buffered and only replayed after completion, because the before-terminal checkpoint requires visible partial text before the terminal event is drained.

Release owners should keep this gate paired with VER-04 browser coverage so component-level incremental semantics and live Console product-path streaming stay aligned.

## VER-04 Browser Product Path Gate

Run the local syntax/list gate for the consolidated Phase 21 Console browser product path spec:

```bash
npx playwright test e2e/phase-21-console-product-path-regression.spec.ts --list
```

This `--list` command is the local/no-key gate. It validates that the Playwright spec is registered across the configured browser projects and can be run on developer machines without starting a Vaadin server or configuring provider credentials.

Run the live browser gate against a running server before release sign-off:

```bash
PI_E2E_PORT=18080 npx playwright test e2e/phase-21-console-product-path-regression.spec.ts --project=chromium
```

The live command requires a running server on `PI_E2E_PORT` and exercises the Kimi-style Console product path through fake-runtime seeded conversations and streams: recent session card restore, the continued active session banner, same-session follow-up send, one assistant bubble for slow streams, cancellation preserving partial output, failed provider/error state rendering, and negative assertions that main chat content does not expose raw runtime-event strings.

VER-04 is not fully satisfied by --list alone. Release/CI owners should treat `--list` as the always-runnable syntax gate and the live `--project=chromium` command as the product-path browser gate when the Vaadin server is available.
