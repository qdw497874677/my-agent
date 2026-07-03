---
phase: 19-multi-turn-runtime-context
verified: 2026-07-03T02:46:00Z
status: passed
score: 5/5 must-haves verified
re_verification:
  previous_status: gaps_found
  previous_score: 4/5
  gaps_closed:
    - "OpenAI-compatible provider boundary is coherently migrated to ordered message-list streaming across reactor-dependent code."
  gaps_remaining: []
  regressions: []
---

# Phase 19: Multi-Turn Runtime Context Verification Report

**Phase Goal:** Continuing a selected session means the model actually receives bounded prior turns, not only the same `sessionId`.
**Verified:** 2026-07-03T02:46:00Z
**Status:** passed
**Re-verification:** Yes — after Plan 05 gap closure

## Goal Achievement

Phase 19 is now verified as achieved. The previous blocker was specifically re-checked and closed: `OpenAiChatMessage` is public, the adapter-web fake OpenAI stream source implements `stream(List<OpenAiChatMessage>, CancellationToken)`, no stale `stream(String, ...)` OpenAI boundary remains in the checked paths, and the authoritative reactor-aware adapter-web gate passes.

The core runtime path is also still intact: selected-session transcript turns are assembled by App-layer policy, filtered/budgeted, injected into `RunContext.sessionContext().messages()` before runtime start, and converted to ordered provider chat messages before OpenAI-compatible streaming.

## Goal-Backward Must-Haves

From ROADMAP Phase 19 success criteria plus PLAN frontmatter must-haves:

1. Runtime/model execution receives bounded prior `user`/`assistant` turns for the selected session.
2. Context policy limits recent turns/characters and records truncation metadata.
3. Sensitive tool/audit/provider/credential data is excluded or redacted before context is sent to a model.
4. Fake-model tests prove prior turns are present and current prompt appears exactly once.
5. Context assembly lives in App/runtime seams and provider message migration is complete across reactor-dependent code.

## Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Runtime/model execution receives bounded prior user/assistant turns for the selected session. | ✓ VERIFIED | `ConversationContextAssembler.assemble(...)` calls `ConversationQueryService.getTranscript(context, sessionId, policy.transcriptLimit(), null)` and emits `SessionEntryPayload.MessageEntry`; `DefaultRunDispatcher.dispatchClaimed(...)` calls the assembler before `AgentRuntime.start` and builds `RunContext` with `sessionContext(queuedRun, contextResult.messages())`. `DefaultRunDispatcherContextTest` passed. |
| 2 | Context policy limits recent turns/chars/token approximation and records truncation metadata. | ✓ VERIFIED | `ConversationContextPolicy` validates positive `maxRecentMessages`, `maxTotalCharacters`, and `transcriptLimit`; assembler drops older messages first by recent-message and character budgets while preserving included order; `ConversationContextMetadata` records included/dropped/excluded/max/result/truncated. `ConversationContextAssemblerTest` passed. |
| 3 | Sensitive tool/audit/provider/credential data is excluded or redacted before any context is sent to a model. | ✓ VERIFIED | Assembler accepts only visible, non-redacted, non-sensitive completed/partial `USER`/`ASSISTANT` messages; excludes tool/error roles, failed/cancelled statuses, current run id, blank text, diagnostic metadata, credential-like metadata, and sensitive text markers. Safety tests passed. |
| 4 | Fake-model tests prove prior turns are present and the current prompt appears exactly once. | ✓ VERIFIED | `FakeModelClient` and `FakeStreamingModelClient` capture `ModelRequest`s via `lastRequest()`/`requests()`; `FakeModelContextCaptureTest` asserts prior message order and exactly one current prompt. Tests passed. |
| 5 | Context assembly lives in App/runtime seams and provider message migration is complete across codebase. | ✓ VERIFIED | App/Domain architecture gates pass, Vaadin does not assemble model context, `OpenAiStreamSource` is message-list-only, `OpenAiChatMessage` is public, `FakeOpenAiProviderE2EConfiguration` implements `stream(List<OpenAiChatMessage>, ...)`, stale `stream(String` search returned no matches in the OpenAI module or adapter-web fake config, and `mvn -pl pi-agent-adapter-web -am -DskipTests test` passed. |

**Score:** 5/5 truths verified

## Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextAssembler.java` | App-layer transcript-to-`SessionContext` message assembly | ✓ VERIFIED | Exists, substantive, uses `ConversationQueryService`, filters unsafe entries, applies budgets, returns messages plus metadata. |
| `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextPolicy.java` | Configurable recent-turn/character defaults | ✓ VERIFIED | Exists with defaults `12`, `12000`, `48` and constructor validation. |
| `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationContextMetadata.java` | Included/dropped/excluded/truncated observability metadata | ✓ VERIFIED | Exists with validated count fields. |
| `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java` | Dispatch-time context injection seam | ✓ VERIFIED | Calls assembler before runtime start; injects messages into `RunContext`; records safe metadata fields. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java` | Local composition wiring | ✓ VERIFIED | Defines local `ConversationContextPolicy`, `ConversationContextAssembler`, and `ConversationQueryService` beans. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java` | Cloud composition wiring | ✓ VERIFIED | Defines cloud context beans and passes assembler/policy to `DefaultRunDispatcher`. |
| `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiChatMessage.java` | Public provider-specific message carrier usable by external `OpenAiStreamSource` implementations | ✓ VERIFIED | `public record OpenAiChatMessage(String role, String content)` with validation and public factories. |
| `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiStreamSource.java` | Messages-based OpenAI streaming source interface | ✓ VERIFIED | Public interface accepts `List<OpenAiChatMessage>` and has no string-prompt stream method. |
| `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClient.java` | `ModelRequest` to ordered provider chat messages conversion | ✓ VERIFIED | `messagesFrom` reads `request.context().sessionContext().messages()` first and appends current input as final `user` message. |
| `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiSpringAiModelFactory.java` | Spring AI prompt construction with roles | ✓ VERIFIED | Builds `new Prompt(toSpringAiMessages(messages))`; maps `user` to `UserMessage` and `assistant` to `AssistantMessage`. |
| `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/FakeOpenAiProviderE2EConfiguration.java` | Adapter-web fake provider implementation using message-list streaming | ✓ VERIFIED | Imports `OpenAiChatMessage` and overrides `stream(List<OpenAiChatMessage>, CancellationToken)`. |
| `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeStreamingModelClient.java` | Captured streaming `ModelRequest` access | ✓ VERIFIED | Captures requests before scripted actions and exposes `lastRequest()`/`requests()`. |
| `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeModelClient.java` | Captured non-streaming `ModelRequest` access | ✓ VERIFIED | Captures requests before returning scripted responses and exposes `lastRequest()`/`requests()`. |
| `pi-testkit/src/test/java/io/github/pi_java/agent/testkit/FakeModelContextCaptureTest.java` | No-key semantic proof | ✓ VERIFIED | Tests both streaming and non-streaming fake runtime paths. |
| `pi-agent-app/src/test/java/io/github/pi_java/agent/app/architecture/AppDependencyArchTest.java` | App boundary guard | ✓ VERIFIED | Explicitly covers `ConversationContextAssembler`, `ConversationContextPolicy`, and `ConversationContextMetadata`. |

## Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| `ConversationContextAssembler` | `ConversationQueryService.getTranscript` | typed transcript source | ✓ WIRED | `getTranscript(context, sessionId, policy.transcriptLimit(), null)` at assembler lines 50-54. |
| `ConversationContextAssembler` | `SessionEntryPayload.MessageEntry` | provider-neutral Domain message carrier | ✓ WIRED | `new SessionEntryPayload.MessageEntry(message.role().wireValue(), text)` at assembler line 75. |
| `DefaultRunDispatcher.dispatchClaimed` | `ConversationContextAssembler` | assemble before `RunContext` | ✓ WIRED | `conversationContextAssembler.assemble(requestContext, queuedRun.sessionId(), runId)` before `RunContext` creation. |
| `DefaultRunDispatcher` | `RunContext` | `SessionContext` with assembled messages | ✓ WIRED | `new RunContext(... sessionContext(queuedRun, contextResult.messages()), ...)`. |
| `CloudRuntimeBeanConfiguration` / `LocalDevRuntimeBeanConfiguration` | `DefaultRunDispatcher` / context assembler | Spring composition roots | ✓ WIRED | Both roots define context policy/assembler beans; cloud passes them to dispatcher. |
| `OpenAiCompatibleStreamingModelClient` | `RunContext.sessionContext.messages` | `messagesFrom(ModelRequest)` | ✓ WIRED | Iterates `request.context().sessionContext().messages()` and appends current input last. |
| `OpenAiSpringAiModelFactory` | Spring AI `Prompt` | infrastructure-only message mapping | ✓ WIRED | Uses `new Prompt(OpenAiSpringAiModelFactory.toSpringAiMessages(messages))`. |
| `FakeStreamingModelClient` / `FakeModelClient` | `ModelRequest.context.sessionContext.messages` | request capture accessors | ✓ WIRED | Capture entire `ModelRequest`, enabling test assertions over context messages. |
| `FakeOpenAiProviderE2EConfiguration.FakeOpenAiStreamSource` | `OpenAiStreamSource.stream(List<OpenAiChatMessage>, CancellationToken)` | test composition fake provider | ✓ WIRED | Manual verification found `public Iterable<OpenAiStreamEvent> stream(List<OpenAiChatMessage> messages, CancellationToken cancellationToken)`. |
| `OpenAiStreamSource` | `OpenAiChatMessage` | public method signature | ✓ WIRED | `OpenAiStreamSource` references `List<OpenAiChatMessage>` and `OpenAiChatMessage` is a public record. |

_Note:_ `gsd-tools verify key-links` for Plan 05 returned false "Source file not found" for dotted symbol names, but manual source verification confirmed both Plan 05 links are wired.

## Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|---|---|---|---|---|
| `ConversationContextAssembler` | `eligible` transcript messages | `ConversationQueryService.getTranscript(...)` | Yes — typed transcript response, then safety filters and budgets | ✓ FLOWING |
| `DefaultRunDispatcher` | `contextResult.messages()` | App assembler called with selected `sessionId` and current `runId` | Yes — injected into `RunContext.sessionContext().messages()` | ✓ FLOWING |
| `OpenAiCompatibleStreamingModelClient` | `messages` provider list | `ModelRequest.context().sessionContext().messages()` plus `RunInput` | Yes — sent to `modelFactory.create(config).stream(messagesFrom(request), ...)` | ✓ FLOWING |
| `OpenAiSpringAiModelFactory` | Spring AI prompt messages | `OpenAiStreamSource.stream(List<OpenAiChatMessage>, ...)` | Yes — maps user/assistant messages into `Prompt(List<Message>)` | ✓ FLOWING |
| `FakeOpenAiProviderE2EConfiguration` | fake provider stream input | `OpenAiStreamSource.stream(List<OpenAiChatMessage>, ...)` | Yes — adapter-web test composition compiles against the current public message-list API | ✓ FLOWING |
| `FakeModelClient` / `FakeStreamingModelClient` | captured `ModelRequest` list | Runtime `GeneralAgentLoop` invokes model client | Yes — request capture tests inspect actual context | ✓ FLOWING |

## Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|---|---|---|---|
| OpenAI provider boundary uses ordered messages | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure-model-openai -Dtest=OpenAiCompatibleStreamingModelClientTest test` | Build success; 7 tests run, 0 failures/errors | ✓ PASS |
| Dispatcher injects context into runtime | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure -Dtest=DefaultRunDispatcherContextTest test` | Build success; 2 tests run, 0 failures/errors | ✓ PASS |
| Final no-key context, safety, and architecture gate | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-app,pi-agent-domain,pi-testkit -Dtest=ConversationContextAssemblerTest,AppDependencyArchTest,DomainDependencyArchTest,FakeModelContextCaptureTest test` | Build success; Domain 1 test, App 9 tests, Testkit 2 tests; 0 failures/errors | ✓ PASS |
| Adapter-web compile with reactor dependencies | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -DskipTests test` | Build success; all 14 reactor modules succeeded and adapter-web test compilation completed; tests skipped by requested flag | ✓ PASS |
| Stale string-prompt OpenAI boundary absent | Search for `stream\s*\(\s*String` in `pi-agent-infrastructure-model-openai` and `FakeOpenAiProviderE2EConfiguration.java` | No matches | ✓ PASS |

The stale non-reactor adapter-web dependency issue mentioned in Plan 05 was not treated as a product gap. The correct authoritative command is the reactor-aware `-am` gate above, and it passed.

## Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|---|---|---|---|---|
| CTX-01 | Plans 01, 02, 03, 05 | Next message in a selected session includes bounded prior user and assistant turns in model context. | ✓ SATISFIED | Assembler emits prior user/assistant `MessageEntry`s; dispatcher injects them into `RunContext`; OpenAI and fake model tests prove model-bound visibility. |
| CTX-02 | Plans 01, 02 | Context assembly applies configurable budget and records truncation. | ✓ SATISFIED | `ConversationContextPolicy` and `ConversationContextMetadata`; assembler tests cover recent/character budget and truncation metadata; dispatcher records safe count metadata. |
| CTX-03 | Plans 01, 04 | Tool, audit, provider, credential, and sensitive data excluded/redacted before model context. | ✓ SATISFIED | Assembler filters roles/status/visibility/redaction/diagnostic and credential-like metadata/text; safety tests passed. |
| CTX-04 | Plans 03, 04, 05 | Tests prove current prompt appears exactly once and prior turns are available to fake model. | ✓ SATISFIED | `OpenAiCompatibleStreamingModelClientTest` and `FakeModelContextCaptureTest` passed. |
| CTX-05 | Plans 01, 02, 04, 05 | Model context assembly is implemented in App/runtime seams, not Vaadin component state. | ✓ SATISFIED | App assembler is plain App usecase code; dispatcher invokes it; cloud/local composition wires it; App/Domain ArchUnit tests passed; adapter-web only composes beans/fake provider, not context rules. |

**Orphaned Phase 19 requirements:** None found. `.planning/REQUIREMENTS.md` maps CTX-01 through CTX-05 to Phase 19, and all are claimed in PLAN frontmatter.

## Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---:|---|---|---|
| `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiProviderErrorMapper.java` | 86,109,122,133 | `return null` | ℹ️ Info | Existing nullable helper/default branches in provider error mapping, not Phase 19 context data-flow stubs. |
| `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiToolCallAccumulator.java` | 182 | `return null` | ℹ️ Info | Existing parser/helper branch, not Phase 19 context data-flow stub. |

No TODO/FIXME/placeholder/empty-handler stubs were found in the Phase 19 App usecase production files or Plan 05 modified fake-provider configuration. The previous stale `stream(String, ...)` blocker is gone.

## Human Verification Required

None required. This phase is runtime/model contract work and has deterministic automated verification. No visual/UI or external-provider manual check is needed for the Phase 19 goal.

## Gaps Summary

No gaps remain. The previous provider-boundary/reactor compile gap was closed by Plan 05 and verified with source checks plus the reactor-aware adapter-web Maven gate.

---

_Verified: 2026-07-03T02:46:00Z_
_Verifier: the agent (gsd-verifier)_
