# Phase 08 Controlled Dynamic Plugin JAR Contracts

Phase 08 validates `PLUG-01` through `PLUG-06` and completes the plugin portion of `E2E-08`. Dynamic plugins are trusted PF4J JARs loaded from an operator-controlled directory, bridged into the Phase 06 `ExtensionSource` model, normalized as ordinary Pi capabilities, and invoked only through existing governed runtime paths such as `ToolExecutionGateway`.

## Requirement Coverage

| Requirement | Phase 08 validation |
|-------------|---------------------|
| `PLUG-01` | `pi.plugins` typed configuration binds a controlled plugin directory, enablement flags, selected/allowlisted IDs, platform API version, duplicate policy, and explicit non-sandbox acknowledgement. `PluginRegistryPropertiesTest`, `Pf4jControlledPluginDiscoveryService`, `PluginGovernanceBeanConfiguration`, and sample plugin E2E validate controlled-directory startup and refresh discovery. |
| `PLUG-02` | `Pf4jControlledPluginDiscoveryService`, `Pf4jPluginSourceDiscovery`, `Pf4jPluginExtensionBridge`, `PluginGovernanceCatalogAdapter`, and the sample plugin JAR load PF4J descriptors, apply platform/API compatibility, and register plugin capabilities through a current `DefaultExtensionContributionRegistry`. |
| `PLUG-03` | Plugin lifecycle/read models expose discovered, loaded, started, disabled, failed, quarantined, compatibility, health, reason, and redacted error state through `PluginSourceDto` and governance adapters. |
| `PLUG-04` | `GET /api/admin/governance/plugins`, `PluginGovernanceApiTest`, `AdminPluginGovernanceViewTest`, and the Admin Registry view expose metadata, capabilities, health, load errors, compatibility errors, and source summaries. |
| `PLUG-05` | `POST /api/admin/governance/plugins/{pluginId}/disable` and `POST /api/admin/governance/plugins/{pluginId}/quarantine` update plugin state so capabilities are unavailable for new resolution through `DynamicPluginToolRegistry`; `PluginCapabilityDisablementTest`, `PluginGovernanceApiTest`, and live sample plugin REST E2E validate this behavior. |
| `PLUG-06` | Configuration validation, Admin UI warning copy, and this document state that JVM classloader isolation is dependency/lifecycle isolation only and **not a sandbox** for untrusted code. |
| `E2E-08` | MCP portion completed in Phase 07. Phase 08 completes the plugin portion with `PluginGovernedToolE2ETest`, `PluginSecurityRedactionE2ETest`, `SamplePluginJarE2ETest`, and `SamplePluginJarCompatibilityE2ETest`, proving plugin tool registration, `ToolExecutionGateway` invocation, audit/events/redaction, live REST refresh, disable, quarantine, and real sample JAR packaging. |

## Architecture and Module Boundaries

| Module | Responsibility | Boundary |
|--------|----------------|----------|
| `pi-agent-infrastructure-plugin` | PF4J integration, controlled-directory discovery via `Pf4jControlledPluginDiscoveryService`, descriptor/lifecycle summaries, plugin state overlay, bridge into Pi extension contributions, refreshable plugin governance catalog adapter, and `DynamicPluginToolRegistry`. | Only this infrastructure module owns `org.pf4j..` imports and plugin implementation objects. It must not depend on Adapter Web, Vaadin, JDBC persistence, MCP SDK, Spring AI provider adapters, or UI packages. |
| `pi-agent-extension-api` | Public framework-free extension API used by plugin JARs to expose Pi capabilities. | No PF4J dependency; plugin JARs can implement both PF4J extension points and Pi `ExtensionSource`, but the Pi API remains PF4J-free. |
| `pi-agent-adapter-web` | Spring Boot configuration, Admin REST endpoints, Admin Vaadin rendering, and product-path E2E composition. | May compose the plugin infrastructure module, but production code must not import PF4J directly and public API/UI contracts stay in `pi-agent-client` DTOs. |
| `pi-sample-plugin-readonly` | Deterministic in-reactor sample PF4J plugin JAR with one safe read-only Pi tool capability. | Sample plugin depends on Pi extension API and PF4J only; it is not a Spring/Vaadin module. |

`PluginInfrastructureArchitectureTest` and `ExtensionApiArchitectureTest` enforce PF4J/plugin implementation isolation so Domain, App, Client, Extension API, Spring starter public API, MCP infrastructure, model/provider infrastructure, sample-independent modules, and Adapter Web production code do not depend on `org.pf4j..` or `io.github.pi_java.agent.infrastructure.plugin..` where forbidden.

## Controlled Directory Configuration

Cloud Server binds plugin settings under `pi.plugins` in Adapter Web and translates them into `PluginRegistryProperties`:

```yaml
pi:
  plugins:
    enabled: true
    directory: /opt/pi/plugins
    startup-discovery: true
    manual-refresh-enabled: true
    platform-api-version: 1.0.0
    allow-duplicate-overrides: false
    non-sandbox-warning-acknowledged: true
    allowlist:
      - sample-readonly-plugin
    selected:
      - sample-readonly-plugin
```

Important semantics:

1. `enabled=true` requires `directory`.
2. `non-sandbox-warning-acknowledged=true` is required whenever dynamic loading is enabled because PF4J classloader isolation is not a sandbox.
3. `platform-api-version` is the platform compatibility version checked against plugin/extension metadata; default is `1.0.0`.
4. `allowlist` and `selected` constrain trusted plugin IDs that can contribute runtime capabilities. If an allowlist is set, selected plugin IDs must be inside it. Not-allowlisted or unselected plugins remain visible in Admin Governance as unavailable/disabled with sanitized `selectionStatus` metadata, but their capabilities do not enter new `ToolRegistry` list/resolve results.
5. `allow-duplicate-overrides=false` preserves fail-fast duplicate capability behavior by default.
6. The directory is an operator-controlled deployment location. Admin APIs do not upload, install, delete, or upgrade JARs.

## Plugin Packaging and Descriptor Expectations

A Phase 08 plugin JAR is a PF4J plugin package plus one or more Pi extension sources. The sample plugin descriptor is:

```properties
plugin.id=sample-readonly-plugin
plugin.class=io.github.pi_java.agent.sample.plugin.ReadonlySamplePlugin
plugin.version=1.0.0
plugin.provider=Pi Java
plugin.description=Sample read-only Pi extension plugin for controlled PF4J loading tests
plugin.requires=1.0.0
```

The sample extension class implements Pi `ExtensionSource` and PF4J `ExtensionPoint`, and is annotated as a PF4J `@Extension`. Its Pi capability is a normal `ToolExtensionCapability` carrying a canonical `ToolDescriptor` and `ToolExecutorBinding`.

Build the sample plugin without model keys, Docker, or external services:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-sample-plugin-readonly -am package
```

Operators then copy the resulting plugin JAR into the controlled directory configured by `pi.plugins.directory`. Phase 08 tests copy the JAR to a temporary controlled directory to validate the same product path.

## Discovery, Compatibility, and Contribution Flow

Plugin loading follows this path:

```text
configured pi.plugins.directory
  -> Pf4jControlledPluginDiscoveryService
  -> PF4J DefaultPluginManager loadPlugins/startPlugins
  -> Pf4jPluginSourceDiscovery
  -> Pf4jPluginExtensionBridge
  -> ExtensionSource / ToolExtensionCapability
  -> DefaultExtensionContributionRegistry
  -> ExtensionToolRegistry
  -> composite ToolRegistry
  -> ToolExecutionGateway
```

Compatibility failures, load failures, disabled plugins, quarantined plugins, not-allowlisted plugins, and unselected plugins remain visible in Admin Governance with sanitized diagnostics/status metadata. They do not contribute usable runtime capabilities for new resolution.

Plugin tool provenance is `ToolProvenance.SourceKind.PLUGIN`, with plugin/source/capability metadata attached as redacted strings. Runtime callers do not use a source-specific plugin execution API.

## Lifecycle, Disable, and Quarantine Semantics

Phase 08 uses the same lifecycle vocabulary as Phase 06 extensions:

- `DISCOVERED` — descriptor/source observed.
- `LOADED` — plugin package loaded.
- `STARTED` — plugin started and compatible capabilities may be usable.
- `DISABLED` — operator disabled the plugin; capabilities are unavailable for new resolution.
- `FAILED` — load, discovery, or compatibility failure; diagnostics are sanitized.
- `QUARANTINED` — stronger operator/safety isolation state; capabilities are unavailable and metadata marks operator action required.

Disable/quarantine affect new capability resolution, new `ToolRegistry` list/resolve calls, and new runs through `DynamicPluginToolRegistry`. They do not promise interruption of already-running calls, guaranteed JVM hot unload, or classloader/resource reclamation.

Manual refresh through `POST /api/admin/governance/plugins/refresh` re-runs controlled-directory rediscovery via `Pf4jControlledPluginDiscoveryService` and replaces the governance snapshot used for subsequent registry resolution. It is an explicit operator-triggered refresh, not an automatic hot directory watcher, and it does not guarantee JVM hot unload of removed or already-loaded plugin classes.

## Admin Governance REST and UI

Public Admin endpoints:

```text
GET  /api/admin/governance/plugins
POST /api/admin/governance/plugins/refresh
POST /api/admin/governance/plugins/{pluginId}/disable
POST /api/admin/governance/plugins/{pluginId}/quarantine
```

Unsupported in Phase 08:

```text
PUT/PATCH/DELETE /api/admin/governance/plugins
plugin upload/install/delete/upgrade/version-management endpoints
automatic hot directory watch controls
```

`PluginGovernanceResponse` returns `PluginSourceDto` records with plugin ID, name, version, vendor, lifecycle, health, compatibility, capability counts, redacted error, controlled-directory-relative path summary, reason, last updated timestamp, capabilities, and redacted metadata. `PluginCapabilityDto` returns capability ID/type/status/version/plugin ID/enablement/compatibility/health and redacted metadata.

Disable and quarantine accept `PluginMutationRequest` with operation and optional reason. The UI renders these as confirmed POST action plans with optional reason metadata, not hidden local-only state changes.

## Audit and Redaction Guarantees

Plugin governance preserves the existing public-surface redaction model:

- raw absolute paths are reduced to controlled-directory-relative or filename-only summaries,
- raw secrets, environment variables, headers, arguments, plugin-provided sensitive metadata, and stack traces are not exposed through DTOs/UI/default events,
- disable/quarantine operations record actor/principal, plugin ID, operation, previous/resulting state, optional reason, timestamp, and sanitized failure details,
- plugin tool execution still uses the governed tool pipeline for policy, validation, payload limits, audit, redaction, and `tool.lifecycle` events.

## Safety Boundary: Not a Sandbox

Dynamic plugin support is for trusted enterprise plugin JARs in a controlled deployment directory. PF4J/JVM classloader isolation is dependency and lifecycle isolation only; it is **not a sandbox** and does not make untrusted plugin code safe. Untrusted code needs a separate sandbox/permission/isolation model outside Phase 08.

## Verification Commands

Architecture gates:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-extension-api,pi-agent-infrastructure-plugin -am -Dtest=ExtensionApiArchitectureTest,PluginInfrastructureArchitectureTest test
```

Sample plugin package:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-sample-plugin-readonly -am package
```

Focused no-key Phase 08 plugin smoke gate:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-plugin,pi-sample-plugin-readonly,pi-agent-adapter-web -am -Dtest=PluginInfrastructureArchitectureTest,PluginGovernanceCatalogAdapterTest,PluginCapabilityDisablementTest,PluginRegistryPropertiesTest,PluginToolRegistryWiringTest,PluginGovernanceApiTest,SamplePluginJarE2ETest,SamplePluginJarCompatibilityE2ETest test
```

Optional browser governance smoke when browser dependencies are available:

```bash
npm run e2e -- e2e/phase-08-plugin-governance.spec.ts
```

All Java gates are no-key and use fake runtime/tool fixtures, in-memory state, temporary controlled directories, deterministic sample plugin JAR packaging, and redaction assertions.

## Explicit Deferrals

- Plugin marketplace, signing/review, distribution, ratings, billing, and moderation.
- Admin upload/install/delete/upgrade/version-management workflows.
- Automatic hot directory watching.
- Full hot reload/unload guarantees and JVM classloader/resource reclamation promises.
- Running untrusted or semi-trusted arbitrary plugin code.
- Fine-grained plugin permission systems beyond existing `ToolExecutionGateway`, policy, audit, workspace, and credential boundaries.
- Broad sample plugin coverage for model providers, policy providers, workspace providers, memory providers, and event listeners.
- Production telemetry spans/metrics for plugin lifecycle — Phase 09 `OPS-01`.
