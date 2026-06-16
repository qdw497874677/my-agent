---
phase: 06-java-extension-surface-spi-and-spring
verified: 2026-06-16T00:27:16Z
status: human_needed
score: 8/8 must-haves verified
human_verification:
  - test: "Open Admin Registry/Governance UI and inspect the Extension Governance section"
    expected: "Extension sources and capabilities are visible with source kind, health, compatibility, enabled state, redacted errors/metadata, and no enable/disable/delete mutation controls."
    why_human: "Visual layout/operability in the rendered Vaadin browser UI cannot be fully proven by grep and unit tests."
---

# Phase 6: Java Extension Surface: SPI and Spring Verification Report

**Phase Goal:** Java Extension Surface: SPI and Spring — provide framework-free Java extension contracts, ServiceLoader discovery, Spring Boot starter/annotations, read-only governance visibility, safety conformance tests, and downstream documentation for MCP/PF4J phases.
**Verified:** 2026-06-16T00:27:16Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

Phase 6 is implemented and the automated evidence supports the goal. The remaining item is human browser inspection of the Vaadin Admin UI presentation, not a code gap.

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Public framework-free extension API/JAR supports metadata, lifecycle, health, version compatibility, and all Phase 6 capability families. | ✓ VERIFIED | `pi-agent-extension-api/pom.xml` has only Domain/App compile dependencies; `ExtensionSource`, `ExtensionMetadata`, `ExtensionCompatibility`, `ExtensionLifecycleState`, `ExtensionHealth`, and `ExtensionCapability.Type` include TOOL, MODEL_PROVIDER, POLICY, EVENT_LISTENER, WORKSPACE_PROVIDER, MEMORY_PROVIDER. |
| 2 | Java ServiceLoader extensions are discovered and normalized as extension sources/capabilities. | ✓ VERIFIED | `ServiceLoaderExtensionDiscovery` calls `ServiceLoader.load(ExtensionSource.class, classLoader)`; `DefaultExtensionContributionRegistry` orders, compatibility-checks, disables, duplicate-checks, and exposes usable/governance entries. |
| 3 | Duplicate capability IDs fail fast by default, and disabled/incompatible capabilities remain governance-visible but unusable. | ✓ VERIFIED | `DefaultExtensionContributionRegistry.mergeDuplicates` throws unless overrides are enabled; `CapabilityEntry.usable()` requires enabled + compatible + `USABLE`; focused registry tests pass in final Maven gate. |
| 4 | Extension tools/providers normalize into existing registry contracts without source-specific execution APIs or raw provider SDK paths. | ✓ VERIFIED | `ExtensionToolRegistry` implements `ToolRegistry`, returns `ToolDescriptor` plus existing `ToolExecutorBinding`; `ExtensionModelProviderRegistry` implements `ModelProviderRegistry` and maps credential refs through Domain `CredentialRef`. |
| 5 | Spring Boot applications can include a starter and discover/register ServiceLoader, explicit Spring Bean, and limited annotation sources. | ✓ VERIFIED | Starter POM exists; `AutoConfiguration.imports` points to `PiAgentExtensionAutoConfiguration`; auto-config uses `@AutoConfiguration`, `@EnableConfigurationProperties`, `@ConditionalOnMissingBean`; `@PiTool`/`@PiEventListener` and annotation factories exist and are wired. |
| 6 | Cloud Server consumes the same starter path and exposes read-only extension governance through public APIs. | ✓ VERIFIED | `pi-agent-adapter-web/pom.xml` depends on `pi-agent-spring-boot-starter`; `GovernanceBeanConfiguration` injects `ExtensionGovernanceCatalog`; `AdminGovernanceController` exposes `GET /api/admin/governance/extensions`; `ExtensionGovernanceApiTest` covers GET-only semantics. |
| 7 | Conformance tests prove extensions cannot bypass ToolExecutionGateway, policy, audit, event, redaction, workspace, or credential boundaries. | ✓ VERIFIED | `ExtensionConformanceE2ETest` creates runs through REST, asserts `tool.proposed`, `tool.policy_decided`, `tool.started`, `tool.completed`/approval events, audit records, no fake secret leakage, no side effect before approval, and `CredentialRef` redaction. Final focused Maven gate passed. |
| 8 | Downstream MCP/PF4J phases have a contract document and requirement traceability remains scoped to Phase 6. | ✓ VERIFIED | `docs/phase-06-extension-surface.md` covers public SDK, ServiceLoader, Spring starter, annotations, governance, conformance commands, safety boundaries, Phase 7/8 deferrals; `.planning/REQUIREMENTS.md` marks only WORK-06 and EXT-01..EXT-05 complete while MCP/PLUG/OPS-01/E2E-08 remain pending. |

**Score:** 8/8 truths verified by automated/code review evidence.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `pi-agent-extension-api/pom.xml` | Framework-free public extension API module | ✓ VERIFIED | Reactor module exists; production deps are `pi-agent-domain` and `pi-agent-app`, with JUnit/AssertJ/ArchUnit test deps only. |
| `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ExtensionSource.java` | Extension-source entry contract | ✓ VERIFIED | Exposes `metadata()` and `capabilities()`; no framework imports. |
| `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ExtensionCapability.java` | Typed capability family | ✓ VERIFIED | Enum covers tool, model provider, policy, event listener, workspace provider, and memory provider. |
| `pi-agent-extension-api/src/test/java/io/github/pi_java/agent/extension/api/ExtensionApiArchitectureTest.java` | SDK architecture gate | ✓ VERIFIED | Included in final focused Maven gate. |
| `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/ExtensionGovernanceResponse.java` | Public extension governance DTO envelope | ✓ VERIFIED | Public client record consumed by App, REST, tests, and UI. |
| `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/extension/ExtensionGovernanceCatalog.java` | App read-only governance port | ✓ VERIFIED | App-owned port; used by `DefaultGovernanceQueryService` and starter adapter. |
| `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryService.java` | Governance overview/detail integration | ✓ VERIFIED | Constructor-injects `ExtensionGovernanceCatalog`; exposes `extensions(RequestContext)` and maps source/capability status to public DTOs. |
| `pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ServiceLoaderExtensionDiscovery.java` | ServiceLoader discovery | ✓ VERIFIED | Uses `ServiceLoader.load(ExtensionSource.class, classLoader)` and supports explicit merged sources. |
| `pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ExtensionToolRegistry.java` | ToolRegistry adapter over extension tools | ✓ VERIFIED | Lists and resolves only usable `ToolExtensionCapability` entries through existing `ToolRegistry` shape. |
| `pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ExtensionGovernanceCatalogAdapter.java` | App governance catalog adapter | ✓ VERIFIED | Maps all visible source/capability entries to App governance statuses. |
| `pi-agent-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | Boot auto-configuration registration | ✓ VERIFIED | Contains `io.github.pi_java.agent.spring.autoconfigure.PiAgentExtensionAutoConfiguration`. |
| `pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/autoconfigure/PiAgentExtensionAutoConfiguration.java` | Starter auto-configuration | ✓ VERIFIED | Configures discovery, contribution registry, extension Tool/Model registries, governance catalog, and annotation factories. |
| `pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/annotation/PiTool.java` | Lightweight Spring tool annotation | ✓ VERIFIED | Found and used by `AnnotatedToolExtensionSourceFactory` tests. |
| `pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/annotation/PiEventListener.java` | Lightweight Spring event listener annotation | ✓ VERIFIED | Found and used by listener factory/tests. |
| `pi-agent-spring-boot-starter/src/test/java/io/github/pi_java/agent/spring/autoconfigure/AnnotatedSpringExtensionTest.java` | Annotation registration proof | ✓ VERIFIED | Included in final focused Maven gate. |
| `pi-agent-adapter-web/pom.xml` | Cloud Server starter dependency | ✓ VERIFIED | Declares `pi-agent-spring-boot-starter`. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AdminGovernanceController.java` | Extension governance REST endpoint | ✓ VERIFIED | `@GetMapping("/extensions")` delegates to `governanceQueryService.extensions(...)`. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminRegistryStatusView.java` | Read-only extension governance UI section | ✓ VERIFIED + HUMAN VISUAL | Renders extension sources/capabilities with read-only attributes and `mutationControlsPresent()` false; browser visual inspection still recommended. |
| `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ExtensionConformanceE2ETest.java` | Product-path extension conformance E2E | ✓ VERIFIED | Included in final focused Maven gate; asserts gateway, policy, event, audit, redaction, credential boundaries. |
| `pi-agent-infrastructure-extension/src/test/java/io/github/pi_java/agent/infrastructure/extension/ServiceLoaderConformanceTest.java` | SPI conformance tests | ✓ VERIFIED | Included in final focused Maven gate. |
| `pi-agent-spring-boot-starter/src/test/java/io/github/pi_java/agent/spring/autoconfigure/SpringExtensionConformanceTest.java` | Spring conformance tests | ✓ VERIFIED | Included in final focused Maven gate. |
| `docs/phase-06-extension-surface.md` | Downstream extension contract index | ✓ VERIFIED | Includes requirement coverage, public SDK, usage, conformance commands, safety boundaries, and Phase 7/8 deferrals. |
| `.planning/REQUIREMENTS.md` | Requirement validation evidence | ✓ VERIFIED | WORK-06 and EXT-01..EXT-05 complete with Phase 6 evidence; MCP/PLUG/OPS-01/E2E-08 remain pending. |
| `pi-agent-spring-boot-starter/src/test/java/io/github/pi_java/agent/spring/autoconfigure/ExtensionStarterArchitectureTest.java` | Starter boundary architecture gate | ✓ VERIFIED | File exists and final architecture gate command passed. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `pi-agent-extension-api` | Domain/App contracts | Maven dependencies | ✓ WIRED | API POM compile-depends on `pi-agent-domain` and `pi-agent-app` only; no Spring/PF4J/MCP/Adapter production deps found. |
| `DefaultGovernanceQueryService` | `ExtensionGovernanceCatalog` | Constructor injection | ✓ WIRED | `DefaultGovernanceQueryService` has `ExtensionGovernanceCatalog` field/constructor dependency and maps catalog sources to DTOs. |
| `ServiceLoaderExtensionDiscovery` | `ExtensionSource` | `ServiceLoader.load(ExtensionSource.class, classLoader)` | ✓ WIRED | Direct ServiceLoader call found. |
| `ExtensionToolRegistry` | `ToolRegistry` | implements `ToolRegistry`, returns descriptor + binding | ✓ WIRED | `listTools()` and `resolve()` adapt usable `ToolExtensionCapability` entries to `ToolRegistry.ToolResolution`. |
| `PiAgentExtensionAutoConfiguration` | `DefaultExtensionContributionRegistry` | Auto-configured bean | ✓ WIRED | `extensionContributionRegistry(...)` bean builds registry from ServiceLoader + Spring/annotation sources and properties. |
| `AnnotatedToolExtensionSourceFactory` | `ToolRegistry` path | `ToolExtensionCapability` descriptor plus binding | ✓ WIRED | Annotated methods become `ToolExtensionCapability`; starter merges them into contribution registry, then extension registry exposes them through `ToolRegistry`. |
| `GovernanceBeanConfiguration` | `ExtensionGovernanceCatalog` | Starter-provided bean injection | ✓ WIRED | Adapter Web governance config receives `ExtensionGovernanceCatalog`; adapter-local empty fallback removed in product path. |
| `AdminGovernanceController` | `GovernanceQueryService.extensions` | GET endpoint | ✓ WIRED | `GET /api/admin/governance/extensions` delegates directly to service method. |
| `docs/phase-06-extension-surface.md` | Phase 7/8 planning | Explicit MCP/PF4J deferrals and reusable governance language | ✓ WIRED | Doc references Phase 7 MCP and Phase 8 PF4J/dynamic plugin reuse/deferrals. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `AdminGovernanceController` | `ExtensionGovernanceResponse` | `GovernanceQueryService.extensions(RequestContext)` | Yes — maps catalog sources | ✓ FLOWING |
| `DefaultGovernanceQueryService` | extension source/capability DTOs | `ExtensionGovernanceCatalog.sources()` | Yes — concrete starter catalog or App test fixture | ✓ FLOWING |
| `PiAgentExtensionAutoConfiguration` | contribution registry | `ServiceLoaderExtensionDiscovery.discover(...)` + Spring `ExtensionSource`/annotation sources | Yes — registry built from actual source lists and properties | ✓ FLOWING |
| `ServiceLoaderExtensionDiscovery` | discovered source list | `ServiceLoader.load(ExtensionSource.class, classLoader)` + explicit sources | Yes — discovery results include source objects or failed statuses | ✓ FLOWING |
| `ExtensionToolRegistry` | tool descriptors/bindings | `DefaultExtensionContributionRegistry.usableCapabilities()` | Yes — only usable tool capabilities are resolved to bindings | ✓ FLOWING |
| `ExtensionModelProviderRegistry` | provider descriptors | usable `ModelProviderExtensionCapability` entries | Yes — maps provider/model descriptors and credential refs | ✓ FLOWING |
| `AdminRegistryStatusView` | rendered extension lines | `ConsoleHttpClient.adminExtensionGovernancePath()` and `showExtensions(ExtensionGovernanceResponse)` | Yes in unit/component tests; browser presentation needs human check | ✓ FLOWING / HUMAN VISUAL |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Final Phase 6 focused no-key gate covers SDK, SPI, starter, Adapter Web governance, and conformance tests | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-extension-api,pi-agent-infrastructure-extension,pi-agent-spring-boot-starter,pi-agent-adapter-web -am -Dtest=ExtensionApiContractTest,ExtensionApiArchitectureTest,ServiceLoaderExtensionDiscoveryTest,ExtensionContributionRegistryTest,PiAgentExtensionAutoConfigurationTest,AnnotatedSpringExtensionTest,ServiceLoaderConformanceTest,SpringExtensionConformanceTest,ExtensionGovernanceApiTest,ExtensionConformanceE2ETest test` | Passed. Output contained expected SLF4J/Vaadin/Mockito warnings and GET-only endpoint method-not-supported warnings in negative tests, with no Maven failure. | ✓ PASS |
| Documentation contract contains required anchors | Reviewed `docs/phase-06-extension-surface.md` and grep evidence for `EXT-01`, `ToolExecutionGateway`, `Phase 7`, `Phase 8`, `Deferrals` | Required downstream anchors present. | ✓ PASS |
| Public SDK remains framework-free | Reviewed `pi-agent-extension-api/pom.xml`; grep for Spring/Vaadin/PF4J/MCP/JDBC/provider SDK deps found none | Compile dependencies only Domain/App. | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| EXT-01 | 06-01, 06-03, 06-07, 06-08 | Developer can extend through Java SPI for tools, model providers, policies, event sinks/listeners, memory providers, workspace providers, and extension metadata. | ✓ SATISFIED | `ExtensionSource`, capability records, `ServiceLoaderExtensionDiscovery`, `DefaultExtensionContributionRegistry`, `ServiceLoaderConformanceTest`, docs. |
| EXT-02 | 06-04, 06-05, 06-06, 06-07, 06-08 | Spring Boot apps can register tools/providers/policies/listeners through Spring Beans or annotations without runtime core changes. | ✓ SATISFIED | `pi-agent-spring-boot-starter`, `PiAgentExtensionAutoConfiguration`, explicit `ExtensionSource` bean merge, `@PiTool`, `@PiEventListener`, Spring conformance tests. |
| EXT-03 | 06-01, 06-02, 06-03, 06-04, 06-08 | Public extension API/JAR with compatibility/version metadata, lifecycle, health status, and conformance tests. | ✓ SATISFIED | `ExtensionMetadata`, `ExtensionCompatibility`, `ExtensionLifecycleState`, `ExtensionHealth`, architecture/conformance tests, Phase 6 docs. |
| EXT-04 | 06-02, 06-06, 06-08 | Admin can view extension sources, capabilities, health, compatibility, enable/disable status, and errors. | ✓ SATISFIED + HUMAN VISUAL | Public DTOs, `ExtensionGovernanceCatalog`, `GET /api/admin/governance/extensions`, `AdminRegistryStatusView`, `ExtensionGovernanceApiTest`; browser visual inspection recommended. |
| EXT-05 | 06-03, 06-04, 06-05, 06-06, 06-07, 06-08 | Extension loading never bypasses ToolExecutionGateway, Policy, Audit, Event, and CredentialRef boundaries. | ✓ SATISFIED | `ExtensionToolRegistry`, product-path `ExtensionConformanceE2ETest`, audit/event/redaction assertions, `ExtensionModelProviderRegistry` credential reference mapping. |
| WORK-06 | 06-01, 06-03, 06-04, 06-07, 06-08 | Workspace/resource providers can be extended via SPI/Spring/plugins/MCP-backed adapters without bypassing ToolExecutionGateway. | ✓ SATISFIED FOR PHASE 6 SCOPE | `WorkspaceProviderExtensionCapability`, SPI/Spring conformance, extension workspace tool approval-gate E2E; REQUIREMENTS.md correctly states MCP/plugin adapters remain Phase 7/8 pending. |

**Orphaned requirement check:** All user-specified Phase 6 IDs (EXT-01, EXT-02, EXT-03, EXT-04, EXT-05, WORK-06) appear in plan frontmatter and in `.planning/REQUIREMENTS.md`. No additional Phase 6 requirement IDs found unclaimed. MCP/PLUG/OPS-01/E2E-08 remain pending, which matches the Phase 6 deferral boundary.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/MemoryProviderExtensionCapability.java` / `docs/phase-06-extension-surface.md` | doc line 43 | Metadata-only memory provider placeholder | ℹ️ Info | Intentional Phase 6 scope; full Memory/RAG wiring deferred. Not a blocker. |
| `pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ExtensionModelProviderRegistry.java` | 53 | `return null` for absent credential ref | ℹ️ Info | Safe optional-field behavior when metadata has no credential ref; not a stub and does not flow to user-visible fake data. |
| `pi-agent-adapter-web` Admin MCP/plugin panels | existing Phase 5/6 UI | MCP/plugin placeholders remain | ℹ️ Info | Intentional Phase 7/8 deferral; Phase 6 extension panel uses real governance data. |

No blocker TODO/FIXME/placeholder or hollow data-flow pattern was found in Phase 6 extension-surface code. Null/default matches reviewed were validation/default handling rather than stubs.

### Human Verification Required

### 1. Vaadin Admin Extension Governance visual/read-only inspection

**Test:** Start the Cloud Server test/dev profile, open the Admin Registry/Governance view, and inspect the Extension Governance section.
**Expected:** Extension sources and capability rows render with source kind, status/lifecycle, health, compatibility, enabled state, redacted errors/metadata, and no enable/disable/delete controls.
**Why human:** Automated tests verify DTO path anchors, rendered text methods, and REST data. They do not fully prove browser layout, visual hierarchy, or interactive absence of all UI affordances in a real browser.

### Gaps Summary

No implementation gaps were found. The phase goal is achieved by code and automated tests. Human verification is requested only for the rendered Vaadin Admin UI presentation.

---

_Verified: 2026-06-16T00:27:16Z_
_Verifier: the agent (gsd-verifier)_
