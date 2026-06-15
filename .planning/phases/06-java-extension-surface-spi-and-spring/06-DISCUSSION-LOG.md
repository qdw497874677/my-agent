# Phase 6: Java Extension Surface: SPI and Spring - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-15T16:30:50Z
**Phase:** 06-Java Extension Surface: SPI and Spring
**Areas discussed:** Module Packaging, Extension Contract Shape, Spring Registration Style, Lifecycle and Governance, Conformance and Safety Boundaries

---

## Module Packaging

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Extension API/JAR packaging | Split SDK + Starter | Add framework-free extension SPI/SDK module plus Spring Boot starter/autoconfigure module. | ✓ |
| Extension API/JAR packaging | Single extension module | Put SPI contracts and Spring support together. | |
| Extension API/JAR packaging | Put into existing modules | Minimize module changes but weaken public extension boundary. | |
| SPI/SDK dependencies | Domain/App contracts only | Keep SDK framework-free and free of Adapter/Infrastructure/Spring. | ✓ |
| SPI/SDK dependencies | Domain/App/Client | Allow public DTO reuse at higher coupling cost. | |
| SPI/SDK dependencies | Near-zero dependencies | Maximize independence but duplicate domain types. | |
| Starter audience | External apps can reference it | Design the starter as public enterprise integration surface; Cloud Server consumes it too. | ✓ |
| Starter audience | Cloud Server internal only | Faster but weaker public starter validation. | |
| Starter audience | No starter | Manual Spring Bean wiring only. | |
| Sample extension artifact/module | Samples/test fixtures | Validate ServiceLoader, Spring registration, and conformance without production sample module. | ✓ |
| Sample extension artifact/module | Formal sample module | More visible but more maintenance surface. | |
| Sample extension artifact/module | No sample | Less work but weaker verification/documentation. | |

**User's choice:** `1A 2A 3A 4A`
**Notes:** The first interactive question returned invalid repeated values, so the area was re-asked in plain-text format. User confirmed all recommended options.

---

## Extension Contract Shape

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| First public SPI/SDK capabilities | Full roadmap family | Tool, Model Provider, Policy, EventSink, Workspace/Resource, Memory, metadata/lifecycle. | ✓ |
| First public SPI/SDK capabilities | Mature existing ports only | Defer Memory SPI. | |
| First public SPI/SDK capabilities | Minimal first | Tool + Provider + Policy + metadata only. | |
| SPI organization | Extension source with capability providers | Each extension exposes metadata plus multiple capability providers. | ✓ |
| SPI organization | Independent ServiceLoader per capability | Simpler, weaker source aggregation. | |
| SPI organization | Single large interface | Easy initially, likely bloated. | |
| Tool extension submission | Complete ToolDescriptor + ToolExecutorBinding | Reuse Phase 4 descriptor-first gateway model. | ✓ |
| Tool extension submission | Simplified metadata inferred by platform | More author-friendly but inference complexity. | |
| Tool extension submission | SPI full descriptor, Spring annotation inference | Flexible, needs clear boundary. | |
| Memory provider handling | Minimal placeholder SPI | Define extension point; no runtime chain/RAG/vector integration. | ✓ |
| Memory provider handling | Defer Memory SPI | Leave for later Memory/RAG phase. | |
| Memory provider handling | Define and wire basic read/write | Would expand Phase 6 scope. | |

**User's choice:** `1A 2A 3A 4A`
**Notes:** User confirmed all recommended options.

---

## Spring Registration Style

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Spring registration modes | Bean + annotation | Beans for explicit enterprise integration; annotations for lightweight tool/listener cases. | ✓ |
| Spring registration modes | Beans only | Clear and type-safe, less annotation scanning. | |
| Spring registration modes | Annotation only | Simple for tools, weak for complex capabilities. | |
| Annotation capability scope | Tools and Event listeners only | Keep annotation support lightweight; complex capabilities use Beans. | ✓ |
| Annotation capability scope | All capability types | More uniform but too broad. | |
| Annotation capability scope | No annotations | Bean-only path. | |
| Autoconfiguration approach | Dedicated starter contributes composites | Discover SPI/Spring extensions and merge into existing platform ports. | ✓ |
| Autoconfiguration approach | Adapter Web imports config manually | Simpler, weaker starter reuse. | |
| Autoconfiguration approach | Library only | Application assembles everything manually. | |
| Conflict/priority handling | Explicit order/source metadata/fail-fast | Deterministic merge, duplicate IDs fail by default. | ✓ |
| Conflict/priority handling | Later overrides earlier | Convenient but risky. | |
| Conflict/priority handling | Spring @Primary/name only | Simple but opaque across SPI/Spring sources. | |

**User's choice:** `1A 2A 3A 4A`
**Notes:** User confirmed all recommended options.

---

## Lifecycle and Governance

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Lifecycle depth | Startup states | DISCOVERED / REGISTERED / FAILED / DISABLED; no runtime hot load/unload. | ✓ |
| Lifecycle depth | Full lifecycle | LOADED / STARTED / STOPPED / DISABLED / FAILED, closer to plugin model. | |
| Lifecycle depth | Minimal state | Registered/failed only. | |
| Enable/disable behavior | Config-driven effective disable | Startup config can disable extension/source/capability; no UI dynamic switch. | ✓ |
| Enable/disable behavior | Display only | Does not affect registry. | |
| Enable/disable behavior | Dynamic Admin API/UI | Larger scope, closer to Phase 8. | |
| Compatibility/version checks | API version range + schema version | Incompatible capabilities do not register and expose errors. | ✓ |
| Compatibility/version checks | Manifest version only | Simpler, less robust. | |
| Compatibility/version checks | Defer to PF4J | No Phase 6 compatibility gate. | |
| Admin display granularity | Source + capabilities + health/errors | Read-only source, capability count/type, disabled/failed reason, compatibility. | ✓ |
| Admin display granularity | Summary counts only | Less detail. | |
| Admin display granularity | Display plus mutation controls | Larger scope, Phase 8-like. | |

**User's choice:** `1A 2A 3A 4A`
**Notes:** User confirmed all recommended options.

---

## Conformance and Safety Boundaries

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Conformance boundary coverage | Full boundary | ToolGateway, Policy, Audit, RunEvent/EventSink, CredentialRef/SecretRef, Workspace, Admin redaction. | ✓ |
| Conformance boundary coverage | ToolGateway + CredentialRef only | Narrower risk focus. | |
| Conformance boundary coverage | Architecture/unit tests only | No E2E conformance. | |
| SPI/Spring validation path | Dual-path same suite | ServiceLoader and Spring Bean/annotation samples pass same conformance suite. | ✓ |
| SPI/Spring validation path | SPI conformance, Spring unit tests | Less parity assurance. | |
| SPI/Spring validation path | Spring conformance, SPI unit tests | Less parity assurance. | |
| Architecture gates | Add SDK/starter ArchUnit | SDK forbids Spring/PF4J/MCP/Adapter/Infrastructure; no Domain/App reverse deps. | ✓ |
| Architecture gates | Update existing Domain/App only | Less complete module boundary coverage. | |
| Architecture gates | Maven only | Weaker protection. | |
| Documentation depth | Phase 6 contract doc | Public APIs, ServiceLoader files, starter usage, metadata, governance, conformance, deferrals. | ✓ |
| Documentation depth | README/requirements only | Less useful for downstream phases. | |
| Documentation depth | No dedicated doc | Weak downstream contract. | |

**User's choice:** `1A 2A 3A 4A`
**Notes:** User confirmed all recommended options.

---

## the agent's Discretion

- Exact module, interface, record, package, and annotation names are left to planning/implementation discretion within the locked architectural boundaries.
- Exact compatibility version range syntax is left to planning discretion as long as it is machine-checkable and documented.
- Exact Admin Governance endpoint/DTO naming is left to planning discretion as long as the surface remains read-only and uses public DTO boundaries.

## Deferred Ideas

- MCP transport/discovery/execution details remain Phase 7.
- PF4J/dynamic plugin classloader lifecycle, disable/quarantine, and runtime plugin management remain Phase 8.
- Runtime hot reload/unload for SPI/Spring extensions is out of Phase 6.
- Admin mutation controls for extension enable/disable are out of Phase 6.
- Full Memory/RAG integration is deferred; Phase 6 only defines a minimal MemoryProvider SPI placeholder.
