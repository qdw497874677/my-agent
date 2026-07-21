# Phase 12 Console Mobile Flow

## Scope

Phase 12 turns the existing Vaadin Agent Console into a mobile-first, browser-verifiable product path without adding a mobile-only backend, replacing REST/SSE DTOs, or redesigning runtime card interiors.

Requirement traceability:

- **MCON-01:** Mobile users can open Console and immediately use the General Agent chat flow with provider/model configuration visible on the same page.
- **MCON-02:** Chat uses a bounded multi-line prompt composer and submits `Phase 12 mobile prompt\nline two\nline three` through the existing no-key Console path.
- **MCON-03:** The live event feed is a vertical mobile feed; a Vaadin poll-backed bounded replay hook appends later run events after `createRun` without another Send click, de-duplicates by sequence, and propagates terminal status to composer/run-context surfaces.
- **MCON-04:** A successful send creates a real active session identity that is shown through the active-session banner and reused by follow-up sends.
- **MCON-05:** Cancellation is reachable through a primary composer control when a run is cancellable, with composer/run-status feedback and tolerant terminal-state fallback.
- **MVER-03:** `e2e/phase-12-console-mobile-flow.spec.ts` is the browser-visible fake/no-key mobile Console gate for Mobile Chrome, Mobile Safari/WebKit proxy, and Tablet.

## Selector Contract

Use stable `data-*` hooks only. Do not target Vaadin generated IDs or shadow-DOM internals.

Core mobile Console hooks:

- `[data-layout="chat-home"]`
- `[data-role="model-selector"]`
- `[data-role="provider-status"]`
- `[data-console-panel="chat"][data-console-panel-active="true"]`
- `[data-role="active-session-banner"][data-active-session-state="new|continued"]`
- `[data-action="show-console-panel"]` must be absent from the user-facing Console.
- `[data-console-panel="agents"], [data-console-panel="sessions"], [data-console-panel="run-context"]` must be absent from the user-facing Console.

Chat/run hooks:

- `[data-role="event-feed"]`
- `[data-event-category]`, `[data-event-type]`, or `[data-run-event]` inside the feed for appended run events
- `[data-role="chat-composer"]`
- `[data-role="chat-input"]`
- `[data-role="composer-run-status"]`
- `[data-action="send-chat"]`
- `[data-action="cancel-run-primary"]`
- `[data-action="cancel-run"]`

Tool/approval reachability hooks for MVER-03:

- `[data-event-category="tool"]`
- `[data-event-category="approval"]`
- `[data-panel="approvals"]` as an allowed fallback surface when card events cannot be emitted deterministically.

Desktop regression hooks preserved by `e2e/phase-05-web-console.spec.ts`:

- `[data-layout="chat-home"]`
- `[data-role="model-selector"]`
- `[data-role="provider-status"]`
- `[data-column="chat-event-stream"]`
- `[data-action="show-console-panel"]` must be absent.

## Verification Commands

Java quick contract command:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test
```

Playwright list command:

```bash
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome" --list
```

Full mobile/tablet matrix:

```bash
npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome" --project="Mobile Safari" --project="Tablet"
```

Desktop Console regression:

```bash
npm run e2e -- e2e/phase-05-web-console.spec.ts --project="chromium"
```

## Final MVER-03 Evidence Contract

The Phase 12 mobile spec no longer accepts a one-shot replay as the main product path. It records the browser-visible feed event count immediately after Send, waits for the live/bounded replay path to increase that count without clicking Send again, and verifies the active-session banner represents a real continued conversation identity for follow-up sends.

The local no-key Java contract gate covers the same behavior without a browser: `ConsoleView.refreshActiveRunEvents()` consumes `ConsoleRunExecutionBridge.listEvents(...)` using `nextAfterSequence`, ignores duplicate replayed sequences, and applies terminal feedback to both run-status surfaces. The Vaadin view wires this hook to UI polling so browser runs can observe post-createRun append behavior through existing REST/SSE DTO seams only.

## Desktop Regression

Phase 12 explicitly preserves desktop Console coverage rather than deferring it to Phase 15. The Phase 05 Playwright regression now checks the chat-only home layout, provider/model configuration, chat/event stream column, chat input, and send action before continuing through the existing API-level no-key catalog, run, session, tool, approval, and cancellation assertions.

This ensures the mobile-first composer work remains compatible with the simplified chat-first desktop contract.

## Deferred Handoffs

- **Phase 13:** Runtime event, tool, and approval interiors are implemented by Phase 13; see `docs/phase-13-runtime-cards.md` for the selector contract, redaction/detail rules, approval UX, and verification commands. Phase 15 still owns real-device/orientation/accessibility hardening.
- **Phase 15:** Real-device Android/iOS browser UAT, orientation sweeps, final keyboard/viewport chrome checks, and deeper accessibility hardening remain Phase 15. Phase 12 uses Playwright Mobile Chrome, Mobile Safari/WebKit proxy, and Tablet projects as deterministic CI-friendly gates; local WebKit/dev-mode host dependency issues remain environment-only and do not change the selector contract.
