---
phase: 08-controlled-dynamic-plugin-jars
plan: 01
subsystem: infra
tags: [pf4j, plugins, extension-api, controlled-directory, governance]

requires:
  - phase: 06-java-extension-surface-spi-and-spring
    provides: Framework-free extension API, lifecycle vocabulary, compatibility model, and infrastructure extension patterns.
  - phase: 07-mcp-client-bridge-and-governed-remote-tools
    provides: Source/capability governance patterns for isolated external integrations.
provides:
  - Isolated `pi-agent-infrastructure-plugin` Maven module with module-local PF4J dependency.
  - Typed controlled-directory plugin registry properties for configuration-file-first dynamic plugin loading.
  - Redacted plugin descriptor, compatibility, and lifecycle summary records for later Admin/governance surfaces.
affects: [phase-08-plugin-loading, admin-governance, extension-integration, architecture-gates]

tech-stack:
  added: [org.pf4j:pf4j 3.12.0]
  patterns:
    - Keep PF4J isolated to plugin infrastructure; root POM only manages the version.
    - Represent plugin configuration and summaries as plain Java contracts before Spring/Admin binding.

key-files:
  created:
    - pi-agent-infrastructure-plugin/pom.xml
    - pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/PluginRegistryProperties.java
    - pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/PluginCompatibilitySummary.java
    - pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/PluginDescriptorSummary.java
    - pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/PluginLifecycleSummary.java
    - pi-agent-infrastructure-plugin/src/test/java/io/github/pi_java/agent/infrastructure/plugin/PluginRegistryPropertiesTest.java
    - pi-agent-infrastructure-plugin/src/test/java/io/github/pi_java/agent/infrastructure/plugin/PluginDescriptorSummaryTest.java
  modified:
    - pom.xml

key-decisions:
  - "Keep PF4J managed in the root POM but declared only in `pi-agent-infrastructure-plugin` so Domain/App/API/starter/MCP/provider modules remain PF4J-free."
  - "Model plugin registry configuration as plain Java records with Optional<Path> and explicit validation before any Spring `@ConfigurationProperties` binding."
  - "Expose only redacted, controlled-directory-relative or filename-only plugin source summaries; raw absolute paths and raw plugin metadata stay out of public summaries."

patterns-established:
  - "Plugin infrastructure isolation: dynamic plugin concerns live in `pi-agent-infrastructure-plugin`, not core Runtime, App, Extension API, MCP, starter, or Adapter Web."
  - "Controlled-directory safety contract: enabling plugin loading requires a directory and explicit non-sandbox warning acknowledgement."
  - "Safe plugin summaries: descriptor/lifecycle surfaces carry compatibility, lifecycle, path summaries, and redacted errors only."

requirements-completed: [PLUG-01, PLUG-02, PLUG-03, PLUG-06]

duration: 5m 54s
completed: 2026-06-16
---

# Phase 08 Plan 01: Isolated PF4J Plugin Infrastructure Foundation Summary

**Controlled PF4J plugin infrastructure module with typed directory configuration and redacted plugin descriptor/lifecycle summaries.**

## Performance

- **Duration:** 5m 54s
- **Started:** 2026-06-16T17:12:32Z
- **Completed:** 2026-06-16T17:18:26Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments

- Added `pi-agent-infrastructure-plugin` to the Maven reactor after extension infrastructure and before MCP/Adapter Web surfaces.
- Added PF4J dependency management at the root while keeping the actual `org.pf4j:pf4j` declaration local to the plugin infrastructure module.
- Defined typed plugin registry properties covering enabled flag, controlled plugin directory, startup/manual refresh flags, allowlist/selection, platform API version, duplicate override controls, and explicit non-sandbox warning acknowledgement.
- Added safe descriptor, compatibility, and lifecycle summary records that redact raw plugin metadata, avoid absolute path leakage, preserve compatibility status, and sanitize lifecycle errors.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add isolated plugin infrastructure Maven module** - `a42161a` (chore)
2. **Task 2 RED: Add failing plugin configuration contracts** - `6e84641` (test)
3. **Task 2 GREEN: Define controlled plugin registry contracts** - `f67ee50` (feat)

**Plan metadata:** pending final docs commit

_Note: Task 2 was TDD and therefore has separate failing-test and implementation commits._

## Files Created/Modified

- `pom.xml` - Adds `pi-agent-infrastructure-plugin` to the reactor and manages `pf4j.version` plus `org.pf4j:pf4j`.
- `pi-agent-infrastructure-plugin/pom.xml` - Defines the isolated plugin infrastructure module with PF4J and required project/test dependencies only.
- `pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/PluginRegistryProperties.java` - Typed controlled-directory plugin registry settings and deterministic validation.
- `pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/PluginCompatibilitySummary.java` - Public compatibility summary for declared plugin API range vs platform API version.
- `pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/PluginDescriptorSummary.java` - Redacted plugin descriptor summary with controlled-directory-relative or filename-only source path summary.
- `pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/PluginLifecycleSummary.java` - Lifecycle summary using extension lifecycle states, non-sandbox warning metadata, and sanitized errors.
- `pi-agent-infrastructure-plugin/src/test/java/io/github/pi_java/agent/infrastructure/plugin/PluginRegistryPropertiesTest.java` - Tests for safe defaults, required directory validation, allowlist/selection validation, and safety warning controls.
- `pi-agent-infrastructure-plugin/src/test/java/io/github/pi_java/agent/infrastructure/plugin/PluginDescriptorSummaryTest.java` - Tests for path redaction, filename fallback, compatibility, lifecycle, and error sanitization.

## Decisions Made

- Keep PF4J managed in root dependencyManagement for version consistency, but declare the dependency only in `pi-agent-infrastructure-plugin` to preserve COLA and extension API isolation.
- Use plain Java records/classes in the infrastructure plugin module; Spring binding and Admin upload/install workflows remain deferred to later Phase 8 plans.
- Require `nonSandboxWarningAcknowledged` when controlled plugin loading is enabled because PF4J class loading is not a sandbox boundary.
- Summarize plugin source paths relative to the configured controlled directory when possible, and fall back to filename-only when a path is outside that directory.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- The environment does not have `rg` installed, so the plan-level PF4J isolation check was performed with the repository Grep tool instead. Results showed PF4J only in `pom.xml` and `pi-agent-infrastructure-plugin/pom.xml` for XML declarations; Java mentions are existing architecture-test forbidden-package strings outside this plan and no production Java imports PF4J yet.
- Existing unrelated uncommitted planning files were present before execution. They were left untouched and excluded from task commits.

## Known Stubs

None. The `""` fallback in `PluginLifecycleSummary` represents intentional “no error” state, and the `"<unknown>"`/`"<redacted-path>"` strings are safety redaction fallbacks rather than UI/data stubs.

## Validation

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-plugin -am -DskipTests compile`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-plugin -am -Dtest=PluginRegistryPropertiesTest,PluginDescriptorSummaryTest test`
- PF4J isolation checked with Grep for `org\.pf4j|pf4j` in XML/Java files.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Later Phase 8 plans can add controlled PF4J discovery/loading against this isolated module without leaking PF4J into core Runtime, Extension API, MCP, or Admin modules.
- Adapter Web can later bind these plain Java properties as Spring configuration while preserving configuration-file-first semantics.
- Governance/Admin surfaces can consume redacted descriptor and lifecycle summaries without exposing absolute paths, raw plugin metadata, or unsafe error content.

## Self-Check: PASSED

- Verified expected files exist: root POM, plugin module POM, plugin registry properties, descriptor summary, lifecycle summary, and this SUMMARY file.
- Verified task commits exist in git history: `a42161a`, `6e84641`, and `f67ee50`.

---
*Phase: 08-controlled-dynamic-plugin-jars*
*Completed: 2026-06-16*
