---
phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
plan: 05
subsystem: testkit-tool-gateway-routing
tags: [java, testkit, tool-gateway, runtime, e2e, policy, cola]

requires:
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: App ToolExecutionGateway and collaborator ports from Plan 04-03.
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: Default gateway infrastructure collaborators and policy/redaction semantics from Plan 04-04.
provides:
  - Gateway-aware pi-testkit fake ToolRegistry, ToolExecutorBinding, and ToolExecutionGateway wrappers around legacy FakeToolInvoker.
  - GeneralAgentLoop model tool-call path routed through ToolExecutionGateway instead of direct ToolInvoker invocation.
  - Fake General Agent tests proving success, deny, approval-required, and source-level no-bypass behavior.
affects: [phase-04-cloud-tool-e2e, phase-05-runtime-cockpit, phase-06-extension-surface, phase-07-mcp-tools, phase-08-plugin-tools]

tech-stack:
  added: []
  patterns: [gateway-routed-testkit, legacy-invoker-behind-binding, lifecycle-event-sequence-normalization, no-key-tool-policy-e2e-foundation]

key-files:
  created:
    - pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeToolExecutionGateway.java
    - pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeToolExecutorBinding.java
    - pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeToolRegistry.java
  modified:
    - pi-testkit/pom.xml
    - pi-testkit/src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java
    - pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeToolInvoker.java
    - pi-testkit/src/test/java/io/github/pi_java/agent/testkit/runtime/FakeGeneralAgentLoopTest.java

key-decisions:
  - "Keep legacy FakeToolInvoker source-compatible, but wrap it behind FakeToolExecutorBinding for gateway-aware paths."
  - "Use DefaultToolExecutionGateway in the testkit fake gateway so fake E2E observes the same governed lifecycle event shape as production App gateway tests."
  - "Normalize gateway-emitted event sequence numbers in GeneralAgentLoop so gateway lifecycle events and loop terminal events remain one monotonic stream."

patterns-established:
  - "GeneralAgentLoop converts model ToolCall intents to ToolExecutionRequest and delegates to ToolExecutionGateway.ToolExecutionCommand with request, session, workspace, request context, and cancellation token."
  - "FakeToolExecutionGateway composes deterministic no-op validator/audit, allow-all payload limiter, fake redactor, fake preview, and FakePolicy-backed policy evaluator around DefaultToolExecutionGateway."
  - "Source scan tests assert GeneralAgentLoop contains gateway invocation and no direct `toolInvoker.invoke` bypass."

requirements-completed: [TOOL-02, TOOL-05, TOOL-06, E2E-02, E2E-03]

duration: 7m 16s
completed: 2026-06-14
---

# Phase 04 Plan 05: Gateway-Routed Testkit General Agent Loop Summary

**No-key General Agent testkit tool calls now execute through the governed ToolExecutionGateway with deterministic success, deny, and approval-required fakes.**

## Performance

- **Duration:** 7m 16s
- **Started:** 2026-06-14T19:24:32Z
- **Completed:** 2026-06-14T19:31:48Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments

- Added `pi-agent-app` as a `pi-testkit` dependency so testkit fakes can use App-layer gateway ports without leaking them into Domain.
- Added `FakeToolRegistry`, `FakeToolExecutorBinding`, and `FakeToolExecutionGateway` so old `FakeToolInvoker` fixtures can be registered as governed executor bindings.
- Rerouted `GeneralAgentLoop` model tool-call execution through `ToolExecutionGateway.execute(...)` and preserved legacy constructors by lazily wrapping `FakeToolInvoker` behind the fake gateway.
- Expanded `FakeGeneralAgentLoopTest` to prove successful model→tool→model flow, policy deny/no side effect, approval-required/no side effect, and no source-level direct invoker bypass.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add gateway-aware testkit fakes** - `e06ea48` (feat)
2. **Task 2: Reroute GeneralAgentLoop through ToolExecutionGateway** - `8c7bfcf` (feat)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-testkit/pom.xml` - Adds `pi-agent-app` dependency for gateway port access.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeToolExecutionGateway.java` - Deterministic fake gateway built on `DefaultToolExecutionGateway` collaborators.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeToolExecutorBinding.java` - Adapter from legacy `FakeToolInvoker` to App `ToolExecutorBinding`.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeToolRegistry.java` - In-memory testkit tool registry with default test descriptors.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeToolInvoker.java` - Keeps legacy API and exposes registered tool names for gateway wrapper construction.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java` - Delegates tool calls to `ToolExecutionGateway` and translates gateway statuses to existing run outcomes.
- `pi-testkit/src/test/java/io/github/pi_java/agent/testkit/runtime/FakeGeneralAgentLoopTest.java` - Adds gateway success/deny/approval and source-scan assertions.

## Decisions Made

- Preserved legacy `GeneralAgentLoop(ModelClient, ToolInvoker, ...)` and streaming constructor signatures for downstream compatibility, but migrated their implementation to a lazy gateway wrapper.
- Used gateway lifecycle events (`tool.proposed`, `tool.policy_decided`, `tool.started`, `tool.completed`, `tool.denied`, `tool.approval_required`) instead of the older `policy.decided` runtime event in new test assertions.
- Kept approval-required mapping as `RunStatus.POLICY_BLOCKED` in the fake loop until a future approval/resume runtime state exists, while preserving the specific `TOOL_APPROVAL_REQUIRED` lifecycle event for UI/E2E assertions.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed fake collaborator constructor mismatches**
- **Found during:** Task 1 compile verification
- **Issue:** New fake gateway initially used outdated constructor shapes for `PolicyEvaluation`, `ProvisionPreview`, `RedactedToolPayload`, and `LimitCheck`.
- **Fix:** Updated fake collaborators to use Optional-backed policy refs, the five-argument preview contract, three-argument redacted payloads, and current `LimitCheck.ok()` factory.
- **Files modified:** `FakeToolExecutionGateway.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-testkit -am -DskipTests compile` passed.
- **Committed in:** `e06ea48`

**2. [Rule 1 - Bug] Preserved monotonic event sequence across gateway and loop events**
- **Found during:** Task 2 test verification
- **Issue:** `DefaultToolExecutionGateway` maintains its own event sequence, causing duplicate/non-monotonic sequences when its lifecycle events are mixed with `GeneralAgentLoop` run/model events.
- **Fix:** Routed legacy wrapper gateway events back through the loop publish path and normalized any older sequence to the next loop sequence before publishing.
- **Files modified:** `GeneralAgentLoop.java`, `FakeGeneralAgentLoopTest.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-testkit -am -Dtest=FakeGeneralAgentLoopTest test` passed.
- **Committed in:** `8c7bfcf`

---

**Total deviations:** 2 auto-fixed (2 bugs)
**Impact on plan:** Both fixes were required for current contracts and event-order correctness. No architectural changes or scope expansion.

## Known Stubs

None. Stub-pattern scanning found only intentional null sentinels for mutually exclusive model clients, stream aggregation state, lazy gateway initialization, and fallback clocks; none are UI-facing placeholders or unresolved plan goals.

## Issues Encountered

- Task 2 verification initially failed because source scanning used a root-relative path while Surefire executed in the `pi-testkit` module directory; the assertion now supports both module-relative and reactor-root paths.
- Existing unrelated planning artifacts from earlier phases were already modified/untracked in the working tree before execution; they were left untouched because this parallel executor is scoped to Plan 04-05.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-testkit -am -DskipTests compile` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-testkit -am -Dtest=FakeGeneralAgentLoopTest test` — passed.
- `grep -n "toolInvoker.invoke" pi-testkit/src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java || true` — no matches.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-testkit,pi-agent-adapter-web -am -Dtest=FakeGeneralAgentLoopTest,CloudServerOpenAiCompatibleE2ETest test` — passed.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 04-07/04-08 can rely on testkit General Agent runs exercising the governed App gateway instead of direct tool invocation.
- Future SPI, Spring Bean, MCP, and plugin tool sources can use the same no-bypass pattern: normalize to registry descriptor/binding and invoke only through `ToolExecutionGateway`.

## Self-Check: PASSED

- Found `04-05-SUMMARY.md`, `FakeToolExecutionGateway.java`, and `GeneralAgentLoop.java` on disk.
- Verified task commits exist in `git log --oneline --all`: `e06ea48` and `8c7bfcf`.

---
*Phase: 04-governed-tool-registry-workspace-and-invocation-pipeline*
*Completed: 2026-06-14*
