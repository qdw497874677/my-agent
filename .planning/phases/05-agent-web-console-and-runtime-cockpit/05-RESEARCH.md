# Phase 5 Research: Agent Web Console and Runtime Cockpit

**Date:** 2026-06-15  
**Scope:** Vaadin Flow all-Java Web Console, Spring Security integration, public REST/SSE consumption, and Playwright browser E2E.

## Findings

- Use Vaadin Flow in the existing `pi-agent-adapter-web` adapter module. Vaadin is an Adapter concern and must not leak into `pi-agent-domain`, `pi-agent-app`, or `pi-agent-client` beyond public DTO contracts.
- Add Vaadin dependencies through Maven dependency management and the web adapter only. Keep Java production code Java-first; Playwright is test-only per D-17.
- Vaadin security must preserve existing API authentication: `/api/**` remains authenticated, health endpoints remain public, and Vaadin routes/static resources need route access without breaking resource-server/JWT readiness. Vaadin's security configurer orders default public resources before secured routes; any `anyRequest` rule must be late to avoid redirect loops.
- The UI should call public REST/SSE/read-model APIs. It should use `RestClient`/`ObjectMapper` or thin adapter services instead of directly injecting App use cases, repositories, runtime internals, or database objects.
- SSE can be consumed from the browser using the existing `/api/sessions/{sessionId}/runs/{runId}/stream` endpoint. UI logic should replay current event history first and then subscribe to live stream; reconnect should use `Last-Event-ID` or `afterSequence` semantics already established by Phase 2.
- Tool cards can render existing `RunEventDto` values with `payloadSchema=tool.lifecycle`. Phase 4 guarantees redacted public payload maps for tool name/status/policy/preview/output/error fields.
- Phase 4 explicitly deferred approval action/resume. Phase 5 must add public approval/reject contracts and a backend seam; UI-only approval state is not acceptable.
- Admin Governance in Phase 5 is inspect-only. Extension/MCP/plugin views should be empty/future-enabled placeholders backed by read-only public APIs, not static Vaadin-only data and not configuration forms.
- Playwright should use TypeScript test runner with `webServer`, `baseURL`, screenshots on failure, and trace retention on failure. Default E2E must run against fake/no-key Spring Boot test profile and deterministic data.

## Recommended Implementation Shape

1. Backend public read models first:
   - Agent Catalog DTOs/API (`/api/agents`) in `pi-agent-client` + Adapter controller.
   - Approval DTOs/API for waiting tool calls (`/api/sessions/{sessionId}/runs/{runId}/approvals/{approvalId}` or equivalent) with App service seam and audit/event behavior.
   - Admin Governance DTOs/API (`/api/admin/governance/**`) for runtime health, provider/tool registry overview, extension/MCP/plugin placeholders, policy decisions, and audit summaries.
2. Vaadin foundation:
   - Add dependencies, route layouts, route-level auth, REST/SSE client services, and shared event rendering utilities in Adapter Web.
3. User Console:
   - Chat-first landing, three-column workbench, recent sessions/history, integrated chat/event stream, cancel, Agent Catalog drawer/cards, tool cards, approval cards.
4. Admin Governance:
   - Separate route/layout, inspect-only overview and read-only detail/list views.
5. Browser E2E:
   - Deterministic no-key fake runtime profile, Playwright tests covering D-18/E2E-07, screenshots/traces for failures.

## Source Notes

- Vaadin Flow Context7: `/vaadin/flow`, security route ordering and default permit matcher patterns.
- Playwright Context7: `/microsoft/playwright`, `webServer`, screenshots, trace retention, TypeScript runner.
- Phase 4 contracts: `docs/phase-04-governed-tool-contracts.md` confirms `tool.lifecycle`, approval-required no-execution deferral, redacted summaries, and `/api/tools` readiness.
