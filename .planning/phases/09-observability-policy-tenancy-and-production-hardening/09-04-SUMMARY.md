---
phase: 09-observability-policy-tenancy-and-production-hardening
plan: 04
subsystem: observability
tags: [micrometer, opentelemetry, tool-governance, policy-telemetry, spring-wiring]

# Dependency graph
requires:
  - phase: 09-observability-policy-tenancy-and-production-hardening
    provides: PiTelemetry primitives and telemetry context/redaction from Plan 09-02
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: ToolExecutionGateway and ToolPolicyEvaluator chokepoints
provides:
  - Safe telemetry decorators for governed tool execution and policy decisions
  - Adapter Web composition that wraps default tool governance beans when PiTelemetry is present
  - Redaction tests proving raw arguments, raw output, and fake secrets do not become metric tags
affects: [phase-09, observability, tool-governance, admin-operations]

# Tech tracking
tech-stack:
  added: []
  patterns: [decorator, telemetry-chokepoint, optional-spring-composition, redacted-low-cardinality-tags]

key-files:
  created:
    - pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/TelemetryToolExecutionGateway.java
    - pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/TelemetryToolPolicyEvaluator.java
    - pi-agent-infrastructure-observability/src/test/java/io/github/pi_java/agent/infrastructure/observability/TelemetryToolExecutionGatewayTest.java
    - pi-agent-infrastructure-observability/src/test/java/io/github/pi_java/agent/infrastructure/observability/TelemetryToolPolicyEvaluatorTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ToolTelemetryWiringTest.java
    - .planning/phases/09-observability-policy-tenancy-and-production-hardening/deferred-items.md
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ToolGovernanceBeanConfiguration.java

key-decisions:
  - "Instrument tool execution and policy decisions at the governed App-port chokepoints instead of individual tool implementations."
  - "Keep telemetry tags low-cardinality and redacted: tool_id, status/decision, policy_ref, and policy_decision only."
  - "Make Adapter Web telemetry wrapping optional on PiTelemetry so test/minimal contexts retain default fallback behavior."

patterns-established:
  - "Telemetry decorators open PiTelemetryContext from RequestContext, start spans, emit Micrometer counters/timers, and never tag raw arguments/results."
  - "Spring composition wraps default governance beans with telemetry decorators only when PiTelemetry is available."

requirements-completed: [OPS-01]

# Metrics
duration: 10m40s
completed: 2026-06-19
---

# Phase 09 Plan 04: Governed Tool and Policy Telemetry Summary

**Governed tool execution and policy evaluation now emit redacted Micrometer/OpenTelemetry telemetry from the single safety chokepoints.**

## Performance

- **Duration:** 10m40s
- **Started:** 2026-06-19T22:44:37Z
- **Completed:** 2026-06-19T22:55:17Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments

- Added `TelemetryToolExecutionGateway`, a `ToolExecutionGateway` decorator that records `pi.tool.executions.total`, `pi.tool.execution.duration`, and a `pi.tool.execution` span with sanitized `tool_id`, `status`, and `policy_decision` metadata.
- Added `TelemetryToolPolicyEvaluator`, a `ToolPolicyEvaluator` decorator that records `pi.policy.decisions.total` and a `pi.policy.decision` span with sanitized `tool_id`, `decision`, `policy_ref`, and `status` metadata.
- Wired Adapter Web tool governance composition to wrap default policy and gateway beans with telemetry decorators whenever `PiTelemetry` is available, while preserving default fallback behavior.
- Added redaction-focused unit tests containing `PI_PHASE9_FAKE_SECRET_DO_NOT_LEAK` to prove raw tool arguments, raw outputs, and secret-like values are not emitted as metric tags.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Add failing test for tool execution telemetry** - `5716c0a` (test)
2. **Task 1 GREEN: Add ToolExecutionGateway telemetry decorator** - `a121b1f` (feat)
3. **Task 2: Add policy decision telemetry decorator and Adapter wiring** - `b308415` (feat)

**Plan metadata:** pending final docs commit.

_Note: Task 1 followed the requested TDD test → implementation commit split. Task 2 was committed as one feature commit containing its decorator, tests, wiring, and deferred verification note._

## Files Created/Modified

- `pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/TelemetryToolExecutionGateway.java` - Decorates governed tool execution with safe metrics/spans and exposes the delegate for wiring verification.
- `pi-agent-infrastructure-observability/src/main/java/io/github/pi_java/agent/infrastructure/observability/TelemetryToolPolicyEvaluator.java` - Decorates policy evaluation with safe decision metrics/spans and exposes the delegate for wiring verification.
- `pi-agent-infrastructure-observability/src/test/java/io/github/pi_java/agent/infrastructure/observability/TelemetryToolExecutionGatewayTest.java` - Verifies successful/error tool execution telemetry and raw argument/output redaction.
- `pi-agent-infrastructure-observability/src/test/java/io/github/pi_java/agent/infrastructure/observability/TelemetryToolPolicyEvaluatorTest.java` - Verifies successful/error policy decision telemetry and raw argument redaction.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ToolGovernanceBeanConfiguration.java` - Wraps default tool policy and execution gateway beans with telemetry decorators when `PiTelemetry` is available.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ToolTelemetryWiringTest.java` - Asserts Adapter Web uses `TelemetryToolPolicyEvaluator` and `TelemetryToolExecutionGateway` in the configured bean chain.
- `.planning/phases/09-observability-policy-tenancy-and-production-hardening/deferred-items.md` - Records out-of-scope verification blockers encountered in neighboring/parallel work.

## Decisions Made

- Instrumented the existing `ToolExecutionGateway` and `ToolPolicyEvaluator` App ports, not individual tool implementations, so all built-in, extension, MCP, and plugin tools inherit one observability boundary.
- Used low-cardinality, redacted tags only: tool IDs, execution statuses, policy decisions/refs, and generic error status. Raw arguments, raw outputs, summaries, reasons, and exception messages are intentionally not tags.
- Kept telemetry wrapping optional on `PiTelemetry` to preserve simple test contexts and default no-telemetry composition.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Recorded unrelated reactor compile blocker instead of modifying MCP infrastructure**
- **Found during:** Task 2 verification.
- **Issue:** The full Adapter Web verification reactor first stopped in `pi-agent-infrastructure-mcp` because `McpToolRegistry` referenced an unavailable `McpServerProperties.transportKind()` method.
- **Fix:** Did not alter out-of-scope MCP code; recorded the blocker in `deferred-items.md` and verified the owned observability tests directly.
- **Files modified:** `.planning/phases/09-observability-policy-tenancy-and-production-hardening/deferred-items.md`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability -am -Dtest=TelemetryToolExecutionGatewayTest,TelemetryToolPolicyEvaluatorTest test` passed.
- **Committed in:** `b308415`

---

**Total deviations:** 1 tracked blocking issue (out of scope, deferred)
**Impact on plan:** Owned telemetry code and tests were completed; full cross-module verification is currently affected by parallel/out-of-scope compile failures.

## Issues Encountered

- Full plan verification command `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability,pi-agent-adapter-web -am -Dtest=TelemetryToolExecutionGatewayTest,TelemetryToolPolicyEvaluatorTest,ToolTelemetryWiringTest test` is blocked in the current parallel workspace by unrelated Adapter Web test compilation in `StructuredLoggingRedactionTest` (`LoggingEventCompositeJsonEncoder` type incompatibility). This is outside Plan 09-04 ownership and appears related to concurrent Plan 09-06 work.
- Earlier in Task 2 verification, the reactor also surfaced an unrelated MCP compile mismatch (`McpServerProperties.transportKind()`), documented in `deferred-items.md`.
- Direct verification of the owned observability telemetry tests passed.

## Verification

- ✅ `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability -am -DskipTests compile`
- ✅ `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-observability -am -Dtest=TelemetryToolExecutionGatewayTest,TelemetryToolPolicyEvaluatorTest test`
- ⚠️ Full requested cross-module command blocked by out-of-scope/parallel compile failures noted above.

## Known Stubs

None found in files created/modified for this plan.

## Self-Check: PASSED

- Found summary file at `.planning/phases/09-observability-policy-tenancy-and-production-hardening/09-04-SUMMARY.md`.
- Found key created files: `TelemetryToolExecutionGateway.java`, `TelemetryToolPolicyEvaluator.java`, and `ToolTelemetryWiringTest.java`.
- Found task commits: `5716c0a`, `a121b1f`, and `b308415`.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Tool and policy telemetry decorators are ready for Admin operations read-model aggregation in later Phase 9 plans.
- Before final Phase 9 verification, resolve the out-of-scope parallel compile blockers in MCP/structured logging so full Adapter Web verification can run end-to-end.

---
*Phase: 09-observability-policy-tenancy-and-production-hardening*
*Completed: 2026-06-19*
