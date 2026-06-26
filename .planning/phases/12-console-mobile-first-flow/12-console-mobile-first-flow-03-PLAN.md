---
phase: 12-console-mobile-first-flow
plan: 03
type: execute
wave: 3
depends_on: [12-console-mobile-first-flow-01, 12-console-mobile-first-flow-02]
files_modified:
  - e2e/phase-12-console-mobile-flow.spec.ts
  - e2e/fixtures/fake-runtime.ts
  - e2e/phase-05-web-console.spec.ts
  - docs/phase-12-console-mobile-flow.md
  - docs/phase-11-responsive-shell.md
autonomous: true
requirements: [MCON-01, MCON-02, MCON-03, MCON-04, MCON-05, MVER-03]
must_haves:
  truths:
    - "Maintainer can run a deterministic no-key mobile Console E2E that starts or continues General Agent chat flow."
    - "Browser test proves multi-line prompt submission, visible event feed progression, session surface use, run-context reachability, and cancel or terminal status."
    - "Browser test proves tool/approval areas remain reachable in the mobile Console feed without redesigning Phase 13 card interiors."
    - "Browser test proves scrolling prior events does not remove practical access to the composer and primary Cancel while a run is cancellable, or terminal status stays visible."
    - "Representative Mobile Chrome, Mobile Safari/WebKit proxy, and Tablet projects list and run the Phase 12 spec."
    - "Desktop Console regression remains covered after the mobile-first Console refactor."
    - "Developer documentation records selectors, verification commands, and Phase 13/15 handoffs."
  artifacts:
    - path: "e2e/phase-12-console-mobile-flow.spec.ts"
      provides: "MVER-03 mobile Console product-path browser gate"
    - path: "e2e/fixtures/fake-runtime.ts"
      provides: "Reusable deterministic fake runtime helper additions for browser-visible cancellable runs and test-only tool/approval event emission"
    - path: "e2e/phase-05-web-console.spec.ts"
      provides: "Updated desktop Console regression expectations for preserved workbench behavior"
    - path: "docs/phase-12-console-mobile-flow.md"
      provides: "Phase 12 selector/verification/handoff documentation"
    - path: "docs/phase-11-responsive-shell.md"
      provides: "Handoff note that Console mobile flow was implemented in Phase 12"
  key_links:
    - from: "e2e/phase-12-console-mobile-flow.spec.ts"
      to: "ConsoleView/ChatEventStreamPanel hooks"
      via: "stable selectors data-console-panel, data-role=chat-input, data-role=event-feed, data-action=send-chat"
      pattern: "data-console-panel|event-feed|send-chat"
    - from: "e2e/phase-12-console-mobile-flow.spec.ts"
      to: "e2e/fixtures/fake-runtime.ts"
      via: "dev headers/fake runtime helper for no-key run and cancel/terminal state"
      pattern: "fakeRuntime|cancelRun|waitForTerminal"
    - from: "docs/phase-12-console-mobile-flow.md"
      to: "ROADMAP Phase 12 requirements"
      via: "documents MCON-01..MCON-05 and MVER-03 commands"
      pattern: "MVER-03"
---

<objective>
Add the browser-visible Phase 12 product-path gate, preserve desktop Console regression, and document the mobile Console flow contract.

Purpose: Satisfy MVER-03 and validate that Plans 01-02 produce a real, deterministic, no-key mobile Console workflow across the required representative project matrix.

Output: Playwright spec, any minimal fake-runtime fixture support, updated desktop regression expectations, and documentation.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/REQUIREMENTS.md
@.planning/phases/12-console-mobile-first-flow/12-CONTEXT.md
@.planning/phases/12-console-mobile-first-flow/12-RESEARCH.md
@.planning/phases/12-console-mobile-first-flow/12-VALIDATION.md
@.planning/phases/12-console-mobile-first-flow/12-console-mobile-first-flow-01-SUMMARY.md
@.planning/phases/12-console-mobile-first-flow/12-console-mobile-first-flow-02-SUMMARY.md
@e2e/fixtures/mobile-smoke.ts
@e2e/fixtures/fake-runtime.ts
@e2e/phase-05-web-console.spec.ts
@e2e/phase-10-mobile-route-smoke.spec.ts
@e2e/phase-11-shell-navigation.spec.ts
@docs/phase-10-mobile-baseline.md
@docs/phase-11-responsive-shell.md

<interfaces>
Expected Phase 12 selectors from Plans 01-02:
```text
[data-console-panel="chat|agents|sessions|run-context"]
[data-console-panel-active="true"]
[data-action="show-console-panel"][data-console-target="agents|sessions|run-context|chat"]
[data-agent-id="cloud-general-agent"] [data-primary-action^="general-agent-"]
[data-role="session-card"][data-session-active="true"]
[data-role="event-feed"]
[data-role="chat-composer"]
[data-role="chat-input"]
[data-role="composer-run-status"]
[data-action="send-chat"]
[data-action="cancel-run-primary"]
[data-action="cancel-run"]
```

Existing helpers from Phase 10-11 to reuse:
```ts
expectNoPageHorizontalOverflow(page)
expectTapTargetAtLeast(locator, 44, label)
expectFocusVisible(page, locator)
```
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Add Phase 12 mobile Console product-path Playwright gate</name>
  <files>e2e/phase-12-console-mobile-flow.spec.ts, e2e/fixtures/fake-runtime.ts</files>
  <read_first>
    - .planning/phases/12-console-mobile-first-flow/12-CONTEXT.md lines 41-47 for D-16 through D-20.
    - .planning/phases/12-console-mobile-first-flow/12-RESEARCH.md lines 259-275 and 432-457 for E2E selector and validation shape.
    - .planning/phases/12-console-mobile-first-flow/12-VALIDATION.md lines 16-35 for required commands.
    - e2e/fixtures/mobile-smoke.ts.
    - e2e/fixtures/fake-runtime.ts.
    - e2e/phase-05-web-console.spec.ts.
    - e2e/phase-10-mobile-route-smoke.spec.ts.
    - e2e/phase-11-shell-navigation.spec.ts.
  </read_first>
  <behavior>
    - Test 1: Mobile Chrome opens `/console`, sees `[data-console-panel="chat"][data-console-panel-active="true"]`, no page-level horizontal overflow, and visible `[data-role="chat-composer"]`.
    - Test 2: test opens Agents via `[data-action="show-console-panel"][data-console-target="agents"]`, locates `[data-agent-id="cloud-general-agent"]`, clicks `[data-primary-action^="general-agent-"]`, then returns/observes Chat active.
    - Test 3: test fills `[data-role="chat-input"]` with exact multi-line text `Phase 12 mobile prompt\nline two\nline three`, clicks `[data-action="send-chat"]`, and expects `[data-role="event-feed"]` or `[data-role="composer-run-status"]` to contain `/running|queued|model|completed|cancel/i`.
    - Test 4: test opens Sessions and Run Context panels, verifies `[data-role="session-card"]` or an empty session state is reachable, verifies backup `[data-action="cancel-run"]` reachability when cancellable, then attempts `[data-action="cancel-run-primary"]` if visible or accepts terminal status per D-16/D-18.
    - Test 5 (MVER-03 tool/approval reachability): the deterministic fake/no-key run is configured via a test-only fake-runtime hint (e.g. `mobileToolApprovalHint()` dev header or prompt fixture) to emit at least one browser-visible tool-call event and one approval event into `[data-role="event-feed"]`; the test then asserts tool/approval reachability without redesigning Phase 13 card interiors by locating existing stable hooks `[data-event-category="tool"]` and `[data-event-category="approval"]` (from `ToolCallCard`/`ApprovalCard`) inside the feed. The assertion is tolerant: if both cannot be emitted deterministically, at least one of `[data-event-category="tool"]` or `[data-event-category="approval"]` must be reachable, or `[data-panel="approvals"]`/Run Context must be openable. No assertion is made on card interiors, dialogs, or risk UX.
    - Test 6 (D-18/MCON-03 scroll-with-controls): after the feed shows streamed events, the test scrolls `[data-role="event-feed"]` (or the page) through prior events using Playwright `mouse.wheel` / `locator.scrollIntoViewIfNeeded()` / `page.evaluate`, then asserts that `[data-role="chat-composer"]` remains visible or practically reachable, that `[data-action="cancel-run-primary"]` remains visible/reachable while the run is cancellable or terminal/completed status is visible in `[data-role="composer-run-status"]` / `[data-role="event-feed"]`, and that the feed shows meaningful progression beyond a single static match.
    - Test 7: spec is tagged or structured so it can run on Mobile Chrome, Mobile Safari, and Tablet; Mobile Firefox is not required for the full SSE path.
  </behavior>
  <action>
    Create `e2e/phase-12-console-mobile-flow.spec.ts` using `@playwright/test` and existing helper imports from `e2e/fixtures/mobile-smoke.ts` and `e2e/fixtures/fake-runtime.ts`. Use stable `data-*` selectors only; do not target Vaadin generated shadow DOM. Include one main test named exactly `mobile console user can browse agent, send prompt, observe stream, inspect sessions, and cancel or finish run`. At start, call existing fake-runtime/dev-header setup patterns from `phase-05-web-console.spec.ts`; if `fake-runtime.ts` lacks a browser-visible slow/cancellable helper, add a minimal helper such as `mobileConsoleDevHeaders()` or `createCancellableRunHint()` that only sets test/dev headers or prompt text and does not add production runtime capability. Use the prompt text `Phase 12 mobile prompt\nline two\nline three`. Assert no page horizontal overflow after opening Chat, Agents, Sessions, and Run Context. Assert tap target size for `[data-action="send-chat"]` and, if visible, `[data-action="cancel-run-primary"]` with `expectTapTargetAtLeast(..., 44, ...)`. For cancellation, implement deterministic tolerant logic: if primary cancel is visible within a short timeout, click it and expect `/cancelling|cancelled|terminal|completed/i`; otherwise require terminal/completed status in `[data-role="composer-run-status"]` or `[data-role="event-feed"]`. For MVER-03 tool/approval reachability, add a test-only fake-runtime helper `mobileToolApprovalHint()` (or extend `createCancellableRunHint`) in `e2e/fixtures/fake-runtime.ts` that emits at least one tool-call event and one approval event into the feed via test/dev headers or prompt fixture only — no production runtime/model/tool capability changes; then in the spec assert, inside `[data-role="event-feed"]`, that at least one of the existing stable hooks `[data-event-category="tool"]` or `[data-event-category="approval"]` is reachable (or that `[data-panel="approvals"]` / Run Context is openable). Add an inline comment marker exactly `// MVER-03 tool/approval reachability` above this assertion. Do not assert Phase 13 tool/approval card interiors, dialogs, or risk UX. For D-18/MCON-03, after the feed shows streamed events, scroll `[data-role="event-feed"]` or the page through prior events using `mouse.wheel` / `scrollIntoViewIfNeeded()` / `page.evaluate`, then assert `[data-role="chat-composer"]` remains visible or practically reachable and that `[data-action="cancel-run-primary"]` remains visible/reachable while cancellable, or terminal/completed status is visible; also assert the feed text/child count shows meaningful progression beyond one static match. Add a `test.skip` or conditional only for explicitly unsupported project/browser behavior already present in config; do not skip Mobile Chrome, Mobile Safari, or Tablet broadly.
  </action>
  <verify>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome" --list</automated>
    <automated>npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome" --project="Mobile Safari" --project="Tablet"</automated>
  </verify>
  <acceptance_criteria>
    - `test -f e2e/phase-12-console-mobile-flow.spec.ts` succeeds.
    - `grep -R "mobile console user can browse agent, send prompt, observe stream, inspect sessions, and cancel or finish run" e2e/phase-12-console-mobile-flow.spec.ts` finds the main MVER-03 test.
    - `grep -R "Phase 12 mobile prompt" e2e/phase-12-console-mobile-flow.spec.ts` finds the multi-line prompt fixture.
    - `grep -R "data-console-target=\"agents\"\|data-console-target.*agents\|event-feed\|cancel-run-primary" e2e/phase-12-console-mobile-flow.spec.ts` finds core selector assertions.
    - `grep -R "MVER-03 tool/approval reachability" e2e/phase-12-console-mobile-flow.spec.ts` finds the labeled tool/approval reachability assertion.
    - `grep -R "data-event-category=\"tool\"\|data-event-category=\"approval\"\|data-panel=\"approvals\"" e2e/phase-12-console-mobile-flow.spec.ts` finds tool/approval reachability selectors.
    - `grep -R "mobileToolApprovalHint\|ToolApprovalHint" e2e/fixtures/fake-runtime.ts` finds the test-only tool/approval event fixture support.
    - `grep -R "scroll" e2e/phase-12-console-mobile-flow.spec.ts` finds the D-18/MCON-03 mobile scroll assertion.
    - `grep -R "chat-composer" e2e/phase-12-console-mobile-flow.spec.ts` finds the composer-still-reachable assertion.
    - Playwright list command passes; full matrix command passes or any local browser/dev-server limitation is recorded in the plan summary without weakening the committed spec.
  </acceptance_criteria>
  <done>Phase 12 has a deterministic no-key mobile Console E2E proving browse/select, multi-line send, streamed/terminal UI, sessions/run-context reachability, tool/approval reachability in the feed, scroll-safe composer/cancel access, and cancel-or-terminal behavior across required projects.</done>
</task>

<task type="auto" tdd="false">
  <name>Task 2: Preserve desktop Console regression and document Phase 12 flow</name>
  <files>e2e/phase-05-web-console.spec.ts, docs/phase-12-console-mobile-flow.md, docs/phase-11-responsive-shell.md</files>
  <read_first>
    - .planning/phases/12-console-mobile-first-flow/12-CONTEXT.md lines 44-45 for D-19 desktop regression preservation.
    - .planning/phases/12-console-mobile-first-flow/12-VALIDATION.md lines 53-58 for docs Wave 0 requirement.
    - e2e/phase-05-web-console.spec.ts.
    - docs/phase-11-responsive-shell.md.
    - docs/phase-10-mobile-baseline.md.
  </read_first>
  <action>
    Update `e2e/phase-05-web-console.spec.ts` only where necessary to preserve desktop Console regression after new Phase 12 hooks: assert `data-layout="three-column-workbench"`, availability of `[data-column="sessions"]`, `[data-column="chat-event-stream"]`, and `[data-column="run-context"]`, and preserve existing no-key Console product assertions. Do not remove existing Phase 05 run/session/tool/approval assertions unless selector names changed; update them to the new stable Phase 12 hooks if needed. Create `docs/phase-12-console-mobile-flow.md` with sections exactly `Scope`, `Selector Contract`, `Verification Commands`, `Desktop Regression`, and `Deferred Handoffs`. Document MCON-01 through MCON-05 and MVER-03; list commands from VALIDATION.md: Java quick command, Playwright list command, full Mobile Chrome/Mobile Safari/Tablet matrix, and desktop `phase-05-web-console.spec.ts --project="chromium"`. In `Deferred Handoffs`, explicitly state that runtime/tool/approval card interiors remain Phase 13 and real-device/orientation/final accessibility hardening remains Phase 15. Append a short Phase 12 handoff note to `docs/phase-11-responsive-shell.md` replacing the Phase 12 deferred bullet with "Implemented in Phase 12; see docs/phase-12-console-mobile-flow.md" while keeping Phase 13-15 deferred bullets.
  </action>
  <verify>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome" --list</automated>
    <automated>npm run e2e -- e2e/phase-05-web-console.spec.ts --project="chromium"</automated>
  </verify>
  <acceptance_criteria>
    - `test -f docs/phase-12-console-mobile-flow.md` succeeds.
    - `grep -R "MCON-01" docs/phase-12-console-mobile-flow.md` and `grep -R "MVER-03" docs/phase-12-console-mobile-flow.md` find requirement traceability.
    - `grep -R "data-layout=\"three-column-workbench\"\|three-column-workbench" e2e/phase-05-web-console.spec.ts` finds desktop regression coverage.
    - `grep -R "Phase 13" docs/phase-12-console-mobile-flow.md` and `grep -R "Phase 15" docs/phase-12-console-mobile-flow.md` find deferred handoffs.
    - Playwright list command passes; desktop Chromium command passes or local Vaadin/browser startup limitation is recorded in the plan summary.
  </acceptance_criteria>
  <done>Desktop Console regression remains explicit and Phase 12 mobile Console selectors, commands, and deferred boundaries are documented for verification and downstream phases.</done>
</task>

</tasks>

<verification>
Phase gate commands:
1. `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleUserFlowTest,WebConsoleMobileFlowContractTest test`
2. `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome" --list`
3. `npm run e2e -- e2e/phase-12-console-mobile-flow.spec.ts --project="Mobile Chrome" --project="Mobile Safari" --project="Tablet"`
4. `npm run e2e -- e2e/phase-05-web-console.spec.ts --project="chromium"`
</verification>

<success_criteria>
- MVER-03: New browser E2E starts a fake/no-key Console flow, observes streamed/terminal UI, opens agent/session/run-context surfaces, verifies tool/approval area reachability in the feed, and cancels or reaches terminal status.
- MCON-03/D-18: Browser E2E scrolls prior events and proves the composer and primary Cancel remain practically reachable while cancellable, or terminal status stays visible.
- MCON-01 through MCON-05 are represented in a real browser path, not only Java contract tests.
- D-16 through D-20 are implemented through deterministic fake-runtime/public API/stable selector Playwright patterns.
- Desktop Console regression is updated instead of deferred to Phase 15.
</success_criteria>

<output>
After completion, create `.planning/phases/12-console-mobile-first-flow/12-console-mobile-first-flow-03-SUMMARY.md`.
</output>
