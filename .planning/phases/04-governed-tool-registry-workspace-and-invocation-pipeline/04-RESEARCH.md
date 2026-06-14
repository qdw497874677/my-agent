# Phase 04 Research: Governed Tool Registry, Workspace, and Invocation Pipeline

**Status:** Complete  
**Date:** 2026-06-14

## Planning-Relevant Findings

### JSON Schema Validation

- Use `com.networknt:json-schema-validator` in Infrastructure only. Public metadata indicates the 3.x line is current; `3.0.4` is the latest javadoc version and `3.0.1` appeared in Maven Central in March 2026.
- Networknt supports JSON Schema draft v4, v6, v7, 2019-09, and 2020-12. Phase 4 descriptors should carry schema dialect/version metadata, but Domain/App must represent schema documents as plain Java maps/records and never import Networknt types.
- Plan impact: introduce the dependency only through root dependency management and `pi-agent-infrastructure`; keep `ToolArgumentValidator` as an App port.

### Policy Decision Schema

- Existing `PolicyDecision` already has the required meanings: `ALLOW`, `DENY`, `REQUIRE_APPROVAL`, `REQUIRE_SANDBOX`, and `BLOCK`.
- `REQUIRE_APPROVAL` must be a suspend/wait outcome, not immediate deny. It must emit approval-required events/audit and must not call executor bindings.
- `REQUIRE_SANDBOX` is a capability gate. If no compatible sandbox is available, return sandbox-required/blocked-suspended outcome with audit/events; do not pretend local-temp workspace is a production sandbox.

### Gateway Ordering

Recommended App gateway sequence:

1. Resolve descriptor and executor binding from `ToolRegistry`.
2. Publish `tool.proposed` event.
3. Validate arguments against descriptor input schema.
4. Evaluate policy from descriptor scopes, risk, side effects, agent allowed scopes, policy refs, workspace policy, tenant/user/run context.
5. Generate `ProvisionPreview` when descriptor/policy requires it.
6. Stop without invoking executor for invalid args, deny/block, approval-required, or sandbox-required.
7. Enforce timeout/cancellation/payload limits.
8. Execute binding.
9. Redact/summarize result or error.
10. Persist audit and publish lifecycle event through existing persist-then-emit path.

### Workspace/Command Safety

- Local-temp workspace is acceptable only as dev/test bounded execution. It should be root-constrained, path traversal resistant, short-lived, and documented as not a sandbox.
- Commands should be allowlisted, run with workspace root as working directory, sanitized environment, bounded timeout/cancellation, and truncated stdout/stderr summaries.
- Built-in example tools should be narrow fixtures: safe read-only info, workspace resource write/append, and allowlisted command preview/execute. They must not read arbitrary host env or host files.

## Validation Architecture

Phase 4 validation should be automated and layered:

- Domain contract tests: descriptor metadata, status taxonomy, lifecycle event wire names/payloads, no framework dependencies.
- App unit tests: descriptor-first registry, query service, gateway ordering, no-bypass executor invocation counts, policy/preview/audit/event behavior.
- Infrastructure tests: Networknt validation, in-memory registry, conservative default policy, redaction, payload limits, local-temp workspace boundary, allowlisted command execution.
- Testkit tests: `GeneralAgentLoop` uses `ToolExecutionGateway` rather than direct `ToolInvoker`.
- Adapter tests: read-only `/api/tools`, event DTO mapping, single gateway bean wiring, built-ins visible, persist-then-emit/audit collaborators wired.
- E2E tests: successful model-to-tool-to-model, deny/block, approval-required, preview, workspace-bound command/file action, and fake secret absence in REST/event/audit/persistence outputs.

Recommended quick command style: focused Maven module tests with Java 21, e.g. `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl <module> -am -Dtest=<TestName> test`.

## Pitfalls to Avoid

- Do not add `registerSpringBean`, `registerMcpTool`, or `registerPluginTool` APIs to the core registry.
- Do not let model/runtime/testkit paths continue calling `ToolInvoker.invoke` directly after gateway introduction.
- Do not store raw tool arguments/results in events, audit records, logs, or REST DTOs.
- Do not implement MCP, Java SPI/Spring Bean discovery, PF4J plugins, Web Console approval UI, unrestricted shell/file access, or production sandbox in Phase 4.
- Do not put Jackson, Spring, Networknt, MCP, PF4J, provider SDK, or persistence imports in Domain/App production code.
