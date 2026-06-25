---
phase: 14-admin-governance-full-site-mobile-coverage
plan: 02
subsystem: ui
tags: [vaadin, admin-governance, mobile-cards, mcp, plugins, extensions]

requires:
  - phase: 14-admin-governance-full-site-mobile-coverage-01
    provides: Shared Admin mobile card/detail primitives and redaction helpers
provides:
  - Sectioned Registry overview cards with stable `data-admin-registry-section` selectors
  - Extension source and capability mobile cards with collapsed redacted details
  - MCP server/tool mobile cards sorted abnormal-first with refresh action boundaries preserved
  - Plugin lifecycle/capability mobile cards with warning and existing disable/quarantine actions only
affects: [phase-14-admin-mobile-e2e, phase-15-release-hardening, admin-governance]

tech-stack:
  added: []
  patterns: [Vaadin PiPageSection cards, AdminMobileCardSupport label rows/chips/details, abnormal-first governance sorting]

key-files:
  created:
    - .planning/phases/14-admin-governance-full-site-mobile-coverage/14-admin-governance-full-site-mobile-coverage-02-SUMMARY.md
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpAdminGovernanceViewTest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPluginGovernanceViewTest.java

key-decisions:
  - "Keep Registry/MCP/Plugin/Extension mobile conversion inside adapter-web Vaadin components with no public Admin DTO or route changes."
  - "Prioritize operator-relevant MCP and Plugin entries by abnormal status/error/reason before normal cards."
  - "Preserve renderedText() semantic compatibility while moving visible summaries to label rows, chips, cards, and collapsed Details."

patterns-established:
  - "Registry section cards: each overview status receives `data-admin-registry-section` grouped as registry/extensions/mcp/plugins."
  - "Nested governance detail cards: extension capabilities, MCP tools, and plugin capabilities render as child cards rather than pipe-separated visible rows."
  - "Abnormal-first sort: MCP/plugin entries with UNHEALTHY, FAILED, DOWN, WARN, DISCONNECTED, DISABLED, QUARANTINED, INCOMPATIBLE, error, or reason sort ahead of normal entries."

requirements-completed: [MADM-02, MADM-03, MADM-04, MADM-05]

duration: 16m36s
completed: 2026-06-25
---

# Phase 14 Plan 02: Registry, MCP, Plugin, and Extension Mobile Governance Summary

**Admin Registry governance now renders provider/tool/extension/MCP/plugin data as mobile-readable cards, nested details, status chips, and abnormal-first MCP/plugin sections without changing public Admin API anchors.**

## Performance

- **Duration:** 16m36s
- **Started:** 2026-06-25T04:42:30Z
- **Completed:** 2026-06-25T04:59:06Z
- **Tasks:** 2
- **Files modified:** 3 implementation/test files plus this summary

## Accomplishments

- Converted Registry overview status rendering from dense visible text rows to sectioned mobile status cards with `data-admin-registry-section` selectors for registry, extensions, MCP, and plugins.
- Converted Extension governance into `data-extension-source-card` source cards and nested `data-extension-capability-card` cards with collapsed redacted details.
- Converted MCP governance into refresh action rows plus abnormal-first `data-mcp-server-card` cards and nested `data-mcp-tool-card` tool cards.
- Converted Plugin governance into the existing non-sandbox warning, refresh/disable/quarantine actions only, abnormal-first `data-plugin-card` cards, and nested `data-plugin-capability-card` cards.
- Preserved existing ConsoleHttpClient path helpers, public Admin DTO boundaries, and `renderedText()` semantic assertions.

## Task Commits

Each task was committed atomically:

1. **Task 1: Convert registry overview and extension sections to mobile cards** - `0231a94` (feat)
2. **Task 2: Convert MCP and Plugin governance to abnormal-first mobile cards** - `5780465` (feat)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java` - Refactored Registry, Extension, MCP, and Plugin rendering to mobile cards/details while preserving path/action helpers.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpAdminGovernanceViewTest.java` - Added MCP server/tool card selector assertions and abnormal-first ordering coverage.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPluginGovernanceViewTest.java` - Added plugin/capability card selector assertions, action selector coverage, and warning control checks.
- `.planning/phases/14-admin-governance-full-site-mobile-coverage/14-admin-governance-full-site-mobile-coverage-02-SUMMARY.md` - Execution summary and verification record.

## Decisions Made

- Kept all UI conversion inside `pi-agent-adapter-web` and did not modify `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin` DTOs.
- Used nested card components for MCP tools, plugin capabilities, and extension capabilities so mobile inspection does not depend on desktop tables or pipe-separated rows.
- Used status/error/reason-based ranking to surface abnormal MCP/plugin rows before normal entries while preserving the existing DTO order semantics in `renderedText()` assertions where tests rely on them.

## Deviations from Plan

None - plan executed as written. No public Admin DTO files changed.

## Issues Encountered

- Initial implementation attempted to render source metadata for `ExtensionSourceDto`, but that DTO has no metadata accessor. Resolved by keeping source diagnostics to redacted error and capability count, while capability metadata remains behind collapsed Details.
- Parallel execution caused interleaved commit history from other phase 14 agents; task commits for this plan remain atomic and identified above.

## Known Stubs

None. The `null`/empty checks in `AdminRegistryStatusView` are defensive display fallbacks for optional timestamps/metadata and do not represent unimplemented UI data sources.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=McpAdminGovernanceViewTest,AdminPluginGovernanceViewTest test` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=AdminGovernanceViewsTest,McpAdminGovernanceViewTest,AdminPluginGovernanceViewTest test` — passed, 16 tests.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 14 browser/mobile E2E can assert Registry, MCP, Plugin, and Extension card/detail selectors directly.
- Operations, policy/audit, approvals, and full-route mobile browser coverage can build on the same `AdminMobileCardSupport` and selector patterns.

## Self-Check: PASSED

- FOUND: `.planning/phases/14-admin-governance-full-site-mobile-coverage/14-admin-governance-full-site-mobile-coverage-02-SUMMARY.md`
- FOUND: `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java`
- FOUND: `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/McpAdminGovernanceViewTest.java`
- FOUND: `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminPluginGovernanceViewTest.java`
- FOUND: task commit `0231a94`
- FOUND: task commit `5780465`

---
*Phase: 14-admin-governance-full-site-mobile-coverage*
*Completed: 2026-06-25*
