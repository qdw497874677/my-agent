# Phase 04 Discovery: JSON Schema and Policy Decision Shape

**Scope:** Targeted discovery for Phase 4 research-needed items: JSON Schema validation/versioning and policy decision schema.

## Decisions for Planning

1. Use `com.networknt:json-schema-validator` in Infrastructure/App-adapter implementation boundaries only, not Domain.
   - Rationale: current Java validator supports Draft v4/v6/v7/v2019-09/v2020-12 and is widely used.
   - Boundary: Domain stores schema as Pi-owned records/maps/strings; networknt types must not enter Domain or App ports.
2. Standardize tool schemas with explicit `schemaDialect`, `schemaVersion`, `inputSchema`, and optional `outputSchema` metadata in `ToolDescriptor`.
   - Use Draft 2020-12 as the default new schema dialect unless a descriptor explicitly declares another supported dialect.
3. Model policy decisions as Pi-owned Domain/App records around the existing `PolicyDecision` enum values: `ALLOW`, `DENY`, `REQUIRE_APPROVAL`, `REQUIRE_SANDBOX`, `BLOCK`.
   - Preserve D-06: `REQUIRE_APPROVAL` is suspend/wait, not immediate deny.
   - Preserve D-07: `REQUIRE_SANDBOX` is a capability gate; if no compatible sandbox executor exists, return a suspended/blocked outcome with audit/events rather than pretending local-temp is a production sandbox.
4. Redaction, validation, payload limits, and result summaries are gateway responsibilities and must be covered before E2E.

## Sources

- Existing project research: `.planning/research/SUMMARY.md`, `.planning/research/ARCHITECTURE.md`, `.planning/research/PITFALLS.md`.
- Current web verification: networknt JSON Schema Validator supports JSON Schema Draft v4, v6, v7, v2019-09, and v2020-12; Maven Central shows current 3.x releases in 2026.

## Planning Implication

Plans should introduce the dependency in root dependencyManagement plus `pi-agent-infrastructure`, keep validation implementation out of Domain/App, and include contract tests proving invalid arguments block execution before executor binding invocation.
