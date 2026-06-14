# Phase 4: Governed Tool Registry, Workspace, and Invocation Pipeline - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-14
**Phase:** 04-Governed Tool Registry, Workspace, and Invocation Pipeline
**Areas discussed:** Gateway contract, Policy approval flow, Workspace execution model, Redaction/audit/events, Built-in example tools

---

## Area Selection

| Option | Description | Selected |
|--------|-------------|----------|
| Gateway contract | Shape and boundaries for ToolDescriptor, ToolRegistry, ToolExecutorBinding, and ToolExecutionGateway. | ✓ |
| Policy approval flow | How deny/approval/sandbox/provision-preview behave before Phase 5 UI approval cards exist. | ✓ |
| Workspace execution model | Whether Phase 4 ships fake-only, local-temp bounded, or another restricted workspace/command implementation. | ✓ |
| Redaction/audit/events | How tool inputs/outputs are summarized/redacted, which lifecycle events exist, and what audit records capture. | ✓ |
| Built-in example tools | Which safe read-only and side-effectful tools demonstrate the gateway without broad shell/file access. | ✓ |

**User's choice:** Discuss all areas.
**Notes:** User requested Chinese response and re-asked after dismissing the first English question.

---

## Gateway Contract

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Tool Registry registration model | Standardized descriptor first | All sources normalize into ToolDescriptor + ToolExecutorBinding; Registry remains source-agnostic. | ✓ |
| Tool Registry registration model | Source-aware Registry | Registry directly supports source-specific registration methods. | |
| Tool Registry registration model | Simplify then refactor | Phase 4 supports only built-in/fake registration first. | |
| Tool Registry registration model | the agent decides | Only single gateway is locked. | |
| ToolDescriptor risk/side-effect granularity | Explicit classification | Descriptor includes scopes, riskLevel, sideEffectClass, timeoutDefaults, provenance/version, input/output schema. | ✓ |
| ToolDescriptor risk/side-effect granularity | Minimal classification | readOnly / sideEffectful / dangerous plus schema and timeout. | |
| ToolDescriptor risk/side-effect granularity | Policy externalized | Descriptor describes tool only; policy carries risk/side-effect. | |
| ToolDescriptor risk/side-effect granularity | the agent decides | Planner chooses the TOOL-01 minimum. | |
| Gateway responsibilities | Full-chain governance | Resolve, validate, policy, preview/approval/sandbox gate, timeout/cancel/payload limit, execute, normalize/redact/audit/events. | ✓ |
| Gateway responsibilities | Execution governance only | Gateway handles validate/policy/timeout/execute; caller handles audit/events/redaction. | |
| Gateway responsibilities | Layered gateway | Gateway orchestrates validator/policy/auditor/redactor/executor services. | |
| Gateway responsibilities | the agent decides | Only one entry point is locked. | |
| Gateway and ToolInvoker relation | Gateway wraps ExecutorBinding | ToolInvoker/FakeToolInvoker becomes an executor binding; GeneralAgentLoop calls Gateway only. | ✓ |
| Gateway and ToolInvoker relation | Replace ToolInvoker | Deprecate ToolInvoker and introduce ToolExecutionGateway + ToolExecutor. | |
| Gateway and ToolInvoker relation | Coexist temporarily | Keep direct ToolInvoker path alongside Gateway. | |
| Gateway and ToolInvoker relation | the agent decides | Planner chooses compatibility vs clarity. | |

**User's choice:** Recommended options for all gateway-contract questions.
**Notes:** Locks descriptor-first source normalization and a non-bypassable full-chain gateway.

---

## Policy Approval Flow

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| REQUIRE_APPROVAL runtime behavior | Suspend and wait | Gateway emits approval-required event and run enters waiting/suspended state for future resume. | ✓ |
| REQUIRE_APPROVAL runtime behavior | Immediate block | Treat approval-required as policy-blocked because no UI exists yet. | |
| REQUIRE_APPROVAL runtime behavior | Auto reject | Treat approval-required as deny. | |
| REQUIRE_APPROVAL runtime behavior | the agent decides | Only event/audit representation is locked. | |
| REQUIRE_SANDBOX behavior | Capability gate | If no sandbox executor exists, return require-sandbox blocked/suspended; fake sandbox may test branch. | ✓ |
| REQUIRE_SANDBOX behavior | local-temp as sandbox | Treat local-temp bounded executor as sandbox. | |
| REQUIRE_SANDBOX behavior | Always block | Require-sandbox always blocks. | |
| REQUIRE_SANDBOX behavior | the agent decides | Planner chooses minimal honest behavior. | |
| Provision/preview trigger | Risk actions require preview | Side-effectful/destructive or policy-required tools generate preview before execution. | ✓ |
| Provision/preview trigger | Caller explicitly requests | Only `askPreview=true` triggers preview. | |
| Provision/preview trigger | Preview all tools | Every tool generates preview. | |
| Provision/preview trigger | the agent decides | Only preview contract is locked. | |
| Default policy engine | Conservative configurable | Allow safe read-only, require preview/approval for side effects, deny/block dangerous by default. | ✓ |
| Default policy engine | Allow unless denied | Faster developer experience, weaker cloud safety. | |
| Default policy engine | Deny unless allowed | Strongest safety, heavier early experience. | |
| Default policy engine | the agent decides | Only pluggable ToolPolicy is locked. | |

**User's choice:** Recommended options for all policy-flow questions.
**Notes:** Approval-required must be future-resumable even before Web Console exists.

---

## Workspace Execution Model

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Default Workspace/Command implementation depth | fake + local-temp bounded | Keep deterministic fake and add restricted local-temp workspace/command executor with workspace root, allowlist, timeout, and no unrestricted host shell. | ✓ |
| Default Workspace/Command implementation depth | fake-only | Only fake workspace/command proves contracts. | |
| Default Workspace/Command implementation depth | production-grade sandbox | Add container/Firecracker/nsjail-style sandbox. | |
| Default Workspace/Command implementation depth | the agent decides | Only no unrestricted host shell/filesystem is locked. | |
| Workspace-backed tool exposure | Narrow tool set | Provide explicit workspace read/write/allowed-command tools, each governed by descriptor/policy. | ✓ |
| Workspace-backed tool exposure | Generic shell/file tools | One broad executeCommand/readFile/writeFile tool. | |
| Workspace-backed tool exposure | Read-only only | No side-effectful workspace tool in Phase 4. | |
| Workspace-backed tool exposure | the agent decides | Planner defines the minimum verifiable tool set. | |
| Command allowlist location | Policy + Workspace config | Command allowance decided from policy/workspace config/tool descriptor/context. | ✓ |
| Command allowlist location | Hardcoded in code | Built-in fixed command list only. | |
| Command allowlist location | Tool parameters declare it | Per-call tool args declare allowed commands. | |
| Command allowlist location | the agent decides | Only no bypass of Gateway/policy is locked. | |
| local-temp data lifecycle | short-lived test/dev | Create per run/session and clean up where safe; persistent production workspace storage deferred. | ✓ |
| local-temp data lifecycle | persist locally | Keep local directory for debugging. | |
| local-temp data lifecycle | in-memory only | Avoid disk writes entirely. | |
| local-temp data lifecycle | the agent decides | Planner decides based on E2E needs. | |

**User's choice:** Recommended options for all workspace questions.
**Notes:** Local-temp bounded execution is not a production sandbox.

---

## Redaction, Audit, and Events

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Tool input/output redaction strategy | Schema-driven + conservative default | Descriptor/schema marks sensitive fields; unknown/large payloads summarize/truncate; secrets show only references/redacted metadata. | ✓ |
| Tool input/output redaction strategy | denylist keywords | Redact names like password/token/key. | |
| Tool input/output redaction strategy | summarize everything | Store only summaries, no raw text. | |
| Tool input/output redaction strategy | the agent decides | Only no secret/sensitive default exposure is locked. | |
| Tool lifecycle RunEvents granularity | Complete state machine | proposed, policy_decided, preview_generated, approval_required, started, updated, completed, failed, denied, cancelled. | ✓ |
| Tool lifecycle RunEvents granularity | Minimal loop | proposed, policy_decided, started, completed/failed/denied/cancelled. | |
| Tool lifecycle RunEvents granularity | Reuse existing events | Avoid new event types where possible. | |
| Tool lifecycle RunEvents granularity | the agent decides | Planner defines enum coverage for TOOL-06. | |
| Audit record granularity | Decision-level + summary-level | Record policy, preview, approval, execution outcome with tool/risk/principal/workspace/redacted summaries/reason/trace IDs. | ✓ |
| Audit record granularity | High-risk/failures only | Reduce data volume but lose ordinary tool auditability. | |
| Audit record granularity | RunEvents only | No separate audit_records. | |
| Audit record granularity | the agent decides | Only security-sensitive actions require audit. | |
| Payload limit/result summarization | enforce limits + summarize | Layered limits; oversized content becomes summary/truncation metadata or Artifact/Resource reference. | ✓ |
| Payload limit/result summarization | limit events only | ToolResult may keep full content. | |
| Payload limit/result summarization | no limits yet | Defer to hardening. | |
| Payload limit/result summarization | the agent decides | Only payload limit hook is locked. | |

**User's choice:** Recommended options for all redaction/audit/event questions.
**Notes:** Payload limits and summarization are Phase 4 requirements, not later hardening.

---

## Built-in Example Tools

| Question | Option | Description | Selected |
|----------|--------|-------------|----------|
| Built-in example categories | Three-category minimum set | read-only info tool + workspace write artifact/resource tool + allowlisted command preview/execute tool. | ✓ |
| Built-in example categories | read-only + side-effectful | Two-class coverage. | |
| Built-in example categories | more demo tools | Add HTTP/mock API/secret-consuming tools. | |
| Built-in example categories | the agent decides | Only read-only and side-effectful coverage is locked. | |
| Side-effectful default execution | preview then approval | Side-effect tool previews first; policy may require approval; no execution before approval. | ✓ |
| Side-effectful default execution | preview then auto execute | Execute if policy allows after preview. | |
| Side-effectful default execution | default deny | Only prove blocking. | |
| Side-effectful default execution | the agent decides | Only preview/policy trigger is locked. | |
| Real env/system access | no real sensitive env | Use injected fake/config test secrets and workspace-scoped resources; do not read arbitrary host env/system files. | ✓ |
| Real env/system access | allowlisted env only | Read explicit env allowlist. | |
| Real env/system access | system info read-only | Read non-sensitive system info like date/java version. | |
| Real env/system access | the agent decides | Planner chooses safe testing method. | |
| Tool Registry query API | direct REST/API | Phase 4 directly exposes Tool Registry catalog/query API for future Admin Governance. | ✓ |
| Tool Registry query API | App use case + optional REST | Define App query use case; expose REST if cheap. | |
| Tool Registry query API | App use case only | Phase 5 adds REST/Admin. | |
| Tool Registry query API | the agent decides | Only future Admin queryability locked. | |

**User's choice:** Three-category tools, preview-then-approval, no real sensitive host env/system reads, and direct REST/API for Tool Registry query.
**Notes:** User selected direct REST/API rather than the recommended lighter option.

---

## the agent's Discretion

- Exact Java record/interface names, package subdivisions, enum names, DTO endpoint paths, and internal service decomposition.
- Exact JSON Schema validator library and schema draft/version, as long as validation remains outside Domain and is tested.
- Exact bounded local-temp workspace implementation details and cleanup mechanics.

## Deferred Ideas

- Public Java SPI/Spring Bean tool registration — Phase 6.
- MCP tool bridge — Phase 7.
- Dynamic plugin tool registration — Phase 8.
- Production-grade sandbox/Coding Agent workspace — future phase.
- Web Console approval/tool cards — Phase 5.
