---
phase: 08-controlled-dynamic-plugin-jars
plan: 07
subsystem: plugins-e2e
tags: [pf4j, sample-plugin, e2e, governed-tools, compatibility, disablement]

requires:
  - phase: 08-controlled-dynamic-plugin-jars
    provides: Plugin governed tool E2E and redaction coverage from Plan 08-05.
  - phase: 08-controlled-dynamic-plugin-jars
    provides: Plugin Admin governance UI and public REST controls from Plan 08-06.
provides:
  - In-reactor `pi-sample-plugin-readonly` PF4J sample plugin JAR with one safe read-only Pi tool capability.
  - Adapter Web controlled-directory plugin discovery backed by real PF4J DefaultPluginManager loading.
  - Product-path sample plugin E2E for Admin status, tool catalog registration, REST-created run invocation, audit, events, compatibility failure, disable, and quarantine.
affects: [phase-08, plugin-loading, admin-governance, tool-gateway, e2e]

tech-stack:
  added: [pi-sample-plugin-readonly]
  patterns:
    - Real sample plugin packaging remains a separate Maven module with no Spring/Vaadin dependency.
    - Adapter Web discovers controlled-directory PF4J plugins into the existing PluginGovernanceCatalogAdapter and ExtensionToolRegistry path.
    - Sample plugin E2E copies a built JAR into a temporary controlled plugin directory and requires no Docker, network, model key, or external service.

key-files:
  created:
    - pi-sample-plugin-readonly/pom.xml
    - pi-sample-plugin-readonly/src/main/java/io/github/pi_java/agent/sample/plugin/ReadonlySamplePlugin.java
    - pi-sample-plugin-readonly/src/main/java/io/github/pi_java/agent/sample/plugin/ReadonlySampleExtensionSource.java
    - pi-sample-plugin-readonly/src/main/resources/plugin.properties
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/SamplePluginJarE2ETest.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/SamplePluginJarCompatibilityE2ETest.java
  modified:
    - pom.xml
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/PluginGovernanceBeanConfiguration.java
    - pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/Pf4jPluginExtensionBridge.java

key-decisions:
  - "Package the deterministic sample plugin as a separate in-reactor Maven module so tests validate a real PF4J plugin JAR rather than in-memory fake plugin sources only."
  - "Wire Adapter Web plugin discovery through PF4J DefaultPluginManager only when controlled plugin loading is enabled and a plugin directory is configured."
  - "Preserve ToolExtensionCapability instances during PF4J metadata enrichment so plugin tools keep their descriptor and executor binding through ExtensionToolRegistry."

requirements-completed: [PLUG-01, PLUG-02, PLUG-03, PLUG-05, E2E-08]

duration: 12m
completed: 2026-06-17
---

# Phase 08 Plan 07: Sample Plugin JAR Product-Path E2E Summary

**A real in-reactor PF4J sample plugin JAR now loads from a controlled directory and proves plugin tool registration, governed invocation, compatibility failure, disable, and quarantine behavior.**

## Performance

- **Duration:** 12m
- **Started:** 2026-06-17T16:34:00Z
- **Completed:** 2026-06-17T16:46:00Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments

- Added `pi-sample-plugin-readonly` to the Maven reactor with a PF4J `plugin.properties` descriptor and minimal plugin class.
- Implemented `ReadonlySampleExtensionSource` as a PF4J extension exposing one safe read-only `ToolExtensionCapability` (`plugin.sample.readonly.lookup`) with redacted plugin metadata.
- Wired Adapter Web plugin composition to discover real controlled-directory PF4J plugins via `DefaultPluginManager`, while retaining the disabled/no-directory empty registry fallback.
- Added product-path E2E that copies the sample JAR into a temporary controlled plugin directory, verifies Admin plugin status and tool catalog registration, and invokes the sample plugin tool through REST-created run execution, `ToolExecutionGateway`, audit, and `tool.lifecycle` events.
- Added compatibility/disable/quarantine regression tests using the real sample plugin JAR package to prove incompatible plugins remain visible but unusable and disabled/quarantined plugins stop new tool resolution.

## Task Commits

Each task was committed atomically:

1. **Task 1: Build sample read-only plugin JAR** - `3e0fc7d` (feat)
2. **Task 2: Add sample plugin JAR product-path E2E** - `af646c9` (feat)

## Files Created/Modified

- `pom.xml` - Adds `pi-sample-plugin-readonly` to the Maven reactor.
- `pi-sample-plugin-readonly/pom.xml` - Defines the sample plugin module with extension API and PF4J dependencies only.
- `pi-sample-plugin-readonly/src/main/java/io/github/pi_java/agent/sample/plugin/ReadonlySamplePlugin.java` - Minimal PF4J plugin class.
- `pi-sample-plugin-readonly/src/main/java/io/github/pi_java/agent/sample/plugin/ReadonlySampleExtensionSource.java` - Pi ExtensionSource with one read-only tool capability and deterministic binding.
- `pi-sample-plugin-readonly/src/main/resources/plugin.properties` - PF4J plugin descriptor metadata.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/PluginGovernanceBeanConfiguration.java` - Loads PF4J plugins from the configured controlled directory and exposes a primary plugin governance catalog.
- `pi-agent-infrastructure-plugin/src/main/java/io/github/pi_java/agent/infrastructure/plugin/Pf4jPluginExtensionBridge.java` - Handles optional PF4J descriptor metadata safely and preserves `ToolExtensionCapability` bindings during enrichment.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/SamplePluginJarE2ETest.java` - Cloud Server sample plugin load/catalog/run/audit/event E2E.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/SamplePluginJarCompatibilityE2ETest.java` - Real sample plugin JAR compatibility, disable, and quarantine tests.

## Decisions Made

- The sample plugin module intentionally avoids Spring and Vaadin; it depends only on the Pi extension API and PF4J.
- The E2E uses a temporary controlled directory copied from the built sample plugin target JAR so the test validates the same directory-based loading path operators will configure.
- Adapter Web performs startup discovery only for enabled configurations with a plugin directory. Disabled or absent directory configurations still return an empty plugin registry for safe startup.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Avoided adding an uncached explicit Maven jar plugin version**
- **Found during:** Task 1
- **Issue:** Adding an explicit `maven-jar-plugin:3.4.2` execution required network resolution in this environment and failed offline.
- **Fix:** Removed the explicit plugin version/configuration and used Maven's existing jar lifecycle. Verification runs `package` before E2E so the sample JAR exists.
- **Files modified:** `pi-sample-plugin-readonly/pom.xml`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-sample-plugin-readonly -am package`
- **Commit:** `3e0fc7d`

**2. [Rule 3 - Blocking] Made sample extension satisfy PF4J extension-point validation**
- **Found during:** Task 1
- **Issue:** PF4J annotation processing requires `@Extension` classes to implement `ExtensionPoint`.
- **Fix:** `ReadonlySampleExtensionSource` implements both Pi `ExtensionSource` and PF4J `ExtensionPoint`.
- **Files modified:** `ReadonlySampleExtensionSource.java`
- **Verification:** sample plugin package command passed.
- **Commit:** `3e0fc7d`

**3. [Rule 1 - Bug] Preserved tool capabilities during PF4J metadata enrichment**
- **Found during:** Task 2
- **Issue:** The PF4J bridge enriched capabilities through a generic wrapper, which lost the `ToolExtensionCapability` type needed by `ExtensionToolRegistry` to retain descriptor/binding execution.
- **Fix:** Preserve and re-create `ToolExtensionCapability` with enriched metadata while keeping descriptor and binding intact.
- **Files modified:** `Pf4jPluginExtensionBridge.java`
- **Verification:** sample plugin E2E command passed.
- **Commit:** `af646c9`

**4. [Rule 1 - Bug] Hardened optional PF4J descriptor metadata**
- **Found during:** Task 2
- **Issue:** Real PF4J descriptors can omit license metadata; the bridge used `Map.of(...)` with a nullable value.
- **Fix:** Added safe descriptor metadata construction that includes only non-blank optional fields.
- **Files modified:** `Pf4jPluginExtensionBridge.java`
- **Verification:** sample plugin E2E command passed.
- **Commit:** `af646c9`

**5. [Rule 3 - Blocking] Marked plugin governance catalog facade primary**
- **Found during:** Task 2
- **Issue:** `PluginGovernanceCatalogAdapter` implements `PluginGovernanceCatalog`, creating two candidate beans once real adapter wiring was active.
- **Fix:** Marked the public `PluginGovernanceCatalog` facade bean as `@Primary`.
- **Files modified:** `PluginGovernanceBeanConfiguration.java`
- **Verification:** sample plugin E2E command passed.
- **Commit:** `af646c9`

---

**Total deviations:** 5 auto-fixed (3 blocking, 2 bugs). **Impact:** All fixes were required to validate real JAR loading and preserve the intended governed tool path; no unsupported plugin CRUD or sandbox claims were added.

## Issues Encountered

- The first two delegated executor attempts for this plan returned empty results and produced no files, summaries, or commits. The orchestrator retried inline after user approval.
- Maven network access was unavailable for newly specified plugin versions, so the implementation avoided introducing uncached plugin dependencies.
- Pre-existing unrelated uncommitted Phase 02/03 planning artifacts and `bun.lock` remained untouched.

## Known Stubs

None. The sample plugin is intentionally narrow and read-only; deferred model/provider/policy/workspace/memory/event-listener plugin capabilities remain out of scope by plan.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-sample-plugin-readonly -am package` — passed
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-sample-plugin-readonly,pi-agent-adapter-web -am -Dtest=SamplePluginJarE2ETest,SamplePluginJarCompatibilityE2ETest test` — passed

## Auth Gates

None.

## User Setup Required

None - tests use a deterministic in-reactor sample plugin JAR copied to a temporary controlled directory and require no external network, Docker, model key, external service, or arbitrary host shell.

## Next Phase Readiness

- Plan 08-08 can close Phase 8 with architecture gates and documentation using real sample plugin evidence for PLUG-01/02/03/05 and E2E-08.
- Phase 9 can add production observability/policy/tenancy hardening while treating dynamic plugin loading as a configured controlled-directory capability rather than a sandbox.

## Self-Check: PASSED

- Verified expected files exist: sample plugin POM, plugin descriptor, extension source, E2E tests, and this SUMMARY file.
- Verified task commits exist in git history: `3e0fc7d` and `af646c9`.

---
*Phase: 08-controlled-dynamic-plugin-jars*
*Completed: 2026-06-17*
