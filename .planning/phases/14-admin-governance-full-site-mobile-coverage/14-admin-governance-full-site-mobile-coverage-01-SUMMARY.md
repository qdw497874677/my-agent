---
phase: 14-admin-governance-full-site-mobile-coverage
plan: 01
subsystem: ui
tags: [vaadin, mobile, admin-governance, cards, redaction, css]

requires:
  - phase: 13-runtime-cards-timeline-tool-and-approval-ux
    provides: Runtime card/detail/redaction and mobile-safe details patterns reused for Admin governance.
provides:
  - Shared package-local Admin mobile card/detail helper and conservative redactor.
  - Governance Overview stacked status cards for runtime, providers, tools, extensions, MCP, and plugins.
  - Reusable pi-mobile Admin card/detail/field/action-row/status-severity CSS contract.
affects: [phase-14-admin-registry, phase-14-operations, phase-14-policy-audit, phase-14-admin-e2e]

tech-stack:
  added: []
  patterns:
    - Package-local Vaadin Admin card helpers backed by PiPageSection.
    - Collapsed Details for structured/redacted Admin metadata.
    - Stable data-* selector contracts for Java and Playwright verification.

key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobileCardSupport.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobileRedactor.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminGovernanceOverviewView.java
    - pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceViewsTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebMobileBaselineContractTest.java

key-decisions:
  - "Keep Admin mobile primitives package-local inside adapter-web/admin so Vaadin/mobile concerns do not leak into App/Domain/client DTO contracts."
  - "Render Governance Overview summaries as label/value card fields plus status chips while preserving renderedText() semantic compatibility for existing tests."
  - "Treat metadata as structured collapsed Details and redact sensitive keys and values before rendering."

patterns-established:
  - "AdminMobileCardSupport.statusCard creates PiPageSection-based .pi-admin-card surfaces with data-admin-card, data-admin-section, data-admin-field, data-status-chip, and data-admin-details hooks."
  - "AdminMobileRedactor conservatively redacts sk-, rawSecret, apiKey/api_key, password, bearer, authorization, token=, access_token, and refresh_token patterns."
  - "pi-mobile Admin card CSS applies width/max-width/min-width wrapping and status severity chips for normal/warning/abnormal states."

requirements-completed: [MADM-01]

duration: 9min
completed: 2026-06-25
---

# Phase 14 Plan 01: Shared Admin Mobile Cards and Governance Overview Summary

**Governance Overview now renders mobile-readable Admin status cards with collapsed redacted metadata and a reusable card/detail CSS contract for later Phase 14 views.**

## Performance

- **Duration:** 9 min
- **Started:** 2026-06-25T04:28:25Z
- **Completed:** 2026-06-25T04:37:25Z
- **Tasks:** 3
- **Files modified:** 6

## Accomplishments

- Added package-local `AdminMobileCardSupport` helpers for Admin pages, status/metric cards, label-value rows, status chips, action rows, collapsed Details, and redacted detail blocks.
- Added `AdminMobileRedactor` with conservative Admin secret matching so metadata/details do not expose raw keys, bearer tokens, raw secrets, or token-like values.
- Converted `AdminGovernanceOverviewView` from dense visible pipe-separated status text to six stacked cards for runtime, providers, tool registry, extensions, MCP, and plugins.
- Added route continuity links to registry, operations, policy decisions, and audits inside Overview card action rows.
- Extended the `pi-mobile` theme with reusable Admin card/detail/field/action-row/status-severity selectors and contract tests.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add shared Admin mobile card and redaction helper contracts** - `c9d20ae` (feat)
2. **Task 2: Convert Governance Overview to stacked status cards** - `dc85a25` (feat)
3. **Task 3: Add Admin mobile card/detail CSS contract** - `eeaf758` (feat)

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobileCardSupport.java` - Package-local Vaadin helper for Admin mobile pages, cards, label/value rows, chips, links, Details, and metadata rendering.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminMobileRedactor.java` - Package-local conservative string/metadata redaction helper for Admin dense details.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminGovernanceOverviewView.java` - Overview now renders stacked card summaries with stable selectors and collapsed metadata Details.
- `pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css` - Added Admin card/detail/field/action-row/status-severity mobile wrapping and tap-target contract.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/AdminGovernanceViewsTest.java` - Added redactor/helper/Overview selector, redaction, card count, and route link coverage.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebMobileBaselineContractTest.java` - Added CSS contract assertions for Admin mobile card/detail selectors.

## Decisions Made

- Kept Admin mobile helpers package-private in `io.github.pi_java.agent.adapter.web.ui.admin` to avoid creating a public API or leaking Vaadin/mobile concerns beyond adapter-web.
- Preserved `renderedText()` semantic pipe-containing strings for legacy tests while removing pipe-separated visible Overview summaries from card rendering.
- Redacted sensitive metadata both by value pattern and sensitive key labels, so values such as `apiKey=abc123` and `rawSecret=abc123` do not appear in expanded details.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Redacted metadata values when labels are sensitive**
- **Found during:** Task 2 (Convert Governance Overview to stacked status cards)
- **Issue:** Value-only redaction would not hide a plain value when the metadata key was sensitive, e.g. `apiKey=abc123`.
- **Fix:** `AdminMobileCardSupport.labelValue` now renders `[REDACTED]` whenever the label/key contains API/password/secret/token/authorization terms.
- **Files modified:** `AdminMobileCardSupport.java`, `AdminGovernanceViewsTest.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest test`
- **Committed in:** `dc85a25`

---

**Total deviations:** 1 auto-fixed (1 missing critical)
**Impact on plan:** The fix was necessary to satisfy the plan's security/redaction truth and does not expand scope.

## Issues Encountered

- `pi-agent-adapter-web/src/main/frontend` is ignored by an existing `.gitignore`, so the planned `styles.css` change was staged explicitly with `git add -f`.
- Pre-existing unrelated modified/untracked files were present in the working tree from other parallel work and were intentionally not staged.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebMobileBaselineContractTest,AdminGovernanceViewsTest test`
- Boundary check: no `io.github.pi_java.agent.domain`, `io.github.pi_java.agent.app`, or `com.vaadin.hilla` imports in `AdminMobile*.java`.

## Known Stubs

None.

## Self-Check: PASSED

- Found `14-admin-governance-full-site-mobile-coverage-01-SUMMARY.md`.
- Found created helper files `AdminMobileCardSupport.java` and `AdminMobileRedactor.java`.
- Verified task commits exist: `c9d20ae`, `dc85a25`, `eeaf758`.

## Next Phase Readiness

- Registry, MCP, Plugin, Extension, Operations, Policy, and Audit plans can reuse `AdminMobileCardSupport`, `AdminMobileRedactor`, and `.pi-admin-*` CSS without duplicating card/detail primitives.
- Overview selectors (`data-admin-overview-card`, `data-governance-area`, `data-admin-details`) are ready for Phase 14 Admin Playwright coverage.

---
*Phase: 14-admin-governance-full-site-mobile-coverage*
*Completed: 2026-06-25*
