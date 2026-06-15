---
phase: 06-java-extension-surface-spi-and-spring
plan: 05
subsystem: spring-annotation-extensions
tags: [java, spring-boot, annotations, extension-spi, tool-registry, governance]

requires:
  - phase: 06-java-extension-surface-spi-and-spring
    provides: Spring Boot starter auto-configuration and deterministic ExtensionSource contribution registry from plan 06-04
provides:
  - Lightweight runtime-retained @PiTool annotation for Spring bean methods
  - Lightweight runtime-retained @PiEventListener annotation for Spring bean methods
  - Annotation factories that convert already-registered Spring bean methods into ExtensionSource capabilities
  - Annotated tool ToolDescriptor plus ToolExecutorBinding registration with SPRING_BEAN provenance
  - ApplicationContextRunner coverage for annotated tool registry, listener governance, binding execution, and duplicate capability failure
affects: [phase-06, spring-extension-registration, tool-registry, extension-governance, phase-07-mcp, phase-08-plugins]

tech-stack:
  added: []
  patterns: [runtime-retained method annotations, explicit bean-factory annotation inspection, descriptor-first annotated tools, governance-visible event listener metadata]

key-files:
  created:
    - pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/annotation/PiTool.java
    - pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/annotation/PiEventListener.java
    - pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/autoconfigure/AnnotatedToolExtensionSourceFactory.java
    - pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/autoconfigure/AnnotatedEventListenerExtensionSourceFactory.java
    - pi-agent-spring-boot-starter/src/test/java/io/github/pi_java/agent/spring/autoconfigure/AnnotatedSpringExtensionTest.java
  modified:
    - pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/autoconfigure/PiAgentExtensionAutoConfiguration.java
    - pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/autoconfigure/SpringExtensionSourceFactory.java

key-decisions:
  - "Limit Spring annotations to method-backed lightweight tools and event listener metadata; complex providers, policies, workspace, and memory extensions still require explicit ExtensionSource beans."
  - "Discover annotations from already-registered Spring beans through explicit starter factories rather than component-scan magic."
  - "Represent annotated tools as ordinary ToolExtensionCapability entries with ToolDescriptor plus ToolExecutorBinding so execution remains behind ToolRegistry and ToolExecutionGateway consumers."

patterns-established:
  - "Annotation factories return ExtensionSource instances that merge into the existing ServiceLoader/Spring source discovery path."
  - "Annotated tools carry SPRING_BEAN provenance with binding refs formatted as beanName#methodName."
  - "Annotated listener methods contribute EVENT_LISTENER governance capabilities only; they do not replace EventSink persistence/fanout."

requirements-completed: [EXT-02, EXT-05]

duration: 6m
completed: 2026-06-15
---

# Phase 06 Plan 05: Spring Annotation Extension Summary

**Lightweight Spring method annotations now register tools and event listener metadata through the same governed extension contribution path as explicit beans.**

## Performance

- **Duration:** 6m
- **Started:** 2026-06-15T23:42:17Z
- **Completed:** 2026-06-15T23:48:20Z
- **Tasks:** 3
- **Files modified:** 7

## Accomplishments

- Added `@PiTool` and `@PiEventListener` as the only annotation-driven Spring extension types in this phase.
- Implemented starter-owned annotation scanning over already-registered Spring beans, preserving the explicit Boot auto-configuration boundary.
- Converted annotated tool methods into descriptor-first `ToolExtensionCapability` registrations with `ToolExecutorBinding` and `SPRING_BEAN` provenance.
- Converted annotated listener methods into governance-visible `EVENT_LISTENER` capabilities without altering event sink persistence/fanout semantics.
- Added focused ApplicationContextRunner tests for annotation metadata, tool registration, resolved binding execution, listener governance, and duplicate capability failure.

## Task Commits

Each task was committed atomically:

1. **Task 1: Define limited Spring extension annotations** - `a7dac00` (feat)
2. **Task 2: Convert annotated Spring methods into extension sources** - `d3ebc12` (feat)
3. **Task 3: Test annotation registration and boundary behavior** - `5a96316` (test)

**Plan metadata:** pending final docs commit

_Note: TDD tasks used failing-first verification before implementation commits; the test file was incrementally expanded across the three task commits._

## Files Created/Modified

- `pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/annotation/PiTool.java` - Lightweight runtime method annotation for tool metadata including id, name, description, version, scopes, risk, side effect, timeout, raw schema string, and metadata.
- `pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/annotation/PiEventListener.java` - Lightweight runtime method annotation for event listener metadata including id, event types, order, version, and metadata.
- `pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/autoconfigure/AnnotatedToolExtensionSourceFactory.java` - Scans registered Spring beans for `@PiTool`, validates conservative signatures, and builds `ToolExtensionCapability` descriptors and bindings.
- `pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/autoconfigure/AnnotatedEventListenerExtensionSourceFactory.java` - Scans registered Spring beans for `@PiEventListener` and builds governance-visible listener capabilities.
- `pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/autoconfigure/PiAgentExtensionAutoConfiguration.java` - Wires annotation factories into the existing extension contribution registry build path.
- `pi-agent-spring-boot-starter/src/main/java/io/github/pi_java/agent/spring/autoconfigure/SpringExtensionSourceFactory.java` - Merges explicit Spring `ExtensionSource` beans with optional annotation-derived sources.
- `pi-agent-spring-boot-starter/src/test/java/io/github/pi_java/agent/spring/autoconfigure/AnnotatedSpringExtensionTest.java` - Covers annotation contracts, registry behavior, binding execution, governance, and duplicate rejection.

## Decisions Made

- Kept annotation scope intentionally narrow: tools and event listeners only, matching D-10 and avoiding annotation-driven providers/policies/workspace/memory scope creep.
- Used Spring bean factory type inspection instead of component scanning so only beans already admitted into the Spring application context can contribute annotation capabilities.
- Used a minimal default object `ToolSchema` for annotated tools; the raw `inputSchema` annotation string is retained in the annotation contract but no Jackson/JSON Schema parser dependency was added to the starter.

## Deviations from Plan

None - plan executed within the intended annotation scope.

## Issues Encountered

- Duplicate capability tests intentionally log Spring context initialization warnings because ApplicationContextRunner asserts fail-fast duplicate behavior.
- Maven emits existing SLF4J no-provider warnings from upstream test dependencies; tests pass and no logging dependency was added.
- Pre-existing unrelated planning-file changes and `bun.lock` remain in the working tree from parallel work; this plan did not stage or modify them.

## Known Stubs

None. Annotation-derived tools are registered into the concrete extension registry and can be resolved to executable bindings; annotation-derived listeners are intentionally metadata/governance capabilities for the future listener bridge and do not replace `EventSink` semantics.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-spring-boot-starter -am -Dtest=AnnotatedSpringExtensionTest test`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-spring-boot-starter -am test`

## Next Phase Readiness

- Plan 06-06 can wire the Cloud Server through the starter and expose real read-only extension governance using explicit and annotation-derived Spring sources.
- Plan 06-07 can add conformance tests proving annotated tools still execute through the governed gateway path.
- Later MCP/PF4J phases can continue normalizing all source types into the same contribution registry language.

## Self-Check: PASSED

- Verified key files exist: `PiTool.java`, `PiEventListener.java`, `AnnotatedToolExtensionSourceFactory.java`, `AnnotatedEventListenerExtensionSourceFactory.java`, and `AnnotatedSpringExtensionTest.java`.
- Verified task commits exist: `a7dac00`, `d3ebc12`, and `5a96316`.

---
*Phase: 06-java-extension-surface-spi-and-spring*
*Completed: 2026-06-15*
