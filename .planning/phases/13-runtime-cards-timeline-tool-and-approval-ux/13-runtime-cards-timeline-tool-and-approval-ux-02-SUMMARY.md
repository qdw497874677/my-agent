---
phase: 13-runtime-cards-timeline-tool-and-approval-ux
plan: 02
subsystem: ui
tags: [vaadin, mobile, tool-cards, redaction, runtime-details]

requires:
  - phase: 13-runtime-cards-timeline-tool-and-approval-ux-01
    provides: RuntimeDetailRedactor reusable redaction utility and RunEventRenderer tool-card seam
provides:
  - Structured mobile ToolCallCard summary with tool/source/status/policy/approval/duration/error/summary fields
  - Redacted structured input/output and advanced diagnostic detail layers for tool runtime events
  - Contract coverage for visible MCARD-02 fields, stable data attributes, no raw secret exposure, and preserved one-card-per-event rendering
affects: [phase-13-runtime-cards, tool-cards, approval-card-followup, mobile-console-event-feed]

tech-stack:
  added: []
  patterns: [Vaadin Details for tool-card layered redacted payload inspection, RuntimeDetailRedactor for all payload-derived tool text]

key-files:
  created: []
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ToolCallCard.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeDetailRedactor.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleCatalogAndToolCardsTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleRuntimeCardsTest.java

key-decisions:
  - "Tool lifecycle rendering remains one ToolCallCard per runtime event; no toolCallId aggregation cache was introduced."
  - "Tool payload-derived summary, structured detail, and advanced diagnostics now reuse RuntimeDetailRedactor rather than local ad-hoc redaction."

patterns-established:
  - "ToolCallCard: visible mobile summary labels for Tool, Source, Status, Policy, Approval, Duration, Error, Summary, and supporting risk/progress/result context."
  - "ToolCallCard layered details: `Input / output summary` for structured summaries and `Advanced redacted detail` for sequence/type/schema/policy/preview/diagnostics."

requirements-completed: [MCARD-02, MCARD-03]

duration: 11m09s
completed: 2026-06-24
---

# Phase 13 Plan 02: Structured Redacted Tool Execution Cards Summary

**Mobile tool execution cards with visible policy/approval summary fields and layered redacted input/output diagnostics**

## Performance

- **Duration:** 11m09s
- **Started:** 2026-06-24T05:47:33Z
- **Completed:** 2026-06-24T05:58:42Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- Refactored `ToolCallCard` from a pipe-delimited lifecycle span into a structured Vaadin card with `pi-tool-call-card pi-card` styling hooks and stable tool attributes.
- Made key MCARD-02 fields visible by default: tool name, source, status, policy decision/state, approval state, duration, error, and summary/result context.
- Added two expandable `Details` layers for tool payload inspection: `Input / output summary` and `Advanced redacted detail`.
- Routed all tool payload-derived rendering through `RuntimeDetailRedactor` and strengthened redaction tests for API keys, passwords, raw token values, authorization bearer values, and `sk-live-*` secrets.
- Preserved `RunEventRenderer` tool lifecycle semantics: one rendered `ToolCallCard` per incoming tool runtime event, with no aggregation map/cache.

## Task Commits

Each task was committed atomically:

1. **Task 1: Refactor ToolCallCard into structured summary and detail layers** - `fa0ab1b` (feat)
2. **Task 2: Add redacted input/output and advanced diagnostics sections** - `6f4d0af` (feat)

_Note: TDD RED changes were created and verified as failing before GREEN implementation; per-task commits capture the final passing task states._

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ToolCallCard.java` - Structured mobile tool card with visible summary labels, stable data attributes, redacted structured details, and redacted advanced diagnostics.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeDetailRedactor.java` - Redaction utility now ranks sensitive keys first before bounded formatting so secret markers survive truncation as `[REDACTED]`.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleCatalogAndToolCardsTest.java` - Tool card contract coverage for visible source/policy/approval/duration fields, attributes, and raw secret suppression.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleRuntimeCardsTest.java` - Runtime/tool card detail-layer contract coverage for redacted structured and advanced diagnostics.

## Decisions Made

- Kept tool-card implementation inside adapter-web Vaadin UI classes and reused the Plan 01 package-private `RuntimeDetailRedactor`; no public REST/SSE DTOs or Domain/App contracts changed.
- Kept one-card-per-tool-event semantics and did not introduce `Map<String, ToolCallCard>`, `toolCallId` aggregation caches, or mutation of prior lifecycle cards.
- Used inline Vaadin `Details` sections instead of modal/dialog primitives, matching Phase 13 decisions to keep dense diagnostics viewport-safe and inline.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Preserved redaction evidence before bounded truncation**
- **Found during:** Task 2 (Add redacted input/output and advanced diagnostics sections)
- **Issue:** Adding a tool detail fixture exposed that bounded diagnostic text could truncate after a long non-sensitive field before any redacted secret marker appeared, making existing redaction assertions brittle and reducing audit confidence.
- **Fix:** Updated `RuntimeDetailRedactor.stringifyValue(...)` to rank sensitive-looking keys first when rendering maps, ensuring bounded details retain `[REDACTED]` markers while still avoiding raw sensitive payload exposure.
- **Files modified:** `RuntimeDetailRedactor.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleCatalogAndToolCardsTest,WebConsoleRuntimeCardsTest test`
- **Committed in:** `6f4d0af`

---

**Total deviations:** 1 auto-fixed (1 Rule 1 bug)
**Impact on plan:** The fix strengthened the planned redaction contract and did not expand scope or alter public APIs.

## Issues Encountered

- Existing repository had unrelated modified/untracked files before this executor started. Only Phase 13 Plan 02 files were staged and committed.
- Initial RED test fixture exceeded `Map.of(...)` entry limits; it was corrected to `Map.ofEntries(...)` before implementation and committed with the task's test changes.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None. Stub-pattern scan found only null/blank guards and safe empty fallbacks in modified runtime-card utilities; no placeholder/mock data feeds the tool-card UI.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleCatalogAndToolCardsTest,WebConsoleRuntimeCardsTest test` — passed, 13 tests.
- Searched `ToolCallCard.java` for `Map<String, ToolCallCard>` and `toolCallId` cache patterns — no lifecycle aggregation cache introduced.
- Searched Console UI classes for new Dialog/ConfirmDialog/Notification/MenuBar/ContextMenu primitives — no new modal primitives introduced.

## Next Phase Readiness

- Phase 13 Plan 03 can build approval cards against the same redaction/detail conventions while keeping inline risk-first UI semantics.
- Browser/CSS hardening in Plan 04 can target stable tool hooks: `data-event-category="tool"`, `data-tool-status`, `data-tool-name`, `data-tool-source`, `data-policy-state`, and `data-detail-layer`.

## Self-Check: PASSED

- Verified key files exist: `ToolCallCard.java`, `RuntimeDetailRedactor.java`, `WebConsoleCatalogAndToolCardsTest.java`, and `WebConsoleRuntimeCardsTest.java`.
- Verified task commits exist: `fa0ab1b` and `6f4d0af`.

---
*Phase: 13-runtime-cards-timeline-tool-and-approval-ux*
*Completed: 2026-06-24*
