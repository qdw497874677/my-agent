---
phase: 13-runtime-cards-timeline-tool-and-approval-ux
plan: 04
type: execute
wave: 4
depends_on:
  - 13-runtime-cards-timeline-tool-and-approval-ux-02
  - 13-runtime-cards-timeline-tool-and-approval-ux-03
files_modified:
  - pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css
  - e2e/phase-13-runtime-cards.spec.ts
  - e2e/fixtures/fake-runtime.ts
  - docs/phase-13-runtime-cards.md
  - docs/phase-12-console-mobile-flow.md
autonomous: true
requirements: [MCARD-01, MCARD-02, MCARD-03, MCARD-04, MCARD-05]
must_haves:
  truths:
    - "Mobile browser gate covers representative status, model, tool, approval, policy, terminal, error, and dense-detail runtime event cards."
    - "Details expanders, approval actions, and card controls meet 44px tap-target and visible focus expectations."
    - "Runtime/tool/approval cards do not cause page-level horizontal overflow at representative mobile viewport pressure."
    - "Phase 13 behavior and selector contract are documented for Phase 14/15 handoff."
  artifacts:
    - path: "e2e/phase-13-runtime-cards.spec.ts"
      provides: "Representative Console runtime-card mobile browser gate"
    - path: "pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css"
      provides: "Mobile card/detail/action overflow, tap, and focus styling"
    - path: "docs/phase-13-runtime-cards.md"
      provides: "Phase 13 selector, verification, and handoff documentation"
  key_links:
    - from: "e2e/phase-13-runtime-cards.spec.ts"
      to: "Console event feed"
      via: "stable selectors [data-role=event-feed] and [data-event-category]"
      pattern: "data-role=\\\"event-feed\\\""
    - from: "styles.css"
      to: "Runtime/tool/approval card classes"
      via: "CSS selectors for pi-runtime-event-card, pi-tool-call-card, pi-approval-card"
      pattern: "pi-(runtime-event|tool-call|approval)-card"
---

<objective>
Add final mobile CSS, browser representative matrix, and documentation for Phase 13 runtime/tool/approval card UX.

Purpose: Satisfy all MCARD requirements in the browser-visible Console path while honoring D-15/D-16/D-17/D-18: representative event matrix, stable selectors, no-key fake runtime, Java + Playwright dual gates.
Output: CSS card/detail rules, Playwright Phase 13 spec, optional fake-runtime hint, and docs.
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
@.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-CONTEXT.md
@.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-RESEARCH.md
@.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-runtime-cards-timeline-tool-and-approval-ux-02-SUMMARY.md
@.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-runtime-cards-timeline-tool-and-approval-ux-03-SUMMARY.md
@docs/phase-10-mobile-baseline.md
@docs/phase-11-responsive-shell.md
@docs/phase-12-console-mobile-flow.md
@e2e/phase-12-console-mobile-flow.spec.ts
@e2e/fixtures/mobile-smoke.ts
@e2e/fixtures/fake-runtime.ts
@pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css

<interfaces>
Existing Playwright helper contracts:

```ts
export async function expectNoPageHorizontalOverflow(page: Page, tolerance = 1): Promise<void>;
export async function expectTapTargetAtLeast(locator: Locator, minimum = 44, label = 'tap target'): Promise<void>;
export async function expectFocusVisible(page: Page, locator: Locator, label = 'focused control'): Promise<void>;
```

Selector contract to verify:
- `[data-role="event-feed"]`
- `[data-event-category="model|status|policy|terminal|event|tool|approval"]`
- `[data-expandable="true"]`
- `[data-detail-layer="structured|advanced"]`
- `[data-action="approve-tool-call|reject-tool-call"]`
- `[data-risk-action="approve|reject"]`
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Add Phase 13 mobile card CSS contract</name>
  <files>pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css</files>
  <read_first>
    - pi-agent-adapter-web/src/main/frontend/themes/pi-mobile/styles.css
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/RuntimeEventCard.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ToolCallCard.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ApprovalCard.java
  </read_first>
  <action>Add CSS rules for `.pi-runtime-event-card`, `.pi-tool-call-card`, `.pi-approval-card`, `.pi-runtime-card-summary`, `.pi-runtime-card-meta`, `.pi-status-chip`, `.pi-risk-chip`, `.pi-detail-block`, `.pi-redacted-json`, and `[data-detail-layer]`. Rules must set `width: 100%`, `max-width: 100%`, `min-width: 0`, `overflow-wrap: anywhere`, `word-break: break-word` where appropriate, and use existing tokens `--pi-mobile-tap-target`, `--pi-mobile-space-sm`, `--pi-mobile-space-md`, `--pi-mobile-shell-border`, `--pi-mobile-shell-surface`, and `--pi-mobile-focus-ring`. Ensure `.pi-approval-card [data-approval-actions="inline"]` lays out as a wrapping action row and its buttons/actions have at least `min-height: var(--pi-mobile-tap-target)`. Do not add animations or modal overlay styles.</action>
  <acceptance_criteria>
    - `styles.css` contains selectors `.pi-runtime-event-card`, `.pi-tool-call-card`, and `.pi-approval-card`.
    - `styles.css` contains selectors `.pi-status-chip`, `.pi-risk-chip`, `.pi-detail-block`, and `.pi-redacted-json`.
    - `styles.css` contains `[data-approval-actions="inline"]` and `min-height: var(--pi-mobile-tap-target)`.
    - `styles.css` contains `overflow-wrap: anywhere` in the Phase 13 card/detail rules.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleRuntimeCardsTest,WebConsoleCatalogAndToolCardsTest,WebConsoleApprovalCardsTest test</automated>
  </verify>
  <done>Runtime/tool/approval card classes have mobile overflow, tap-target, and focus-compatible theme support.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Add representative Phase 13 Playwright runtime-card gate</name>
  <files>e2e/phase-13-runtime-cards.spec.ts, e2e/fixtures/fake-runtime.ts</files>
  <read_first>
    - e2e/phase-12-console-mobile-flow.spec.ts
    - e2e/fixtures/mobile-smoke.ts
    - e2e/fixtures/fake-runtime.ts
    - docs/phase-12-console-mobile-flow.md
  </read_first>
  <behavior>
    - Test 1: Mobile Chrome list gate loads `phase-13-runtime-cards.spec.ts` and declares a representative runtime card test.
    - Test 2: Browser test opens `/console`, sends a deterministic prompt/hint, waits for event feed cards, and verifies categories for tool or approval plus at least one non-tool runtime event.
    - Test 3: Browser test expands Details controls, asserts no raw sensitive marker text is visible, samples approve/reject or available approval controls for 44px and focus-visible, and checks no page-level horizontal overflow.
  </behavior>
  <action>Create `e2e/phase-13-runtime-cards.spec.ts` based on Phase 12's Console product path. Import `expectNoPageHorizontalOverflow`, `expectTapTargetAtLeast`, and `expectFocusVisible`. Add or reuse a fake-runtime hint function in `fake-runtime.ts`, e.g. `phase13RuntimeCardMatrixHint()`, returning a no-key prompt string that asks the deterministic fake runtime for model/status/tool/approval/policy/terminal/error/dense-detail events. The spec must use stable selectors only: `[data-role="event-feed"]`, `[data-event-category]`, `[data-event-category="tool"]`, `[data-event-category="approval"]`, `[data-expandable="true"]`, `[data-detail-layer="advanced"]`, `[data-action="approve-tool-call"]`, `[data-action="reject-tool-call"]`. Use tolerant assertions if fake runtime emits either an inline approval card or the pending approval panel, but do not count Run Context alone as satisfying Phase 13 card interiors. Include `--list` compatibility: no test should require live browser execution just to list.</action>
  <acceptance_criteria>
    - `e2e/phase-13-runtime-cards.spec.ts` exists and imports `expectNoPageHorizontalOverflow`, `expectTapTargetAtLeast`, and `expectFocusVisible`.
    - `e2e/phase-13-runtime-cards.spec.ts` contains selectors `[data-event-category="tool"]`, `[data-event-category="approval"]`, `[data-detail-layer="advanced"]`, `[data-action="approve-tool-call"]`, and `[data-action="reject-tool-call"]`.
    - `e2e/fixtures/fake-runtime.ts` exports `phase13RuntimeCardMatrixHint` or the spec explicitly reuses `mobileToolApprovalHint()` with a Phase 13-specific prompt constant.
    - The spec contains a negative assertion for raw secret markers `sk-live-secret` and `raw-token-value`.
  </acceptance_criteria>
  <verify>
    <automated>PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-13-runtime-cards.spec.ts --project="Mobile Chrome" --list</automated>
  </verify>
  <done>Phase 13 has a deterministic representative browser gate for runtime/tool/approval card interiors and mobile safety assertions.</done>
</task>

<task type="auto">
  <name>Task 3: Document Phase 13 selector and verification handoff</name>
  <files>docs/phase-13-runtime-cards.md, docs/phase-12-console-mobile-flow.md</files>
  <read_first>
    - docs/phase-12-console-mobile-flow.md
    - docs/phase-11-responsive-shell.md
    - docs/phase-10-mobile-baseline.md
    - .planning/REQUIREMENTS.md
    - .planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-CONTEXT.md
  </read_first>
  <action>Create `docs/phase-13-runtime-cards.md` with sections: `Scope`, `Requirement Traceability`, `Selector Contract`, `Redaction and Dense Details`, `Approval UX Contract`, `Verification Commands`, `Known CI and Emulation Gaps`, and `Deferred Handoffs`. Include exact commands: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleRuntimeCardsTest,WebConsoleCatalogAndToolCardsTest,WebConsoleApprovalCardsTest test` and `PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-13-runtime-cards.spec.ts --project="Mobile Chrome" --list`. Update `docs/phase-12-console-mobile-flow.md` Deferred Handoffs bullet for Phase 13 to link to the new document and state that runtime event/tool/approval interiors are implemented by Phase 13 while Phase 15 still owns real-device/orientation/accessibility hardening.</action>
  <acceptance_criteria>
    - `docs/phase-13-runtime-cards.md` contains headings `## Scope`, `## Requirement Traceability`, `## Selector Contract`, `## Redaction and Dense Details`, `## Approval UX Contract`, `## Verification Commands`, and `## Deferred Handoffs`.
    - `docs/phase-13-runtime-cards.md` lists MCARD-01, MCARD-02, MCARD-03, MCARD-04, and MCARD-05.
    - `docs/phase-13-runtime-cards.md` contains both Java and Playwright commands from the action.
    - `docs/phase-12-console-mobile-flow.md` contains `docs/phase-13-runtime-cards.md`.
  </acceptance_criteria>
  <verify>
    <automated>JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleRuntimeCardsTest,WebConsoleCatalogAndToolCardsTest,WebConsoleApprovalCardsTest test</automated>
  </verify>
  <done>Documentation captures Phase 13 implementation, selectors, verification commands, and remaining Phase 15 handoffs.</done>
</task>

</tasks>

<verification>
Run Java targeted contracts and Playwright list gate. If full browser execution is attempted and blocked by local Vaadin/WebKit host dependencies, document the environment limitation in the summary without weakening selector/list gates.
</verification>

<success_criteria>
All MCARD requirements have browser/documentation coverage, theme rules prevent mobile overflow, and Phase 13 has a clear verification/documentation handoff to Phase 14 and Phase 15.
</success_criteria>

<output>
After completion, create `.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-runtime-cards-timeline-tool-and-approval-ux-04-SUMMARY.md`.
</output>
