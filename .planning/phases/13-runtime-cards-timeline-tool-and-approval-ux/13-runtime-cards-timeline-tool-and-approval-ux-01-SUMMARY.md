---
phase: 13-runtime-cards-timeline-tool-and-approval-ux
plan: 01
subsystem: ui
tags: [vaadin, mobile, runtime-cards, redaction, event-feed]

requires:
  - phase: 12-console-mobile-first-flow
    provides: Existing Console Chat/Event Feed seam and mobile-first run-event flow
provides:
  - RuntimeDetailRedactor utility for conservative redaction and bounded diagnostics
  - RuntimeEventCard compact timeline-style card with layered redacted details
  - RunEventRenderer wiring for non-tool runtime events inside the existing feed
affects: [phase-13-runtime-cards, console-event-feed, tool-cards, approval-cards]

tech-stack:
  added: []
  patterns: [Vaadin Details for layered inline diagnostics, reusable adapter-web redaction utility]

key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeDetailRedactor.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeEventCard.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleRuntimeCardsTest.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java

key-decisions:
  - "Runtime cards stay inside RunEventRenderer and ChatEventStreamPanel appendEvent seams instead of adding a standalone timeline route."
  - "Sensitive runtime diagnostics use a package-private adapter-web redaction utility so future tool and approval cards can reuse the same conservative behavior."

patterns-established:
  - "RuntimeEventCard: compact visible summary plus inline Details and Advanced redacted detail sections."
  - "RunEventRenderer: specialized tool/approval cards remain authoritative; all other runtime branches attach RuntimeEventCard components while preserving text/category contracts."

requirements-completed: [MCARD-01, MCARD-03, MCARD-05]

duration: 9min
completed: 2026-06-24
---

# Phase 13 Plan 01: Runtime Cards Foundation Summary

**Compact Vaadin runtime event cards with reusable redacted diagnostics wired into the existing Console event feed**

## Performance

- **Duration:** 9 min
- **Started:** 2026-06-24T05:33:43Z
- **Completed:** 2026-06-24T05:42:18Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- Added `RuntimeDetailRedactor` for conservative redaction of API keys, passwords, secrets, tokens, authorization/bearer values, and known raw token forms.
- Added `RuntimeEventCard` with compact visible status/type/timestamp/summary and two inline `Details` layers: `Details` and `Advanced redacted detail`.
- Wired non-tool runtime event branches in `RunEventRenderer` to return `RuntimeEventCard` components while preserving model/policy/status/terminal/generic text and category contracts.
- Preserved specialized `ToolCallCard` and `ApprovalCard` routing for tool lifecycle and approval-required events.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Add reusable redaction and runtime-card contracts** - `0a207d4` (test)
2. **Task 1 GREEN: Add reusable redaction and runtime-card contracts** - `a7c3452` (feat)
3. **Task 2 RED: Wire runtime event cards through RunEventRenderer** - `0a373fe` (test)
4. **Task 2 GREEN: Wire runtime event cards through RunEventRenderer** - `165d67f` (feat)

_Note: TDD tasks produced separate RED and GREEN commits._

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeDetailRedactor.java` - Package-private runtime diagnostic formatter/redactor with `redact`, `stringify`, and `shorten` helpers.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeEventCard.java` - Compact Vaadin `Div` card with stable `data-*` hooks and layered inline details.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RunEventRenderer.java` - Non-tool runtime branches now attach `RuntimeEventCard` components while preserving feed text/category behavior.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleRuntimeCardsTest.java` - Contracts for summary visibility, redaction, attributes, renderer runtime-card routing, and specialized card preservation.

## Decisions Made

- Kept runtime card rendering in the existing `RunEventRenderer.render(...)` → `ChatEventStreamPanel.appendEvent(...)` seam to satisfy the no-new-route/no-new-modal Phase 13 decision.
- Implemented runtime diagnostics as inline Vaadin `Details` components rather than Dialog/ConfirmDialog/Notification/MenuBar/ContextMenu primitives.
- Kept `RuntimeDetailRedactor` package-private/final in adapter-web because this plan establishes a Console UI utility, not a public DTO/API contract.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Used the actual RunEventDto timestamp accessor**
- **Found during:** Task 1 (GREEN implementation)
- **Issue:** Initial implementation referenced `occurredAt()`, but the client DTO exposes `timestamp()`.
- **Fix:** Updated `RuntimeEventCard` to use `event.timestamp()`.
- **Files modified:** `RuntimeEventCard.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleRuntimeCardsTest test`
- **Committed in:** `a7c3452`

**2. [Rule 1 - Bug] Bounded advanced diagnostics enough to exclude full long raw strings**
- **Found during:** Task 1 (GREEN implementation)
- **Issue:** The first advanced detail bound was longer than the test's long URL/string, allowing the full 180-character string to appear.
- **Fix:** Reduced default bounded diagnostic length to 160 characters while preserving redaction.
- **Files modified:** `RuntimeDetailRedactor.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleRuntimeCardsTest test`
- **Committed in:** `a7c3452`

---

**Total deviations:** 2 auto-fixed (2 Rule 1 bugs)
**Impact on plan:** Both fixes were required for DTO correctness and the planned bounded/redacted detail contract. No scope expansion.

## Issues Encountered

- Existing repository had unrelated modified/untracked files before this executor started. Only Phase 13 Plan 01 files were staged and committed.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None. Stub-pattern scan only found null/blank guard clauses and a null-safe payload fallback in the created runtime card/redactor code; these do not feed placeholder UI or mock data.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleRuntimeCardsTest,WebConsoleMobileFlowContractTest test` — passed, 32 tests.
- `grep -R "new Dialog\|new ConfirmDialog\|Notification.show\|new MenuBar\|new ContextMenu" pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console` — no matches.

## Next Phase Readiness

- Downstream Phase 13 plans can reuse `RuntimeDetailRedactor` for tool-card and approval-card detail hardening.
- Runtime cards are already delivered through the existing Chat/Event Feed component seam, so later timeline/tool/approval UX work can remain route-local and mobile-first.

## Self-Check: PASSED

- Created files verified conceptually in task commits: `RuntimeDetailRedactor.java`, `RuntimeEventCard.java`, `WebConsoleRuntimeCardsTest.java`.
- Modified file verified conceptually in task commits: `RunEventRenderer.java`.
- Commits recorded: `0a207d4`, `a7c3452`, `0a373fe`, `165d67f`.

---
*Phase: 13-runtime-cards-timeline-tool-and-approval-ux*
*Completed: 2026-06-24*
