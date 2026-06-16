# Phase 8: Controlled Dynamic Plugin JARs - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-16
**Phase:** 08-Controlled Dynamic Plugin JARs
**Areas discussed:** Plugin Loading Model, Lifecycle Controls, Admin Operations, Safety and Verification Boundaries

---

## Gray Area Selection

| Option | Description | Selected |
|--------|-------------|----------|
| All 4 | Cover loading, lifecycle, Admin operations, and safety/test boundaries. | ✓ |
| Loading model | PF4J vs ServiceLoader bridge, packaging, controlled directory, descriptor discovery. | |
| Lifecycle controls | State semantics and disable/quarantine behavior. | |
| Admin operations | View/mutate scope, warnings, errors, capabilities, audit. | |
| Safety verification | Trust posture, non-sandbox warning, E2E/ArchUnit/sample scope. | |

**User's choice:** 全部 4 项（推荐）

---

## Plugin Loading Model

| Decision Point | Option | Description | Selected |
|----------------|--------|-------------|----------|
| Technical baseline | PF4J + bridge | PF4J owns JAR/classloader/lifecycle; bridge into existing ExtensionSource/Capability. | ✓ |
| Technical baseline | Pure ServiceLoader | Simpler but weak dynamic lifecycle/isolation. | |
| Technical baseline | Custom classloader | High control but high risk/cost. | |
| Plugin entry | PluginDescriptor + ExtensionSource | PF4J/plugin metadata plus Pi extension capabilities. | ✓ |
| Plugin entry | ExtensionSource only | Close to Phase 6 but weak plugin lifecycle metadata. | |
| Plugin entry | New Plugin SPI | More specialized but duplicates Phase 6. | |
| Directory config | Configuration-file-first | Spring/YAML/env controls; no Admin upload/install. | ✓ |
| Directory config | Admin upload/install | Full product flow but marketplace/distribution scope. | |
| Directory config | Fixed default directory | Simple but not enterprise-controlled. | |
| Load timing | Startup scan + manual refresh | Low-risk dynamic behavior mirroring Phase 7 MCP refresh. | ✓ |
| Load timing | Startup only | Stable but weak Phase 8 dynamic value. | |
| Load timing | Automatic hot watcher | More dynamic but higher concurrency/rollback/classloader risk. | |

**User's choices:** PF4J + 桥接；PluginDescriptor + ExtensionSource；配置文件优先；启动扫描 + 手动刷新。

---

## Lifecycle Controls

| Decision Point | Option | Description | Selected |
|----------------|--------|-------------|----------|
| Disable/quarantine effect | New Run/new calls only | Prevent new resolution/invocation without forcibly killing in-flight work. | ✓ |
| Disable/quarantine effect | Immediately interrupt all runs | Stronger but requires cross-run cancellation/cleanup. | |
| Disable/quarantine effect | Plugin-declared strategy | Flexible but delegates control to plugin. | |
| Quarantine meaning | Stronger than disable | Risk/failure isolation, unusable, prominent, explicit release required. | ✓ |
| Quarantine meaning | Same as disable | Simpler but weak governance semantics. | |
| Quarantine meaning | Auto-delete plugin | Too destructive for v1. | |
| State model | Reuse ExtensionLifecycleState | Use existing states plus plugin-specific reason/error/source metadata. | ✓ |
| State model | New PluginState | More specific but splits governance language. | |
| State model | String states | Fast but weak typing/testing. | |
| Unload/reload | Disable/quarantine, no hot-unload promise | Fits v1 and JVM limitations. | ✓ |
| Unload/reload | Best-effort stop + reload as promise | Useful internally but risky to promise. | |
| Unload/reload | Full hot unload guarantee | Out of v1 scope. | |

**User's choices:** 只影响新 Run；quarantine 更强；直接复用并补充插件原因；disable/quarantine，不承诺热卸载。

---

## Admin Operations

| Decision Point | Option | Description | Selected |
|----------------|--------|-------------|----------|
| Admin operation scope | View + refresh + disable/quarantine | Real PLUG-05 controls without marketplace scope. | ✓ |
| Admin operation scope | Complete plugin management | Upload/install/upgrade/delete, too broad. | |
| Admin operation scope | Read-only | Fails PLUG-05. | |
| Detail content | Metadata/capabilities/health/errors | Full governance view with redacted diagnostics. | ✓ |
| Detail content | Summary only | Too little for operations. | |
| Detail content | Raw exception details | Leaks sensitive data. | |
| Audit | Must audit | Records actor, plugin ID, operation, reason, before/after state. | ✓ |
| Audit | Logs only | Insufficient for governance. | |
| Audit | No record | Not acceptable for security-sensitive plugin changes. | |
| Confirmation/reason | Confirm + optional reason | Prevents mistakes while keeping v1 simple. | ✓ |
| Confirmation/reason | Click applies immediately | Too risky. | |
| Confirmation/reason | Required reason | More complete but heavier UI/test scope. | |

**User's choices:** 查看 + 刷新 + 禁用/隔离；元数据/能力/健康/错误；必须审计；确认 + 可选原因。

---

## Safety and Verification Boundaries

| Decision Point | Option | Description | Selected |
|----------------|--------|-------------|----------|
| Trust model | Trusted directory, not sandbox | Controlled trusted plugin JARs; classloader isolation is not a security sandbox. | ✓ |
| Trust model | Semi-trusted plugins | Misleading without real sandbox. | |
| Trust model | Untrusted plugins | Out of v1. | |
| Sample plugin scope | Tool + metadata | Safe read-only tool, metadata, health, compatibility. | ✓ |
| Sample plugin scope | All capability types | Comprehensive but too broad. | |
| Sample plugin scope | Descriptor only | Insufficient for ToolGateway E2E. | |
| Default capability policy | Explicit allowlist + existing policy | Visible does not mean executable; keep Agent allowlists and ToolPolicyEvaluator. | ✓ |
| Default capability policy | Auto-available | Unsafe. | |
| Default capability policy | All require approval | Overly broad; existing risk/policy should decide. | |
| Verification matrix | Full chain + boundary gates | Load, compat fail, registration, ToolGateway, disable/quarantine, Admin, audit/redaction, ArchUnit. | ✓ |
| Verification matrix | Unit tests only | Insufficient product-path coverage. | |
| Verification matrix | Manual UAT only | Violates automated verification expectations. | |

**User's choices:** 可信目录，非沙箱；工具 + 元数据为主；显式 allowlist + 现有 policy；全链路 + 边界门禁。

---

## the agent's Discretion

- Exact module and package names.
- Exact DTO/API/property names.
- Whether plugin governance is a dedicated catalog port or extension governance adapter extension.
- Exact best-effort stop/refresh internals, provided no guaranteed hot unload is promised.

## Deferred Ideas

- Plugin marketplace and distribution workflows.
- Admin upload/install/delete/upgrade workflows.
- Automatic directory hot watching and full hot reload/unload guarantees.
- Untrusted/semi-trusted plugin execution.
- Full sample coverage for every extension capability family.
