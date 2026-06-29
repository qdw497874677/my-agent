---
phase: 17-console-session-restore-ux
plan: 06
type: execute
wave: 5
depends_on:
  - 17-console-session-restore-ux-05
files_modified:
  - e2e/phase-17-console-session-restore-ux.spec.ts
  - docs/phase-17-console-session-restore-ux.md
autonomous: true
gap_closure: true
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
    - "Browser restore path uses visible controls or visible recent-history surface, not hidden panel switchers."
    - "Phase 17 handoff docs describe the fixed visible history/details selectors and live-run command."
  artifacts:
    - path: "e2e/phase-17-console-session-restore-ux.spec.ts"
      provides: "Executable Playwright product-path gate aligned with visible Console controls"
      min_lines: 80
    - path: "docs/phase-17-console-session-restore-ux.md"
      provides: "Updated selector/verification/deferred-boundary documentation for gap closure"
      min_lines: 50
  key_links:
    - from: "e2e/phase-17-console-session-restore-ux.spec.ts"
      to: "ConsoleView visible history/details selectors"
      via: "click/assert only visible controls or visible session cards"
      pattern: "data-role=\"session-card\""
    - from: "docs/phase-17-console-session-restore-ux.md"
      to: "17-VERIFICATION.md gaps"
      via: "documents live browser and advanced-detail verification path"
      pattern: "History|details|visible"
---

<objective>
Update the Phase 17 browser product-path gate and handoff docs so verification proves the fixed visible restore flow instead of a hidden-control path.

Purpose: Plan 04 encoded the intended live browser flow but only passed `--list`, used hidden panel switcher controls, and missed its declared `min_lines: 80` artifact threshold. After Plan 05 makes history/details visible, this plan realigns the Playwright spec and documentation to the real user path.

Output: A substantive Playwright spec using visible restore controls/surfaces, plus updated docs for selectors, commands, and gap-closure verification.
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
@.planning/phases/17-console-session-restore-ux/17-VERIFICATION.md
@.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-04-SUMMARY.md
@.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-05-SUMMARY.md
@e2e/phase-17-console-session-restore-ux.spec.ts
@e2e/fixtures/fake-runtime.ts
@docs/phase-17-console-session-restore-ux.md

<interfaces>
Use selectors produced by Plan 05 and earlier Phase 17 work:
```text
[data-role="session-card"][data-session-id][data-session-active]
[data-field="session-title"|"session-preview"|"session-status"]
[data-console-panel="chat|sessions|run-context"][data-console-panel-active]
[data-action="show-console-panel"][data-console-target="sessions|run-context|chat"] only if the button/control is visible
[data-role="active-session-banner"][data-active-session-state="new|continued"]
[data-action="new-conversation"]
[data-message-role="user|assistant|tool|error"][data-session-id][data-run-id][data-message-status][data-stream-state]
[data-message-kind="primary-bubble"|"secondary-card"]
```

Current verifier gaps to address:
- `e2e/phase-17-console-session-restore-ux.spec.ts` is 77 lines while Plan 04 declares `min_lines: 80`.
- `openConsolePanel(...)` currently clicks hidden `data-action="show-console-panel"` controls. Replace or harden helpers so the test path starts from visible history/details controls or a visible recent-history surface.
- Keep no-key/list command, but document the live command to run when a local server is available.
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Realign Playwright restore flow with visible history controls</name>
  <files>e2e/phase-17-console-session-restore-ux.spec.ts</files>
  <behavior>
    - Test 1: The spec still creates/restores a fake-runtime conversation, opens `/console`, and finds the restored session through a visible recent-history surface or visible History control.
    - Test 2: The helper never blindly clicks hidden `show-console-panel` controls; if it uses a panel control, it first asserts the control is visible/enabled.
    - Test 3: The flow selects the session, verifies Chat is active/visible, verifies `Continue:` state, restored user/assistant bubbles, same-session follow-up, active card identity, and secondary-card/no-runtime-noise behavior.
    - Test 4: The spec is substantive enough to satisfy the prior artifact contract (`wc -l` at least 80) without padding meaningless comments.
  </behavior>
  <action>Update `e2e/phase-17-console-session-restore-ux.spec.ts` so it reflects Plan 05's visible Console path. Prefer directly locating visible `[data-role="session-card"]` cards if the history rail is visible; otherwise use only a visible History/session affordance and then assert the sessions panel is visible. Remove assumptions that hidden controls can be clicked. Keep stable selector assertions for session card fields, active banner, transcript bubble identity, same-session follow-up, active card, and primary-vs-secondary runtime noise. Add a small assertion for reachable details/run-context if Plan 05 exposes a visible details control, but do not make this a flaky visual/screenshot test. Preserve the no-key `--list` gate; do not require real provider keys.</action>
  <verify>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-17-console-session-restore-ux.spec.ts --project="Mobile Chrome" --list && test $(wc -l &lt; e2e/phase-17-console-session-restore-ux.spec.ts) -ge 80</automated>
  </verify>
  <done>Browser spec is listable, at least 80 lines, and encodes a user-visible restore path instead of hidden-control navigation.</done>
</task>

<task type="auto">
  <name>Task 2: Update Phase 17 handoff docs for gap-closure selectors and live verification</name>
  <files>docs/phase-17-console-session-restore-ux.md</files>
  <action>Update the Phase 17 doc to include the visible history/recent-session and advanced/details affordances added by Plan 05, the corrected Playwright command from Task 1, and a short gap-closure note explaining that users no longer need hidden controls or test-only `showConsolePanel(...)` calls to access recent history/details. Keep existing deferred boundaries: search/rename/archive/pin/delete, localStorage history, streaming lifecycle, multi-turn context, provider/model stability, and broad Phase 21 regression remain out of scope. Add a live-browser command example for when the local server is intentionally available, but keep the no-key list gate as the automated plan verification.</action>
  <verify>
    <automated>test -f docs/phase-17-console-session-restore-ux.md &amp;&amp; test $(wc -l &lt; docs/phase-17-console-session-restore-ux.md) -ge 50</automated>
  </verify>
  <done>Docs accurately describe visible restore/detail selectors, automated/list and live-browser verification commands, and explicit deferred boundaries after gap closure.</done>
</task>

</tasks>

<verification>
Run the no-key browser list gate and artifact line checks:
`PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-17-console-session-restore-ux.spec.ts --project="Mobile Chrome" --list && test $(wc -l < e2e/phase-17-console-session-restore-ux.spec.ts) -ge 80 && test $(wc -l < docs/phase-17-console-session-restore-ux.md) -ge 50`
</verification>

<success_criteria>
- Playwright restore path is aligned with user-visible controls and stable selectors.
- Spec satisfies the artifact substantiveness contract without meaningless padding.
- Documentation reflects gap closure and does not claim deferred Phase 18/19/20/FUT behavior.
</success_criteria>

<output>
After completion, create `.planning/phases/17-console-session-restore-ux/17-console-session-restore-ux-06-SUMMARY.md`
</output>
