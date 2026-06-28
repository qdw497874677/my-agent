---
phase: 17-console-session-restore-ux
plan: 04
type: execute
wave: 3
depends_on:
  - 17-console-session-restore-ux-03
files_modified:
  - pi-agent-adapter-web/src/main/resources/messages.properties
  - pi-agent-adapter-web/src/main/resources/messages_zh.properties
  - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionRestoreUxTest.java
  - e2e/phase-17-console-session-restore-ux.spec.ts
  - e2e/fixtures/fake-runtime.ts
  - docs/phase-17-console-session-restore-ux.md
autonomous: true
requirements:
  - CIA-01
  - CIA-02
  - CIA-03
  - CIA-04
  - SESS-02
  - SESS-03
must_haves:
  truths:
    - "Browser product path proves recent history, session selection, restored bubbles, continued identity, and same-session follow-up selectors."
    - "English and Chinese labels cover History, New conversation, Continue, session empty/status, and abnormal transcript statuses."
    - "Advanced runtime/tool/provider details remain secondary/collapsed/reachable and are not rendered as raw runtime-event noise in the main chat path."
  artifacts:
    - path: "e2e/phase-17-console-session-restore-ux.spec.ts"
      provides: "Playwright product-path gate for Console session restore UX"
      min_lines: 80
    - path: "pi-agent-adapter-web/src/main/resources/messages.properties"
      provides: "English i18n labels for Phase 17 Console restore UX"
      contains: "console.session"
    - path: "pi-agent-adapter-web/src/main/resources/messages_zh.properties"
      provides: "Chinese i18n labels for Phase 17 Console restore UX"
      contains: "console.session"
    - path: "docs/phase-17-console-session-restore-ux.md"
      provides: "Selector/verification/deferred-boundary handoff documentation"
      min_lines: 40
  key_links:
    - from: "e2e/phase-17-console-session-restore-ux.spec.ts"
      to: "Console restore selectors"
      via: "data-role=session-card, data-message-role, data-active-session-state"
      pattern: "data-message-role"
    - from: "messages.properties/messages_zh.properties"
      to: "ConsoleView/SessionListPanel/ChatEventStreamPanel"
      via: "Vaadin getTranslation keys"
      pattern: "console.session"
---

<objective>
Add final product-path verification, i18n, and handoff documentation for Phase 17.

Purpose: Phase 17 should ship with deterministic no-key evidence that the Console behaves like a chat product for recent session restore and continuation, while preserving deferred boundaries for streaming, multi-turn context, provider/model stability, and future management features.

Output: Playwright listable restore spec, synchronized English/Chinese labels, and Phase 17 selector/deferred-boundary docs.
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
@.planning/phases/17-console-session-restore-ux/17-CONTEXT.md
@.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-cross-browser-orientation-accessibility-and-release-hardening-04-SUMMARY.md
@pi-agent-adapter-web/src/main/resources/messages.properties
@pi-agent-adapter-web/src/main/resources/messages_zh.properties
@e2e/phase-12-console-mobile-flow.spec.ts
@e2e/fixtures/fake-runtime.ts

<interfaces>
Selectors expected from prior Phase 17 plans:
```text
[data-role="session-card"][data-session-id][data-session-active]
[data-field="session-title"|"session-preview"|"session-updated-at"|"session-status"]
[data-role="active-session-banner"][data-active-session-state="new|continued"]
[data-action="new-conversation"]
[data-message-role="user|assistant|tool|error"][data-session-id][data-run-id][data-message-status][data-stream-state]
```

Use existing Playwright patterns from Phase 12/15: stable selectors, no screenshots, fake-runtime/no-key paths, route load + product-path assertions.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Synchronize Phase 17 Console labels and status copy</name>
  <files>pi-agent-adapter-web/src/main/resources/messages.properties, pi-agent-adapter-web/src/main/resources/messages_zh.properties, pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSessionRestoreUxTest.java</files>
  <behavior>
    - Test 1: English and Chinese bundles both contain keys for History, New conversation, Continue title prefix, New Conversation action, session empty state, and abnormal transcript statuses.
    - Test 2: component tests reference translated labels rather than hardcoded English-only copy where practical.
  </behavior>
  <action>Add synchronized i18n keys for Phase 17 labels: History/Sessions rail wording, `New conversation`, `Continue: {0}`, New Conversation action, restored transcript empty state, failed/cancelled/partial status badges, and compact advanced/details wording if needed. Keep provider/model readiness wording changes minimal because Phase 20 owns full provider/model stability. Do not introduce search/rename/archive/pin/delete copy because those are deferred.</action>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleSessionRestoreUxTest test</automated>
  </verify>
  <done>Both locale bundles contain synchronized Phase 17 Console restore labels with no deferred feature copy.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Add deterministic Playwright restore product-path gate</name>
  <files>e2e/phase-17-console-session-restore-ux.spec.ts, e2e/fixtures/fake-runtime.ts</files>
  <behavior>
    - Test 1: no-key browser path can create or rely on fake-runtime conversation data, visit `/console`, and see recent session cards.
    - Test 2: selecting a session returns to Chat, shows `Continue:` banner, and restores prior user/assistant bubbles with `data-message-role` and `data-session-id` selectors.
    - Test 3: sending a follow-up keeps the active session identity and asserts the visible session card/session id does not change to a new session.
    - Test 4: the main chat path does not render raw runtime-event noise as equal transcript prose; tool/error details remain secondary/reachable if present.
  </behavior>
  <action>Create `e2e/phase-17-console-session-restore-ux.spec.ts` using existing Phase 12/15 Playwright style: stable selectors, no screenshots, no real provider keys, and tolerant timing. If fixture helpers need an API-created session/run before visiting `/console`, add a narrow helper to `e2e/fixtures/fake-runtime.ts` that creates a conversation with at least one user prompt and terminal assistant/fallback event, then returns `sessionId`. Assert selectors and order, not only text (D-12 and pitfalls #17). Keep the automated command listable/no-key by using `--list`; do not require live browser execution unless the existing harness already supports it reliably.</action>
  <verify>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-17-console-session-restore-ux.spec.ts --project="Mobile Chrome" --list</automated>
  </verify>
  <done>Phase 17 browser spec is discoverable/listable and encodes the restore/continue product path with stable selectors.</done>
</task>

<task type="auto">
  <name>Task 3: Document selector contract, verification commands, and deferred boundaries</name>
  <files>docs/phase-17-console-session-restore-ux.md</files>
  <action>Create concise Phase 17 documentation listing: (1) Console restore UX selectors, (2) component and Playwright verification commands, (3) what Phase 17 implemented, (4) explicit handoffs to Phase 18 streaming bubble lifecycle, Phase 19 multi-turn context, Phase 20 provider/model/local profile stability, and Phase 21 broader regression hardening. Mention that search/rename/archive/pin/delete and localStorage history remain out of scope per deferred decisions. This is product/verification documentation only; do not modify implementation from this task.</action>
  <verify>
    <automated>test -f docs/phase-17-console-session-restore-ux.md &amp;&amp; test -s docs/phase-17-console-session-restore-ux.md</automated>
  </verify>
  <done>Docs give future executors exact selectors, commands, and boundaries without claiming deferred features are complete.</done>
</task>

</tasks>

<verification>
Run focused Java component tests and Playwright list gate. Confirm labels are synchronized and docs explicitly defer Phase 18/19/20/21 scopes.
</verification>

<success_criteria>
- Phase 17 has a deterministic no-key browser spec for restore/continue selectors.
- English/Chinese labels cover active-session and transcript status UX.
- Documentation records selector contracts and deferred boundaries.
</success_criteria>

<output>
After completion, create `.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-04-SUMMARY.md`
</output>
