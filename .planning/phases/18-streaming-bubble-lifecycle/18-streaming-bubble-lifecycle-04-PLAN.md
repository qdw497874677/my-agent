---
phase: 18-streaming-bubble-lifecycle
plan: 04
type: execute
wave: 4
depends_on: [18-streaming-bubble-lifecycle-03]
files_modified:
  - e2e/fixtures/fake-runtime.ts
  - e2e/phase-18-streaming-bubble-lifecycle.spec.ts
  - docs/phase-18-streaming-bubble-lifecycle.md
autonomous: true
requirements: [STRM-01, STRM-02, STRM-03, STRM-04, STRM-05]
must_haves:
  truths:
    - "Browser product-path tests can discover the Phase 18 streaming scenario with stable selectors."
    - "Fake runtime helpers expose slow stream, replay/dedupe, cancellation, and failure scenarios for future live execution."
    - "Documentation records Push/SSE-vs-polling mode, selector contracts, verification commands, and deferred boundaries."
    - "Tests assert semantic streaming signals instead of only final text."
  artifacts:
    - path: "e2e/fixtures/fake-runtime.ts"
      provides: "Phase 18 fake slow-stream/cancel/failure helper exports"
      contains: "createSlowStreamingRun"
    - path: "e2e/phase-18-streaming-bubble-lifecycle.spec.ts"
      provides: "Playwright product-path spec using stable stream selectors and no-key list gate"
      min_lines: 120
    - path: "docs/phase-18-streaming-bubble-lifecycle.md"
      provides: "Selector, stream-mode, verification, and limitation documentation"
      min_lines: 70
  key_links:
    - from: "e2e fake runtime helpers"
      to: "public session/run/event APIs"
      via: "no-key deterministic setup"
      pattern: "createSlowStreamingRun|cancelStreamingRun|createFailedStreamingRun"
    - from: "Playwright spec"
      to: "Console DOM"
      via: "data-message-role/data-stream-state/data-stream-mode selectors"
      pattern: "data-stream-state|data-stream-mode"
    - from: "docs/phase-18-streaming-bubble-lifecycle.md"
      to: "Phase 21 verification hardening"
      via: "handoff commands and semantic assertions"
      pattern: "Phase 21"
---

<objective>
Add the Phase 18 browser/product-path verification surface and handoff documentation without requiring external providers or a live server in the default plan gate.

Purpose: D-15 through D-18 require tests that prove streaming semantics—not just final text—and distinguish product streaming from fallback polling. This plan creates reusable fake-runtime helpers, a listable Playwright spec with semantic selectors, and documentation for live execution and Phase 21 hardening.

Output: Phase 18 E2E fixture/spec/docs with no-key automated gates and live-browser command handoff.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/REQUIREMENTS.md
@.planning/phases/18-streaming-bubble-lifecycle/18-CONTEXT.md
@.planning/phases/18-streaming-bubble-lifecycle/18-streaming-bubble-lifecycle-01-SUMMARY.md
@.planning/phases/18-streaming-bubble-lifecycle/18-streaming-bubble-lifecycle-02-SUMMARY.md
@.planning/phases/18-streaming-bubble-lifecycle/18-streaming-bubble-lifecycle-03-SUMMARY.md
@.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-06-SUMMARY.md
@e2e/fixtures/fake-runtime.ts
@e2e/phase-17-console-session-restore-ux.spec.ts
@docs/phase-17-console-session-restore-ux.md

<interfaces>
Existing fake-runtime helpers:
```typescript
export async function createRun(request: APIRequestContext, text: string): Promise<RuntimeRun>;
export async function createRestoredConversation(request: APIRequestContext): Promise<RestoredConversation>;
export async function cancelRun(request: APIRequestContext): Promise<RuntimeRun>;
export async function listEvents(request: APIRequestContext, sessionId: string, runId: string): Promise<RuntimeEvent[]>;
export const devHeaders = { 'X-Pi-Dev-Tenant': 'e2e-tenant', 'X-Pi-Dev-User': 'e2e-user' };
```

Selectors established by Phases 17/18:
```text
[data-role="console-panel-switcher"]
[data-action="show-console-panel"][data-console-target="sessions|run-context|chat"]
[data-message-role="user|assistant|tool|error"]
[data-session-id]
[data-run-id]
[data-message-status]
[data-stream-state="pending|streaming|completed|failed|cancelled|partial"]
[data-stream-mode="push|sse|polling-fallback"]
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Extend fake-runtime helpers for streaming scenarios</name>
  <files>e2e/fixtures/fake-runtime.ts</files>
  <behavior>
    - Test 1: `createSlowStreamingRun(...)` starts a no-key run whose prompt/hint triggers multiple model delta events and returns session/run metadata without waiting only for final text.
    - Test 2: `createFailedStreamingRun(...)` and `cancelStreamingRun(...)` expose deterministic failure/cancellation setup for browser specs.
    - Test 3: helpers keep using public APIs and `devHeaders`; they do not require provider keys or direct database access.
  </behavior>
  <action>Add exported helpers for Phase 18: `slowStreamingHint()`, `createSlowStreamingRun(request)`, `createFailedStreamingRun(request)`, and `cancelStreamingRun(request)` or equivalent names. Reuse existing `/api/sessions`, `/runs`, `/events`, `/status`, and `/cancel` helpers; do not add new test-only backend endpoints. The slow-stream helper should document/encode prompts that fake runtime recognizes for multiple deltas, replay, cancel, and failure.</action>
  <verify><automated>npm run e2e -- --help</automated></verify>
  <done>Fake-runtime exports are available for Phase 18 browser spec and future Phase 21 live execution without provider credentials.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Add Phase 18 Playwright streaming product-path spec</name>
  <files>e2e/phase-18-streaming-bubble-lifecycle.spec.ts</files>
  <behavior>
    - Test 1: spec is listable under the existing Mobile Chrome project with no webserver and no provider keys.
    - Test 2: live test body navigates `/console`, sends/continues a run, asserts `data-stream-mode` is not silently absent, sees one assistant bubble transition pending/streaming to terminal, and asserts one assistant bubble per run rather than per token.
    - Test 3: cancellation path asserts partial/cancelled state and no post-cancel appended text using `data-run-id`/`data-stream-state` selectors.
    - Test 4: failure path asserts failed state plus safe secondary error/status card and no raw runtime-event noise in main assistant prose.
  </behavior>
  <action>Create `e2e/phase-18-streaming-bubble-lifecycle.spec.ts` using established visible-control and stable-selector patterns from Phase 17. The automated plan gate should be `--list` only, but the test body must be executable when a local server is intentionally running. Use semantic assertions from D-15/D-18: pending appears, visible text changes before terminal where live timing is available, final text exact/contains expected fake chunks, all deltas land in one assistant bubble, component count does not grow per chunk, replay duplicates do not duplicate text, cancel suppresses later deltas, and failure is safe. Do not introduce screenshots or exact millisecond assertions.</action>
  <verify><automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-18-streaming-bubble-lifecycle.spec.ts --project="Mobile Chrome" --list</automated></verify>
  <done>Phase 18 Playwright spec is no-key listable and encodes live semantic streaming assertions for intentional browser execution.</done>
</task>

<task type="auto">
  <name>Task 3: Document Phase 18 selectors, stream modes, and verification handoff</name>
  <files>docs/phase-18-streaming-bubble-lifecycle.md</files>
  <action>Create a concise handoff document covering: Phase 18 scope, locked decisions D-01 through D-18, stream mode contract (`push`/`sse`/`polling-fallback`), live bubble selectors, reducer/dedupe rules, cancellation/failure semantics, automated Java gates, no-key Playwright list gate, live-browser command, and known limitation if provider-level abort is best-effort. Explicitly list deferred items from CONTEXT.md: Phase 19 multi-turn context, Phase 20 provider/model/local profile stability, broad Console cleanup, full provider-level abort where unsupported, and future conversation management/search/regeneration/RAG. Cross-link Phase 21 as the broader regression hardening owner.</action>
  <verify><automated>test $(wc -l &lt; docs/phase-18-streaming-bubble-lifecycle.md) -ge 70</automated></verify>
  <done>Docs capture selectors, commands, stream-mode semantics, and deferred boundaries for implementers and Phase 21 verification.</done>
</task>

</tasks>

<verification>
Run the no-key browser discovery and documentation gates:

```bash
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-18-streaming-bubble-lifecycle.spec.ts --project="Mobile Chrome" --list
test $(wc -l < e2e/phase-18-streaming-bubble-lifecycle.spec.ts) -ge 120
test $(wc -l < docs/phase-18-streaming-bubble-lifecycle.md) -ge 70
```
</verification>

<success_criteria>
- Browser spec and fake-runtime helpers cover pending, same-bubble delta append, dedupe/replay, terminal, cancellation, and failure semantics at selector level.
- Default automated gate remains no-key/listable and does not require external provider configuration.
- Live-server command and Phase 21 handoff are documented.
- Deferred ideas from CONTEXT.md do not appear as Phase 18 implementation tasks.
</success_criteria>

<output>
After completion, create `.planning/phases/18-streaming-bubble-lifecycle/18-streaming-bubble-lifecycle-04-SUMMARY.md`.
</output>
