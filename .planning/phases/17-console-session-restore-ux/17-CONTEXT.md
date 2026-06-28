# Phase 17: Console Session Restore UX - Context

**Gathered:** 2026-06-28
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 17 turns the existing Vaadin Console into a chat-first session restore experience. Users must be able to see recent conversations, select a historical session, clear and hydrate the conversation feed from the typed persisted transcript, clearly see whether they are starting a new conversation or continuing an existing one, and send the next message into the selected session.

This phase stays in the existing Java/Vaadin/COLA direction and consumes the Phase 16 typed conversation read model. It does **not** implement the full streaming assistant bubble lifecycle, pending/delta/terminal aggregation, multi-turn model context assembly, provider/model/local profile persistence stability, conversation search, rename/archive/pin/delete, branching/edit/regenerate, import/export, React/Next.js rewrite, mobile-only APIs, or browser localStorage history.

</domain>

<decisions>
## Implementation Decisions

### Recent History Entry Point
- **D-01:** The primary recent-history information architecture is a compact left-side history rail/sidebar on desktop and tablet, with the same history available from a mobile History panel/button on phone layouts. This keeps the Console chat-first while making recent sessions visible enough to satisfy CIA-01.
- **D-02:** On mobile, selecting a session from history must immediately return the user to Chat, clear the current feed, hydrate the selected transcript, and make the active session identity visible. Do not leave the user stranded in the history panel after selection.
- **D-03:** Recent session cards should default to the Phase 16 `SessionSummaryDto` shape: stable title, last-message preview, last activity time, and status/active-run status. Do not include model/provider metadata as a default card field in Phase 17; provider/model history semantics belong mainly to Phase 20.
- **D-04:** Phase 17 should implement a bounded recent list, e.g. the latest N sessions with a lightweight “more”/cursor hook if straightforward. It should not implement full search, rename, archive, pin, delete, or rich management UX.

### Active Session Identity
- **D-05:** The chat area should show a compact active-session banner/chip near the top of the Chat feed. It should not steal focus from the composer or turn the Console back into a run workbench.
- **D-06:** The banner has two explicit states: `New conversation` when no historical session is selected, and `Continue: {title}` when a historical session is selected. This distinction is required so users do not accidentally continue the wrong session or mistakenly create a new one.
- **D-07:** When a historical session is selected, the banner should include an explicit New Conversation action to exit continuation mode and start a fresh session. This avoids accidental SESS-03 violations where the next message silently appends to an old session.
- **D-08:** The active-session title should use the Phase 16 stable session title derived from the first user message. Phase 17 must not introduce rename, auto-retitle-on-latest-message, or title management behavior.

### Transcript Hydration and Bubble Shape
- **D-09:** Restored transcripts must come from the Phase 16 typed conversation read model via the Console bridge, not from `ChatEventStreamPanel.messages()`, raw run-event maps, REST history maps, or browser local state.
- **D-10:** Hydration should render `user` and `assistant` transcript items as the primary chat bubbles. `tool` and `error` transcript items should be preserved as compact inline cards/status items, not discarded and not rendered as equal conversational prose.
- **D-11:** Phase 17 should upgrade `ChatEventStreamPanel.replaceTranscriptForProof(...)` into a formal typed transcript hydration API such as `replaceTranscript(...)`, while preserving any compatibility/test helper as needed. The method should clear the current feed before rendering the selected session transcript.
- **D-12:** Restored bubbles/cards must include stable selectors and metadata hooks needed by downstream testing and Phase 18: at minimum `data-message-role`, `data-session-id`, `data-run-id` when available, and status/stream-state-style attributes such as `data-message-status` or `data-stream-state`. Tests should assert role/order/grouping, not only visible text.
- **D-13:** Visual metadata should stay lightweight: show time/status only where helpful, with failed/cancelled/partial/error states clearly visible and completed state visually quiet. Detailed run/tool/provider/diagnostic information remains behind existing compact cards/expanders/details.
- **D-14:** Selecting a historical session follows this user-visible sequence: clear the current feed, hydrate typed transcript bubbles/cards, set/update the active-session banner, highlight the selected session card, and scroll to the latest restored message. After the user scrolls up to read history, subsequent automatic updates should avoid fighting the user’s reading position.

### Phase 17 / Phase 18 Boundary
- **D-15:** Phase 17 must ensure that sending after selecting a session appends to the selected session rather than creating a new session. It does **not** implement the true pending/live assistant bubble lifecycle.
- **D-16:** After send, Phase 17 may continue using the existing run/event/polling rendering path for live runtime feedback. The real pending assistant bubble, delta coalescing, terminal/error/cancel state mutation, replay dedupe, and post-cancel delta suppression are Phase 18 responsibilities.
- **D-17:** Planning should avoid introducing a half-implemented streaming aggregator in Phase 17. If a small seam is needed, it should be an additive bubble API/selector foundation for Phase 18, not full streaming semantics.

### Advanced Details and Operational Truth
- **D-18:** Runtime/tool/provider/diagnostic details should be collapsed or visually secondary by default, but remain reachable from the conversation flow when relevant for tool calls, approvals, errors, cancellation, or diagnostics.
- **D-19:** Phase 17 should preserve Phase 13’s inline card/detail/redaction discipline. Kimi-style simplification means reducing noise in the main chat, not hiding operational truth or safety-critical status.

### Existing Regression Boundary
- **D-20:** Pre-existing Console UI/translation/layout test failures noted after Phase 16 should only be brought into Phase 17 scope when they directly block or validate session restore UX, active-session identity, compact history, or typed transcript hydration. Broad unrelated cleanup should remain outside this phase.

### Folded Todos
- No pending todos matched Phase 17 scope.

### the agent's Discretion
- Exact component names, CSS class names, icons, chip colors, spacing, and breakpoint polish are planner/designer discretion as long as the decisions above hold and the UI remains Vaadin/Java-first.
- Exact bounded recent-session limit and “more” cursor presentation are implementation discretion, provided the default list is useful and management/search features do not creep into Phase 17.
- Exact timestamp formatting and status badge wording are implementation discretion, provided abnormal/partial/error/cancel states are visible and completed states stay quiet.
- Exact scroll anchoring mechanics are implementation discretion, provided initial restore lands near the latest message and subsequent updates avoid forcing scroll jumps while the user reads older history.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 17 Scope and Requirements
- `.planning/ROADMAP.md` §Phase 17 — Phase goal, dependency on Phase 16, CIA-01 through CIA-04 and SESS-02/SESS-03 mapping, success criteria, and Kimi-style IA UI hint.
- `.planning/REQUIREMENTS.md` §Conversation IA and Chat-First Console — CIA-01 through CIA-04 for chat-first home, active-session banner, collapsed diagnostics, and reachable advanced details.
- `.planning/REQUIREMENTS.md` §Recent Sessions and Transcript Restore — SESS-02 and SESS-03 for selecting historical sessions and continuing the selected session; also note SESS-01/SESS-04 from Phase 16 and SESS-05 deferred to Phase 20.
- `.planning/REQUIREMENTS.md` §Future Requirements and §Out of Scope — search/rename/archive/pin/delete, branching/edit/regenerate, localStorage history, React rewrite, mobile-only API fork, vector/RAG, and automatic provider fallback are out of Phase 17 scope.
- `.planning/PROJECT.md` §Current Milestone: v1.2 Console 对话产品化 — Kimi-homepage-like Console direction, history restore, real streaming later, multi-turn context later, and local stability later.
- `.planning/STATE.md` — Current milestone state and accumulated Console/mobile decisions.

### Prior Phase Decisions That Must Be Carried Forward
- `.planning/phases/16-conversation-read-model-and-recent-sessions/16-CONTEXT.md` — D-01 through D-04 define the Conversation read-model boundary and session-centric REST paths; D-05 through D-08 define typed transcript source/roles/status/metadata; D-09 through D-12 define recent session title/preview/status semantics; D-13/D-14 prohibit raw event maps as the main chat transcript contract while preserving diagnostics.
- `.planning/phases/16-conversation-read-model-and-recent-sessions/16-conversation-read-model-and-recent-sessions-04-PLAN.md` — Minimal Console proof hooks and explicit handoff that Phase 17 owns visible restore UX.
- `.planning/phases/16-conversation-read-model-and-recent-sessions/16-VERIFICATION.md` — Known pre-existing Console test failures and Phase 16 verification boundary.
- `.planning/phases/12-console-mobile-first-flow/12-CONTEXT.md` — Chat Feed first, in-page secondary panel access, automatic return to Chat after session selection, active-session clarity, stable `data-*` selectors, and adapter-web bridge pattern.
- `.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-CONTEXT.md` — Runtime/tool/approval details stay inline in the Chat/Event Feed, compact by default, expandable, redacted, and tested with stable selectors.
- `.planning/phases/15-cross-browser-orientation-accessibility-and-release-hardening/15-CONTEXT.md` — Stable selector/no-screenshot regression philosophy, orientation/accessibility hardening, and release gate patterns relevant to Console UI changes.

### v1.2 Research Inputs
- `.planning/research/ARCHITECTURE.md` §ConsoleView seam, §ChatEventStreamPanel seam, §AppConsoleRunExecutionBridge seam, §Phase B — Console Session Restore UX, and Anti-Patterns 1/3/4 — Suggested UI hydration sequence, bubble API selectors, and warnings against Vaadin-as-history, raw event logs as chat transcript, and session-id-as-context.
- `.planning/research/PITFALLS.md` #1 — Session selection must restore typed transcript bubbles, not only highlight a sidebar row.
- `.planning/research/PITFALLS.md` #9 — Kimi-style minimalism must not hide provider/tool/error/cancel operational truth.
- `.planning/research/PITFALLS.md` #13 — Finish/error/cancel are state transitions; avoid blank/malformed assistant bubbles.
- `.planning/research/PITFALLS.md` #15 — Composer should avoid duplicate overlapping submits during an active run unless explicitly supported.
- `.planning/research/PITFALLS.md` #16 — Scroll anchoring must avoid fighting users reading restored history.
- `.planning/research/PITFALLS.md` #17 — Tests must assert message role/order/grouping selectors, not just text.

### Existing Code Contracts to Inspect
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleView.java` — Vaadin coordinator; inspect `selectSession(...)`, `planChatSubmission(...)`, `loadRecentSessionsForProof(...)`, attach listener behavior, `selectedSessionId`, panel visibility, column order, and demo bridge.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ChatEventStreamPanel.java` — Current feed/composer and proof transcript replacement seam; upgrade toward formal typed bubble hydration and selectors.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java` — Current recent session rendering, `session-card` selector, selection highlight, and activation handler.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleRunExecutionBridge.java` and `AppConsoleRunExecutionBridge.java` — Console-to-App bridge methods for recent sessions and transcript loading.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/ConversationQueryService.java` — App-layer conversation read model boundary.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/conversation/SessionSummaryDto.java`, `ConversationMessageDto.java`, `ConversationMessageRole.java`, `ConversationMessageStatus.java`, and `ConversationTranscriptResponse.java` — Typed DTO schema to render.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/SessionController.java` — Session-centric typed transcript/recent-session REST endpoints already created in Phase 16.
- `pi-agent-adapter-web/src/main/resources/messages.properties` and `pi-agent-adapter-web/src/main/resources/messages_zh.properties` — Console/session/chat i18n keys must be kept in sync.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleConversationReadModelHookTest.java` — Phase 16 proof test baseline to upgrade from proof hook to visible restore contracts.
- `e2e/phase-12-console-mobile-flow.spec.ts`, `e2e/phase-15-critical-flow-regression.spec.ts`, `e2e/fixtures/fake-runtime.ts`, and `e2e/fixtures/mobile-smoke.ts` — Browser selector and fake-runtime patterns to extend.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `SessionSummaryDto`: carries `sessionId`, `title`, `status`, `lastMessagePreview`, `lastActivityAt`, `createdAt`, `activeRunId`, `activeRunStatus`, and metadata. Use it for recent history cards and active-session title/status.
- `ConversationMessageDto`: carries role, text, status, session/run/step/message refs, timestamps, ordering identity, metadata, visibility, and redaction fields. Use it for bubble/card hydration.
- `ConversationQueryService`: App-layer read-model service created in Phase 16; Vaadin should access it only through the bridge.
- `ConsoleRunExecutionBridge` / `AppConsoleRunExecutionBridge`: established adapter-web seam for `listRecentSessions(...)` and `getTranscript(...)` without leaking repositories into Vaadin components.
- `SessionListPanel.showRecentSessionsForProof(...)`: current recent-session entry point with `data-role="session-card"`, active selection hook, and keyboard activation semantics; promote or replace with a formal compact-history API.
- `ChatEventStreamPanel.replaceTranscriptForProof(...)`: current typed transcript proof hook; promote to formal `replaceTranscript(...)` style API and add message-level selectors.
- `RunEventRenderer`, `ToolCallCard`, `ApprovalCard`, `ApprovalPanel`, and `RuntimeDetailRedactor`: existing runtime/tool/approval/detail rendering and redaction primitives that should remain reachable but secondary.
- `PiResponsiveShell`, `PiPageHeader`, `PiPageSection`, `PiRouteNavRegistry`, and `pi-mobile/styles.css`: shared Vaadin shell/theme primitives for responsive layout, tap/focus, safe-area, and stable route/page hooks.

### Established Patterns
- Adapter Web owns Vaadin presentation state; App owns query/use-case semantics; Domain/client DTOs must remain free of Vaadin, Spring Web, persistence, browser, and theme types.
- UI should call App through bridge classes rather than injecting repositories or infrastructure services directly.
- Prior phases prefer additive changes and stable data attributes over endpoint churn, mobile-only APIs, or frontend stack replacement.
- Stable `data-*` selectors are the contract for Vaadin component/browser tests; Phase 17 should add message-level role/session/run/status selectors for transcript restore.
- Browser E2E should stay deterministic/no-key using fake runtime fixtures, not real providers or external services.
- Runtime/tool/approval details are diagnostic/safety surfaces; they may be collapsed by default but must remain accessible and redacted.
- Demo/no-Spring constructors and Spring bridge constructors should both stay usable for deterministic component tests.

### Integration Points
- `ConsoleView` should load recent sessions on attach, maintain selected/new conversation state, expose/update active-session banner, call bridge transcript loading on selection, clear/hydrate the chat panel, highlight session cards, and ensure send uses `selectedSessionId` when present.
- `SessionListPanel` should become the formal compact history surface for recent sessions, selected state, mobile panel behavior, and bounded list/more hook.
- `ChatEventStreamPanel` should become a typed bubble/card hydration surface for restored user/assistant/tool/error transcript items while leaving live streaming aggregation for Phase 18.
- `messages.properties` and `messages_zh.properties` need new labels for History, New conversation, Continue, New Conversation action, session status, empty/history states, and abnormal transcript statuses.
- Java component tests should validate bridge calls, feed clearing, typed role rendering, active banner states, stable selectors, and selected-session send behavior.
- Playwright tests should validate the product path: load Console, see recent history, select a session, observe prior user/assistant turns as bubbles with role selectors, see active-session banner, send again, and verify the run/session uses the selected session.

</code_context>

<specifics>
## Specific Ideas

- The user selected the recommended path for all discussed Phase 17 areas.
- History should feel like a Kimi/ChatGPT-style compact session rail rather than a separate management page.
- Active-session identity should be explicit and lightweight: a top-of-chat banner/chip with `New conversation` vs `Continue: {title}` and a New Conversation escape action.
- Restored chat should visually prioritize user/assistant conversation while retaining tool/error items as compact inline cards/status items.
- Phase 17 should lay selector/API groundwork for Phase 18 but must not smuggle in full streaming lifecycle work.

</specifics>

<deferred>
## Deferred Ideas

- Full pending/live assistant bubble lifecycle, delta aggregation, replay dedupe, terminal/error/cancel bubble mutation, and post-cancel suppression — Phase 18.
- Multi-turn model context assembly from selected-session transcript — Phase 19.
- Provider/model readiness, per-run model display/pinning, local profile SQLite restart persistence, model refresh states, and fallback labeling — Phase 20.
- Broad verification/security/regression matrix beyond the direct Phase 17 restore path — Phase 21.
- Search, rename, archive, pin, delete, branching, editing, regeneration, import/export, prompt libraries, long-term memory, RAG, vector DB, and automatic paid-provider fallback — future/out of scope.
- Unrelated pre-existing Console UI/translation/layout test cleanup not required for session restore behavior — defer unless it directly blocks Phase 17 gates.

</deferred>

---

*Phase: 17-console-session-restore-ux*
*Context gathered: 2026-06-28*
