# Phase 12 Console Mobile Flow

## Scope

Phase 12 turns the existing Vaadin Agent Console into a mobile-first, browser-verifiable product path without adding a mobile-only backend, replacing REST/SSE DTOs, or redesigning runtime card interiors.

Requirement traceability:

- **MCON-01:** Mobile users can open Console, switch to Agents, find `cloud-general-agent`, start or continue the General Agent flow, and return to Chat.
- **MCON-02:** Chat uses a bounded multi-line prompt composer and submits `Phase 12 mobile prompt\nline two\nline three` through the existing no-key Console path.
- **MCON-03:** The live event feed is a vertical mobile feed; the Phase 12 E2E scrolls prior events and verifies composer/cancel or terminal status remains practically reachable.
- **MCON-04:** Sessions are reachable through the mobile Console panel contract, with active session cards or empty session state exposed through stable hooks.
- **MCON-05:** Cancellation is reachable through a primary composer control when a run is cancellable, with a run-context backup action/status surface and tolerant terminal-state fallback.
- **MVER-03:** `e2e/phase-12-console-mobile-flow.spec.ts` is the browser-visible fake/no-key mobile Console gate for Mobile Chrome, Mobile Safari/WebKit proxy, and Tablet.

## Selector Contract

Use stable `data-*` hooks only. Do not target Vaadin generated IDs or shadow-DOM internals.

Core mobile Console panel hooks:

- `[data-console-panel="chat|agents|sessions|run-context"]`
- `[data-console-panel-active="true"]`
- `[data-action="show-console-panel"][data-console-target="agents|sessions|run-context|chat"]`
- `[data-agent-id="cloud-general-agent"] [data-primary-action^="general-agent-"]`
- `[data-role="session-card"][data-session-active="true"]`

Chat/run hooks:

- `[data-role="event-feed"]`
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

- `[data-layout="three-column-workbench"]`
- `[data-column="sessions"]`
- `[data-column="chat-event-stream"]`
- `[data-column="run-context"]`

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

## Desktop Regression

Phase 12 explicitly preserves desktop Console coverage rather than deferring it to Phase 15. The Phase 05 Playwright regression now checks the three-column workbench layout, sessions column, chat/event stream column, run-context column, chat input, and send action before continuing through the existing API-level no-key catalog, run, session, tool, approval, and cancellation assertions.

This ensures the mobile-first panel state and sticky composer work remains additive to the desktop workbench contract.

## Deferred Handoffs

- **Phase 13:** Runtime event card interiors, tool card interiors, approval card interiors, approval dialogs, risk presentation, and detailed tool/approval UX remain Phase 13. Phase 12 only verifies that tool/approval areas are reachable in the mobile feed or approved fallback surfaces.
- **Phase 15:** Real-device Android/iOS browser UAT, orientation sweeps, final keyboard/viewport chrome checks, and deeper accessibility hardening remain Phase 15. Phase 12 uses Playwright Mobile Chrome, Mobile Safari/WebKit proxy, and Tablet projects as deterministic CI-friendly gates.
