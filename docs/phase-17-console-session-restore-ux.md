# Phase 17 Console Session Restore UX

Phase 17 makes recent Console sessions feel like a chat product path: users can find a recent session, select it, see restored transcript bubbles, and send a follow-up without silently creating a new session. It deliberately stops short of the later streaming, multi-turn context, and provider/local-profile stabilization phases.

## Selector Contract

Future component and Playwright gates should use stable product selectors rather than Vaadin internals or translated prose:

- Chat-only Console layout: `[data-layout="chat-home"]`
- Provider/model configuration: `[data-role="model-selector"]`, `[data-role="provider-status"]`
- Active chat panel: `[data-console-panel="chat"][data-console-panel-active="true"]`
- Removed panel switcher: `[data-action="show-console-panel"]` must be absent.
- Removed secondary panels: `[data-console-panel="agents"], [data-console-panel="sessions"], [data-console-panel="run-context"]` must be absent.
- Recent history cards: `[data-role="session-card"][data-session-id][data-session-active]`
- Session card fields: `[data-field="session-title"]`, `[data-field="session-preview"]`, `[data-field="session-updated-at"]`, `[data-field="session-status"]`
- More-history indicator only: `[data-role="session-more"]`
- Active-session banner: `[data-role="active-session-banner"][data-active-session-state="new|continued"]`
- New conversation reset action: `[data-action="new-conversation"]`
- Restored transcript messages: `[data-message-role="user|assistant|tool|error"][data-session-id][data-run-id][data-message-status][data-stream-state]`
- Primary chat bubbles: `[data-message-kind="primary-bubble"][data-bubble-align="left|right"]`
- Secondary tool/error transcript cards: `[data-message-kind="secondary-card"][data-transcript-card="tool|error"]`

The main chat path should assert user/assistant bubbles and session identity. Tool/error transcript entries may be visible, but they must remain secondary cards rather than raw runtime-event prose mixed into the assistant transcript. Browser tests must assert the chat surface, active-session banner, transcript hydration, and follow-up send behavior directly; panel switcher navigation is no longer an acceptable restore-path proof.

## Visible History and Details Flow

The current Console route is intentionally chat-first: users see provider/model configuration and the conversation surface without secondary tabs. Recent-session restore is proved by creating or loading a session through the runtime/API path, hydrating the transcript into chat bubbles, and preserving the active session identity for follow-up sends.

- **Session identity:** the active-session banner exposes `data-active-session-state="new|continued"` and remains the visible proof that follow-up sends reuse the selected session.
- **Transcript restore:** hydrated user/assistant/tool/error entries render into the chat stream with stable message-role, session, run, status, and stream-state selectors.
- **Operational detail:** tool/error transcript entries remain secondary cards in the chat stream rather than raw runtime-event prose mixed into assistant text.
- **Removed panels:** History/Sessions, Run Context, and Agents tabs are no longer rendered on the user-facing Console.

## Verification Commands

Focused component/i18n gate:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -Dtest=WebConsoleSessionRestoreUxTest,WebConsoleTranscriptHydrationTest test
```

No-key browser product-path list gate:

```bash
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-17-console-session-restore-ux.spec.ts --project="Mobile Chrome" --list
```

No-key artifact substantiveness gate:

```bash
PLAYWRIGHT_SKIP_WEBSERVER=1 npm run e2e -- e2e/phase-17-console-session-restore-ux.spec.ts --project="Mobile Chrome" --list \
  && test $(wc -l < e2e/phase-17-console-session-restore-ux.spec.ts) -ge 80 \
  && test $(wc -l < docs/phase-17-console-session-restore-ux.md) -ge 50
```

When a live local server is intentionally available, run the same Playwright file without `--list` to exercise the API-created fake-runtime session and browser restore path:

```bash
npm run e2e -- e2e/phase-17-console-session-restore-ux.spec.ts --project="Mobile Chrome"
```

## Implemented in Phase 17

1. Recent sessions are rendered as selectable cards with title, preview, status, updated-at, stable IDs, and active-state attributes.
2. Selecting a recent session switches back to Chat, updates the active-session banner to `continued`, highlights the selected card, and hydrates typed transcript messages.
3. Sending after selection reuses the selected `sessionId`; New Conversation is the explicit reset path for fresh-session creation.
4. Restored user/assistant transcript entries render as primary chat bubbles with message role, session, run, status, and stream-state selectors.
5. Tool/error transcript entries remain secondary/reachable cards so operational detail does not become raw main-chat prose.
6. English and Chinese bundles now contain synchronized restore labels for History, New conversation, Continue, empty transcript, abnormal statuses, and compact details wording.
7. The user-facing Console route now keeps only provider/model configuration and Chat visible; the Playwright product path verifies restore through active-session banner and transcript hydration rather than panel navigation.
8. Direct component construction now falls back to `messages.properties` for failed, cancelled, and partial transcript status chips, avoiding `!{...}!` missing-key markers in secondary cards.

## Gap-Closure Notes

- Plan 05 converted previously hidden advanced panel wiring into visible controls; the later chat-only simplification removed those controls from the user-facing route while preserving internal state seams for runtime behavior.
- Plan 06 and later browser specs realigned to the product path: they assert provider/model configuration, Chat, active-session banner, and restored transcript rather than opening Sessions, Run Details, or Agents tabs.
- The browser spec remains CI-safe in no-key mode through `--list`; the live command is intentionally separate for local/server-backed validation.
- The restore contract is deliberately selector-based and non-screenshot-based so future Phase 21 regression hardening can reuse it across browsers and viewport profiles.

## Deferred Boundaries

- **Phase 18 — streaming bubble lifecycle:** live model deltas should append to one assistant bubble and mutate pending/partial/terminal states. Phase 17 maps persisted transcript status to `data-stream-state` only.
- **Phase 19 — multi-turn runtime context:** follow-up submission preserves session identity, but full history-aware prompt/context assembly belongs to the runtime/context phase.
- **Phase 20 — provider/model and local profile stability:** provider readiness, model selection persistence, SQLite/local profile hardening, fallback/error feedback, and no-key local configuration closure remain downstream.
- **Phase 21 — broader regression hardening:** full cross-browser, release, security, and regression expansion should reuse these selectors but is not completed by this document.

## Explicitly Out of Scope

Search, rename, archive, pin, delete, bulk management, localStorage-only history, and full history management UI are not implemented in Phase 17. `data-role="session-more"` is only a lightweight continuation seam for future history management.
