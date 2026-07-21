# Phase 18 Streaming Bubble Lifecycle

Phase 18 turns run/model events into a live Console assistant answer. The user-facing contract is a single assistant bubble per run: it appears pending after run creation, receives ordered model deltas, and ends as completed, failed, cancelled, or partial without raw runtime noise in the main prose.

## Scope

- Product streaming for the Vaadin Console conversation flow.
- Reducer-owned pending, delta, terminal, replay, dedupe, cancellation, and failure semantics.
- Stable selector contracts for Java and Playwright verification.
- No-key browser discovery and fake-runtime helpers for future live execution.

Out of scope remains broad Console cleanup, Phase 19 multi-turn context, Phase 20 provider/model/local profile stabilization, full conversation management, and provider fallback policy.

## Locked Decisions D-01 through D-18

- D-01 to D-03: product streaming must be Push or SSE first; polling is only an explicit fallback/test seam.
- D-04 to D-08: assistant aggregation is keyed by session/run/step, starts after run creation, appends only non-empty `model.delta` text, treats terminal events as state transitions, and dedupes by event identity/sequence.
- D-09 to D-12: cancellation must use runtime seams plus local reducer stop, preserve partial text, suppress later deltas, and render failures through safe summaries/cards.
- D-13 to D-14: delta coalescing may be bounded by implementation choice, and terminal states force-flush buffered text.
- D-15 to D-18: verification must assert streaming semantics, not only final text; Phase 17 carry-over fixes stay narrow; fake slow stream, replay, cancel, failure, selectors, and product-path browser coverage are required.

## Stream Mode Contract

Every primary assistant bubble rendered by the live stream surface exposes `data-stream-mode`:

| Mode | Meaning | Test Expectation |
| --- | --- | --- |
| `push` | Vaadin Push/UI.access subscription is the active product path. | Preferred live Console mode. |
| `sse` | Explicit browser/server-sent event subscription is the active product path. | Equivalent real-streaming product mode if enabled. |
| `polling-fallback` | Bounded replay/polling is in use. | Allowed only when explicitly labeled; tests must not confuse this with push/SSE. |

The selector is intentionally semantic and language-neutral. Visible labels are localized through the Vaadin resource bundles.

## Live Bubble Selector Contract

Primary assistant bubbles should be discoverable with:

```text
[data-message-role="assistant"]
[data-message-kind="primary-bubble"]
[data-session-id]
[data-run-id]
[data-message-status]
[data-stream-state="pending|streaming|completed|failed|cancelled|partial"]
[data-stream-mode="push|sse|polling-fallback"]
```

Related Console controls used by product-path tests:

```text
[data-layout="chat-home"]
[data-role="model-selector"]
[data-role="provider-status"]
[data-console-panel="chat"][data-console-panel-active="true"]
[data-action="show-console-panel"] must be absent from the user-facing Console
[data-role="chat-input"]
[data-action="send-chat"]
[data-action="cancel-run-primary"]
[data-action="cancel-run"]
[data-role="active-session-banner"][data-active-session-state="new|continued"]
```

Secondary operational cards may use `data-message-kind="secondary-card"` and must remain separate from primary assistant prose.

## Reducer and Dedupe Rules

- Begin a pending assistant bubble only after a session/run identity exists.
- Route `model.delta` text-like payload fields into the reducer-owned assistant bubble.
- Keep tool, policy, approval, audit, provider diagnostic, and runtime lifecycle details out of assistant prose.
- Mark terminal events by mutating the existing bubble state rather than creating blank terminal messages.
- Deduplicate replay/push/poll events by event id and sequence. Text-content dedupe is insufficient because repeated words can be valid model output.
- Clear live reducer state when replacing transcript history or starting a new selected conversation.

## Cancellation Semantics

- Console cancel calls the runtime cancellation seam.
- The reducer stops the active run locally before waiting for the backend response.
- Already-generated text remains visible as partial/cancelled output.
- Late provider deltas for the same stopped run are ignored.
- User cancellation is not presented as generic model failure.

Provider-level abort may be best-effort for adapters that cannot terminate an upstream stream. Phase 18 still enforces runtime/UI suppression and cancelled terminal state; full provider-specific abort support is a future adapter hardening item where unsupported.

## Failure Semantics

- Runtime/provider failures mark the assistant bubble `data-stream-state="failed"`.
- The visible failure summary prefers public fields such as `message`, `reason`, `status`, `errorCategory`, or `category`.
- Raw exception bodies, stack traces, secret-looking keys, provider payload dumps, and tokens must not appear in assistant prose.
- A secondary status/error card may appear when it is redacted and summarized safely.

## Fake Runtime Helpers

`e2e/fixtures/fake-runtime.ts` exports Phase 18 helpers:

- `slowStreamingHint()`
- `createSlowStreamingRun(request)`
- `createFailedStreamingRun(request)`
- `cancelStreamingRun(request)`

These helpers use public `/api/sessions`, `/runs`, `/events`, `/status`, and `/cancel` APIs with `devHeaders`. They do not require provider keys, test-only backend endpoints, or direct database access.

## Automated Java Gates

Representative focused gates from Phase 18 implementation plans:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleStreamingBubbleLifecycleTest test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleLiveStreamingPushTest test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleStreamingCancellationTest test
```

Use the combined test set during broader regression hardening:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleStreamingCancellationTest,WebConsoleStreamingBubbleLifecycleTest,WebConsoleLiveStreamingPushTest test
```

## No-Key Playwright List Gate

The default browser gate is discovery-only and does not start a server or require provider credentials:

```bash
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-18-streaming-bubble-lifecycle.spec.ts --project="Mobile Chrome" --list
```

The spec is intentionally listable in CI and encodes live assertions for deliberate local execution.

## Live Browser Command

When a local server is intentionally available through the normal Playwright webserver path, run:

```bash
npm run e2e -- e2e/phase-18-streaming-bubble-lifecycle.spec.ts --project="Mobile Chrome"
```

If an external server is already running, set `PLAYWRIGHT_BASE_URL` and `PLAYWRIGHT_SKIP_WEBSERVER=1` explicitly:

```bash
PLAYWRIGHT_BASE_URL=http://127.0.0.1:18080 PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-18-streaming-bubble-lifecycle.spec.ts --project="Mobile Chrome"
```

## Phase 21 Handoff

Phase 21 owns broader regression hardening. It should reuse the Phase 18 Java gates, the no-key Playwright list gate, and the live browser command above to cover push/SSE-vs-polling mode, one-bubble-per-run grouping, replay dedupe, cancellation suppression, and safe failure rendering.

## Deferred Boundaries

- Phase 19 multi-turn runtime context assembly and model history injection.
- Phase 20 provider/model readiness, model selection persistence, per-run model/fallback metadata, and SQLite restart persistence.
- Broad Console cleanup beyond narrow selector/control fixes needed for streaming verification.
- Full provider-level stream abort where an adapter cannot support abort through current runtime/provider seams.
- Conversation search, rename, archive, pin, delete, branching, editing, regeneration, import/export, prompt libraries, long-term memory, RAG, vector DB, and automatic paid-provider fallback.
