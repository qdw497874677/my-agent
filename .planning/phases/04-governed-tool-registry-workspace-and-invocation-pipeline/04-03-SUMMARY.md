---
phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
plan: 03
subsystem: app-tool-execution-gateway
tags: [java, app, tool-gateway, policy, audit, events, redaction, cola]

requires:
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: Framework-free ToolDescriptor, ToolExecutionRequest/Result, ProvisionPreview, and tool lifecycle events from Plan 04-01.
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: Descriptor-first ToolRegistry and low-level ToolExecutorBinding from Plan 04-02.
provides:
  - Single App-layer ToolExecutionGateway command path that resolves descriptors before invoking executor bindings.
  - Pluggable validator, policy evaluator, preview generator, payload limiter, redactor, audit, and event orchestration ports.
  - DefaultToolExecutionGateway behavior tests proving invalid, deny, block, approval, sandbox, cancel, timeout, and failure branches do not bypass governance.
affects: [phase-04-tool-validation, phase-04-built-in-tools, phase-05-agent-web-console, phase-06-extension-surface, phase-07-mcp-tools, phase-08-plugin-tools]

tech-stack:
  added: []
  patterns: [single-governed-gateway, descriptor-first-execution, policy-preserving-decisions, redacted-lifecycle-events, tdd-no-bypass-tests]

key-files:
  created:
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolExecutionGateway.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolArgumentValidator.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolPolicyEvaluator.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolRedactor.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolPayloadLimiter.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolPreviewGenerator.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultToolExecutionGateway.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultToolExecutionGatewayTest.java
  modified: []

key-decisions:
  - "Keep schema validation, policy evaluation, preview generation, payload limiting, and redaction as App-layer ports so concrete libraries and policy engines remain replaceable."
  - "Preserve REQUIRE_APPROVAL and REQUIRE_SANDBOX as non-executing gateway outcomes instead of converting them to generic deny."
  - "Emit and audit redacted summary-level lifecycle data from the gateway while leaving raw tool output out of default events and audits."

patterns-established:
  - "DefaultToolExecutionGateway order is resolve descriptor/binding, publish proposed, validate/limit args, evaluate policy, generate preview, gate denied/approval/sandbox/cancelled branches, execute binding, redact/summarize, audit, and publish terminal lifecycle."
  - "Executor bindings remain low-level and are invoked only after validation, policy, preview, and cancellation gates succeed."
  - "Gateway collaborator ports use plain Java records and Domain/App types only; no Spring, Jackson, networknt, MCP, PF4J, or provider SDK imports."

requirements-completed: [TOOL-02, TOOL-03, TOOL-04, TOOL-05, TOOL-06, OPS-02, OPS-03, OPS-05, WORK-08]

duration: 8m 42s
completed: 2026-06-14
---

# Phase 04 Plan 03: Governed ToolExecutionGateway Summary

**Single governed App-layer tool invocation gateway with pluggable validation, policy, preview, payload limiting, redaction, audit, and lifecycle event hooks.**

## Performance

- **Duration:** 8m 42s
- **Started:** 2026-06-14T18:54:36Z
- **Completed:** 2026-06-14T19:03:18Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments

- Added the `ToolExecutionGateway` App port plus collaborator ports for argument validation, policy evaluation, redaction, payload limiting, and preview generation.
- Added `DefaultToolExecutionGateway` as the mandatory orchestration choke point over `ToolRegistry.resolve(...)`, policy/preview gates, executor binding invocation, audit records, and tool lifecycle `RunEvent`s.
- Preserved all policy decision meanings: `DENY`/`BLOCK` return denied, `REQUIRE_APPROVAL` returns `APPROVAL_REQUIRED`, and `REQUIRE_SANDBOX` returns `SANDBOX_REQUIRED`; none invoke the executor binding.
- Added deterministic no-bypass tests for allow/success, invalid arguments, deny, block, approval-required, sandbox-required, preview, cancellation, timeout, and executor failure paths.

## Task Commits

Each task was committed atomically. Task 2 followed TDD with a RED test commit and GREEN implementation commit:

1. **Task 1: Define gateway collaborator ports**
   - `e1e4497` feat: define tool execution gateway ports
2. **Task 2: Implement DefaultToolExecutionGateway with no-bypass tests**
   - `da022e9` test: add failing tool execution gateway tests
   - `a1366ab` feat: implement governed tool execution gateway

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolExecutionGateway.java` - App gateway port and execution command carrying request, session/workspace context, and cancellation token.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolArgumentValidator.java` - Pluggable argument validation result contract.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolPolicyEvaluator.java` - Policy evaluation request/decision metadata preserving allow, deny, approval, sandbox, and block meanings.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolRedactor.java` - Redacted payload summary contract.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolPayloadLimiter.java` - Argument/result payload limit and summarization contract.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolPreviewGenerator.java` - Provision preview generation request contract.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultToolExecutionGateway.java` - App use case implementing descriptor-first governed execution orchestration.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultToolExecutionGatewayTest.java` - Behavior tests for no-bypass, events, audit, preview, redaction, cancellation, timeout, and failures.

## Decisions Made

- Kept validation implementation abstract; Plan 04-04 owns concrete JSON Schema validation while this plan defines and uses the validator port.
- Modeled approval and sandbox gates as terminal/suspended gateway outcomes for now, making them visible in events and audit without executing side effects before Phase 5 approval UI or later sandbox support exists.
- Used redacted summary maps in events/audit and returned results, and discarded raw output from the gateway-normalized result path after redaction/summarization.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Avoided record accessor name collisions**
- **Found during:** Task 1 compile verification
- **Issue:** Static factory methods named `valid()` and `allowed()` collided with Java record accessor method names for boolean components.
- **Fix:** Renamed factories to `ok()` for `ValidationResult` and `LimitCheck`.
- **Files modified:** `ToolArgumentValidator.java`, `ToolPayloadLimiter.java`
- **Commit:** `e1e4497`

**2. [Rule 1 - Bug] Scoped executor exception handling to preserve successful redaction path**
- **Found during:** Task 2 GREEN verification
- **Issue:** Runtime exceptions from post-execution redaction would normalize an otherwise successful tool execution to failure, obscuring implementation/test issues.
- **Fix:** Narrowed the executor `try/catch` to only the executor binding call so timeout/failure normalization applies to executor failures, while redaction/limiting bugs fail loudly during tests.
- **Files modified:** `DefaultToolExecutionGateway.java`
- **Commit:** `a1366ab`

## Known Stubs

None. Stub-pattern scanning found only null checks, optional preview initialization, and gateway fallback values (`default`, no-summary text) that are control-flow/default safety behavior rather than UI-facing placeholder data.

## Issues Encountered

- Task 2 RED intentionally failed because `DefaultToolExecutionGateway` did not exist yet.
- The first GREEN iteration exposed constructor overload ambiguity around nullable vs `Optional` error/preview fields in `ToolExecutionResult`; tests and implementation were adjusted to pass explicit `Optional` values where needed.
- Side-effectful descriptors correctly produced preview events on the success path, so the success event assertion was relaxed from contiguous sequence to subsequence.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -am -DskipTests compile` — passed.
- `grep -R "org.springframework\|com.fasterxml\|com.networknt" pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool` — no matches.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -am -Dtest=DefaultToolExecutionGatewayTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -am test` — passed; existing App architecture and use-case tests remain green.
- Searched `DefaultToolExecutionGateway.java` for required `toolRegistry.resolve`, `eventSink.publish`, and `auditRepository.record` calls — all present.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 04-04 can implement concrete JSON Schema argument validation behind `ToolArgumentValidator` without changing the gateway contract.
- Later built-in/SPI/Spring/MCP/plugin tool sources can normalize to `ToolRegistry` descriptor/binding pairs and consume the same gateway without executor bypass.

## Self-Check: PASSED

- Found `ToolExecutionGateway.java`, `ToolPolicyEvaluator.java`, `DefaultToolExecutionGateway.java`, `DefaultToolExecutionGatewayTest.java`, and this `04-03-SUMMARY.md` on disk.
- Verified commits exist in `git log --oneline --all`: `e1e4497`, `da022e9`, and `a1366ab`.

---
*Phase: 04-governed-tool-registry-workspace-and-invocation-pipeline*
*Completed: 2026-06-14*
