---
phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
plan: 08
subsystem: governed-tool-cloud-e2e-and-contracts
tags: [java, spring-boot, e2e, tool-gateway, workspace, audit, redaction, contracts]

requires:
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: Gateway-routed GeneralAgentLoop and fake/testkit governance paths from Plan 04-05.
  - phase: 04-governed-tool-registry-workspace-and-invocation-pipeline
    provides: Cloud Server tool governance wiring, tool lifecycle DTO mapping, audit, built-ins, and /api/tools from Plan 04-07.
provides:
  - No-key Cloud Server E2E proving successful model-to-tool-to-model execution through ToolExecutionGateway.
  - No-key Cloud Server E2E proving deny and approval-required paths emit events/audit and prevent unauthorized side effects.
  - Security-redaction E2E proving fake sensitive values are absent from default REST/event/audit/persisted tool payloads.
  - Downstream Phase 4 governed tool contract index and requirement traceability evidence.
affects: [phase-05-runtime-cockpit, phase-06-extension-surface, phase-07-mcp-tools, phase-08-plugin-tools, phase-09-hardening]

tech-stack:
  added: []
  patterns: [no-key-cloud-e2e, in-memory-cloud-e2e, governed-tool-gateway-only-path, redacted-tool-lifecycle-events, downstream-contract-index]

key-files:
  created:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/CloudServerGovernedToolE2ETest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/GovernedToolSecurityRedactionE2ETest.java
    - docs/phase-04-governed-tool-contracts.md
    - .planning/phases/04-governed-tool-registry-workspace-and-invocation-pipeline/04-08-SUMMARY.md
  modified:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/InMemoryCloudE2EConfiguration.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultRunQueryService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultToolExecutionGateway.java
    - pi-testkit/src/test/java/io/github/pi_java/agent/testkit/GeneralAgentLoopStreamingTest.java
    - .planning/REQUIREMENTS.md

key-decisions:
  - "Use in-memory Cloud Server E2E repositories for Plan 04-08 so governed tool tests remain no-key and no-Docker while still exercising REST create-run, worker activation, event history, and audit paths."
  - "Model approval-required tool calls as non-executing policy-blocked runtime outcomes until Phase 5 adds approval cards and resume flows, while preserving explicit preview and approval-required lifecycle events."
  - "Document local-temp workspace and allowlisted command execution as deterministic dev/test infrastructure, not a production sandbox."
  - "Redact raw tool output before payload summary/preview generation so default REST/events/audit payloads cannot leak sensitive result values."

requirements-completed: [E2E-02, E2E-03, E2E-06, TOOL-01, TOOL-02, TOOL-03, TOOL-04, TOOL-05, TOOL-06, TOOL-07, WORK-03, WORK-07, WORK-08, OPS-02, OPS-03, OPS-05]

duration: 4m 40s retry execution; prior task commits preserved
completed: 2026-06-15
---

# Phase 04 Plan 08: Governed Tool E2E, Redaction E2E, and Contract Documentation Summary

**No-key Cloud Server E2E now proves governed tool success, deny, approval, preview, workspace-bound command, audit/event persistence, and fake-secret redaction through the product runtime path.**

## Performance

- **Duration:** 4m 40s retry execution, preserving valid prior 04-08 task commits.
- **Started:** 2026-06-15T00:56:00Z
- **Completed:** 2026-06-15T01:00:40Z
- **Tasks:** 3
- **Files modified:** 8 plan-related files

## Accomplishments

- Added `CloudServerGovernedToolE2ETest` with REST-created runs, in-memory Cloud E2E persistence/audit fallback, runtime worker activation, and assertions over persisted event history plus audit records.
- Proved successful read-only and allowlisted workspace command tool calls complete model→tool→model runs through `ToolExecutionGateway` and emit `tool.proposed`, `tool.policy_decided`, `tool.started`, and `tool.completed` lifecycle events.
- Proved deny and approval-required paths emit policy/preview/denial/approval audit and events while executor side-effect counters remain unchanged.
- Added `GovernedToolSecurityRedactionE2ETest` with fake sensitive value `PI_PHASE4_FAKE_SECRET_DO_NOT_LEAK` defined only in test code, scanning REST run detail, event history DTOs, persisted RunEvent strings, audit detail strings, and exception paths.
- Wrote `docs/phase-04-governed-tool-contracts.md` as the downstream contract index for tool descriptors, registry, gateway, validation, policy, preview/approval/sandbox semantics, lifecycle events, audit/redaction/payload limits, built-ins, local-temp limitations, REST catalog API, E2E evidence, and deferrals.
- Updated `.planning/REQUIREMENTS.md` with Phase 4 validation evidence for WORK-03/07/08, TOOL-01..07, OPS-02/03/05, and E2E-02/03/06.

## Task Commits

Each task was committed atomically; this retry preserved valid prior partial commits and added one correctness fix commit:

1. **Task 1: Add governed tool Cloud Server E2E** - `7970a81` (feat)
2. **Task 2: Add security redaction E2E** - `c28471a` (test)
3. **Task 3: Document Phase 4 contracts and update requirement status** - `398a490` (docs)
4. **Retry correctness fix: redact raw output and align streaming lifecycle assertions** - `8c3364e` (fix)

**Plan metadata:** pending final docs/state commit

## Files Created/Modified

- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/CloudServerGovernedToolE2ETest.java` - Product-path no-key E2E for gateway success, deny, approval-required, preview, workspace command, event history, and audit assertions.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/GovernedToolSecurityRedactionE2ETest.java` - Fake-secret absence E2E across REST DTOs, persisted events, audit details, and safe exception paths.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/InMemoryCloudE2EConfiguration.java` - In-memory E2E stores enhanced for run detail/result/event/audit inspection.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultRunQueryService.java` - Supports the product-like run detail/history paths needed by E2E verification.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultToolExecutionGateway.java` - Redacts raw tool output before generating payload previews/summaries.
- `pi-testkit/src/test/java/io/github/pi_java/agent/testkit/GeneralAgentLoopStreamingTest.java` - Aligns streaming assertions with current `tool.*` lifecycle event names.
- `docs/phase-04-governed-tool-contracts.md` - Downstream contract documentation for Phases 5-8.
- `.planning/REQUIREMENTS.md` - Requirement validation evidence for Phase 4 completion.

## Decisions Made

- Used the existing in-memory Cloud E2E pattern instead of requiring Docker/Testcontainers because the plan explicitly requires no-key/no-Docker focused E2E where Docker is unavailable.
- Kept E2E runtime configuration test-local with fake model scripts and probe counters, avoiding real provider keys, real secrets, broad shell access, or host filesystem assumptions.
- Preserved `ToolExecutionGateway` as the only runtime invocation path; tests assert lifecycle events and audit actions rather than unit-only gateway behavior.
- Treated raw tool output redaction before summarization as a correctness/security requirement because preview generation from raw maps can leak sensitive result values even when DTO mapping filters sensitive keys.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Redacted raw tool output before payload preview generation**
- **Found during:** Retry verification of Task 2 redaction E2E.
- **Issue:** `DefaultToolExecutionGateway` summarized raw executor output before redaction, so `payloadPreview` could include `PI_PHASE4_FAKE_SECRET_DO_NOT_LEAK` in default event/audit payloads.
- **Fix:** Redact raw output first, summarize the redacted map, then redact the summary and union raw-output redacted fields into the final result metadata.
- **Files modified:** `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultToolExecutionGateway.java`
- **Verification:** `GovernedToolSecurityRedactionE2ETest` and `DefaultToolExecutionGatewayTest` passed.
- **Commit:** `8c3364e`

**2. [Rule 1 - Bug] Preserved valid partial streaming lifecycle assertion update**
- **Found during:** Retry working-tree inspection.
- **Issue:** Previous failed attempt left `GeneralAgentLoopStreamingTest` expecting current `TOOL_POLICY_DECIDED` and `TOOL_STARTED` lifecycle events instead of the obsolete `POLICY_DECIDED` event. This was valid partial work required by Phase 4 gateway routing.
- **Fix:** Kept the assertion update, verified the focused test, and committed it with the retry fix.
- **Files modified:** `pi-testkit/src/test/java/io/github/pi_java/agent/testkit/GeneralAgentLoopStreamingTest.java`
- **Verification:** `GeneralAgentLoopStreamingTest` passed.
- **Commit:** `8c3364e`

---

**Total deviations:** 2 auto-fixed bugs/correctness issues.
**Impact on plan:** Strengthened the planned redaction guarantee and preserved valid partial work; no architectural changes or scope expansion.

## Known Stubs

None. Stub-pattern scan found no TODO/FIXME/placeholder/coming-soon/not-available markers in the new governed tool E2E tests or Phase 4 contract document. The fake secret and fake runtime/provider usage are intentional no-key E2E fixtures required by the plan, not unresolved product stubs.

## Issues Encountered

- The previous executor had already created valid 04-08 commits but did not create the summary/state/roadmap completion artifacts. This retry preserved those commits rather than redoing completed work.
- The working tree contained unrelated pre-existing Phase 02/03 planning doc modifications/untracked files. They were not staged or modified by this plan.
- An initial retry run of `GovernedToolSecurityRedactionE2ETest` exposed the raw-output preview leak fixed in `8c3364e`.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=CloudServerGovernedToolE2ETest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=GovernedToolSecurityRedactionE2ETest test` — passed after raw-output redaction fix.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=CloudServerGovernedToolE2ETest,GovernedToolSecurityRedactionE2ETest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-app -am -Dtest=DefaultToolExecutionGatewayTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-testkit -am -Dtest=GeneralAgentLoopStreamingTest test` — passed.
- `test -f docs/phase-04-governed-tool-contracts.md && grep -q "TOOL-01" .planning/REQUIREMENTS.md && grep -q "ToolExecutionGateway" docs/phase-04-governed-tool-contracts.md` — passed.

## User Setup Required

None - focused E2E uses fake/in-memory dependencies and requires no provider keys or Docker.

## Next Phase Readiness

- Phase 5 Web Console can consume `/api/tools` and `tool.lifecycle` events knowing redacted summaries, preview metadata, policy decisions, and approval-required events exist on REST/event-history paths.
- Phase 6/7/8 extension, MCP, and plugin tools have a documented gateway-only contract and concrete E2E proof that tool sources must normalize into `ToolDescriptor`/`ToolRegistry` and invoke through `ToolExecutionGateway`.

## Self-Check: PASSED

- Found key files on disk: `04-08-SUMMARY.md`, `CloudServerGovernedToolE2ETest.java`, `GovernedToolSecurityRedactionE2ETest.java`, and `docs/phase-04-governed-tool-contracts.md`.
- Verified commits exist in `git log --oneline --all`: `7970a81`, `c28471a`, `398a490`, and `8c3364e`.
- Verified unrelated Phase 02/03 planning doc changes remain unstaged for this plan.

---
*Phase: 04-governed-tool-registry-workspace-and-invocation-pipeline*
*Completed: 2026-06-15*
