---
phase: 19-multi-turn-runtime-context
plan: 03
subsystem: infrastructure-model-openai
tags: [java, openai-compatible, spring-ai, provider-boundary, multi-turn-context, tdd]

requires:
  - phase: 19-multi-turn-runtime-context
    plan: 02
    provides: RunContext.sessionContext.messages populated with bounded prior conversation history
provides:
  - Messages-based OpenAI-compatible streaming source boundary
  - Ordered provider-neutral chat message conversion from SessionContext history plus current RunInput
  - Spring AI Prompt role mapping for user and assistant messages inside the OpenAI infrastructure module
  - Focused no-key provider-boundary tests for prior history order and current prompt exactly once
affects: [phase-19-fake-model-proof, phase-20-provider-stability, phase-21-architecture-regression]

tech-stack:
  added: []
  patterns: [infrastructure-local-provider-message-type, ordered-chat-message-boundary, spring-ai-role-mapping-isolation]

key-files:
  created:
    - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiChatMessage.java
  modified:
    - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClient.java
    - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiStreamSource.java
    - pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiSpringAiModelFactory.java
    - pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClientTest.java

key-decisions:
  - "Use an infrastructure-local OpenAiChatMessage record so provider chat semantics do not leak into Domain/App contracts."
  - "Build OpenAI provider messages from SessionContext.messages first, then append current RunInput as the final user message exactly once."
  - "Ignore unsupported or blank historical roles at the provider boundary and map only user/assistant roles to Spring AI messages."
  - "Keep Spring AI UserMessage/AssistantMessage imports isolated to pi-agent-infrastructure-model-openai."

patterns-established:
  - "OpenAiStreamSource now accepts List<OpenAiChatMessage>, preventing accidental string-prompt flattening at the OpenAI-compatible boundary."
  - "OpenAiSpringAiModelFactory.toSpringAiMessages(...) is a package-private infrastructure-only mapping seam with focused role/order tests."

requirements-completed: [CTX-01, CTX-04]

duration: 5m56s
completed: 2026-07-01
---

# Phase 19 Plan 03: OpenAI-Compatible Ordered Message Boundary Summary

**OpenAI-compatible provider calls now receive ordered user/assistant chat messages from prior session context plus the current prompt, preserving roles through Spring AI Prompt construction.**

## Performance

- **Duration:** 5m56s
- **Started:** 2026-07-01T14:23:04Z
- **Completed:** 2026-07-01T14:29:00Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- Added `OpenAiChatMessage`, a package-private immutable provider message type local to `pi-agent-infrastructure-model-openai`.
- Changed `OpenAiStreamSource` from `stream(String prompt, ...)` to `stream(List<OpenAiChatMessage>, ...)`, removing the old provider boundary that flattened all context into one prompt string.
- Updated `OpenAiCompatibleStreamingModelClient` to build ordered messages from `request.context().sessionContext().messages()` followed by the current `RunInput.ChatInput` or `RunInput.TaskInput` as the final `user` message.
- Added tests proving prior `user`/`assistant` messages are sent first, unsupported roles are dropped, and the current prompt appears exactly once in provider messages.
- Updated Spring AI integration to construct `new Prompt(List<Message>)` and map infrastructure-local `user` messages to `UserMessage` and `assistant` messages to `AssistantMessage` while preserving order.

## Task Commits

Each task was committed atomically:

1. **Task 1: Convert ModelRequest into ordered provider-neutral chat messages** - `6bc4dcf` (feat)
2. **Task 2: Map ordered messages to Spring AI Prompt roles inside infrastructure** - `5a2c34d` (feat)

**Plan metadata:** pending final docs commit

_Note: Both tasks were executed with TDD-style RED/GREEN loops. The Task 1 RED run failed on missing `OpenAiChatMessage` and the old string-based stream signature; the Task 2 GREEN gate passed after adding focused Spring AI role/order mapping proof._

## Files Created/Modified

- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiChatMessage.java` - Infrastructure-local role/content message record restricted to `user` and `assistant` roles.
- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiStreamSource.java` - Messages-based OpenAI stream source interface.
- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClient.java` - Converts `ModelRequest` history plus current input into ordered `OpenAiChatMessage` values and sends them to the stream source.
- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiSpringAiModelFactory.java` - Maps provider-neutral messages to Spring AI `UserMessage`/`AssistantMessage` list prompt construction.
- `pi-agent-infrastructure-model-openai/src/test/java/io/github/pi_java/agent/infrastructure/model/openai/OpenAiCompatibleStreamingModelClientTest.java` - Captures provider message lists and verifies role/order/current-prompt-once semantics without external keys.

## Decisions Made

- Used a package-private `OpenAiChatMessage` record instead of changing Domain/App model contracts; this keeps provider-boundary details in Infrastructure.
- Kept current prompt extraction as a private helper used only to append the current `RunInput` as a final `user` message; provider calls no longer use a concatenated prompt path.
- Normalized supported historical role casing by mapping `user`/`assistant` only and dropping unsupported/blank roles rather than introducing system/developer prompts or redaction placeholders.
- Isolated all Spring AI message construction to `OpenAiSpringAiModelFactory`, preserving COLA/provider boundaries.

## Deviations from Plan

None - plan executed as written.

## Issues Encountered

- Existing unrelated working-tree changes were present before and after execution: `.gitignore` modified and `.planning/phases/17-console-session-restore-ux/17-VERIFICATION.md` untracked. They were not staged or committed by this plan.
- Focused OpenAI tests emit existing SLF4J no-provider warnings in this module test environment; the warning is unrelated to the provider-boundary change and tests pass.

## Known Stubs

None. Stub scan found only defensive null/blank checks in provider code and existing unrelated fallback code; no plan-blocking placeholders, TODOs, or mock UI data were introduced.

## User Setup Required

None - no external credentials or services required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure-model-openai -Dtest=OpenAiCompatibleStreamingModelClientTest test` — passed with 7 tests.
- Boundary scan: no `stream(String prompt, ...)` implementation remains in the OpenAI module.
- Boundary scan: no Spring AI message or `OpenAiChatMessage` references were found in `pi-agent-domain` or `pi-agent-app`.

## Next Phase Readiness

- Plan 04 can use this provider-boundary seam to add fake-model capture proof across the full runtime path.
- Phase 21 can assert that Spring AI/provider message classes remain isolated to Infrastructure and that App/Domain contracts stay provider-neutral.

## Self-Check: PASSED

- Created/modified files verified present: `OpenAiChatMessage.java`, `OpenAiStreamSource.java`, `OpenAiCompatibleStreamingModelClient.java`, `OpenAiSpringAiModelFactory.java`, `OpenAiCompatibleStreamingModelClientTest.java`, and this summary.
- Task commits verified present in git history: `6bc4dcf`, `5a2c34d`.

---
*Phase: 19-multi-turn-runtime-context*
*Completed: 2026-07-01*
