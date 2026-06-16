# Phase 8 Discovery: Controlled Dynamic Plugin JARs

**Phase:** 08-controlled-dynamic-plugin-jars  
**Created:** 2026-06-16  
**Discovery level:** Level 2 — new PF4J integration and executable plugin packaging.

## Research Findings

### PF4J baseline

- Use PF4J as the plugin JAR/classloader/lifecycle implementation per D-01.
- PF4J supports `plugin.properties` in the plugin archive root with `plugin.class`, `plugin.id`, `plugin.version`, `plugin.provider`, `plugin.requires`, dependencies, description, and license.
- PF4J lifecycle entry points are `PluginManager.loadPlugins()`, `startPlugins()`, and plugin-level start/stop operations. Pi must map these lifecycle observations into existing `ExtensionLifecycleState` values per D-06.
- PF4J metadata is not enough for Pi runtime capability registration; plugin code should expose Pi `ExtensionSource` instances through a bridge per D-02.

### Spring Boot packaging constraints

- Spring Boot executable jars use nested dependency jars and a Boot launcher/classloader. External plugin JARs should live outside the executable app artifact in a controlled directory configured via typed properties.
- Do not promise hot unload/classloader reclamation. Disable/quarantine should stop new resolution/invocation and optionally best-effort stop plugin lifecycle only where safe, per D-07 through D-09.

### Maven/sample plugin packaging

- Use an in-reactor deterministic sample plugin JAR for tests, with `maven-jar-plugin` manifest or `plugin.properties` metadata and no external services/keys.
- Keep sample scope narrow: one safe read-only tool plus metadata/health/compatibility per D-22.

## Planning Implications

- Create an isolated `pi-agent-infrastructure-plugin` module containing all PF4J imports and plugin classloader code per D-05 and D-24.
- Keep Domain/App/client/extension API/Spring starter/MCP/provider modules free of PF4J dependencies; enforce with ArchUnit in closeout.
- Add App/client plugin governance contracts before infrastructure wiring so Adapter REST/UI can stay public-DTO-only per D-16.
- Model mutation as narrow audited refresh/disable/quarantine operations; no upload/install/delete/upgrade or hot watching per D-03, D-04, and D-11.

## Sources Queried

- PF4J docs via Context7: plugin descriptor formats, lifecycle, plugin manager loading/starting.
- Spring Boot 3.5 docs via Context7: executable JAR format, nested JARs, configuration metadata.
- Apache Maven docs via Context7: jar plugin classifier/attached JAR patterns for deterministic test artifacts.
