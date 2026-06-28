# Phase 16: Conversation Read Model and Recent Sessions - Context

**Gathered:** 2026-06-28
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 16 establishes the canonical, ownership-safe conversation read model for recent sessions and typed transcripts. It provides App/client DTOs, query boundaries, repository filters, transcript assembly semantics, and minimal Console hooks needed to prove the read model. This phase does **not** deliver Kimi-style visual polish, full session restore UX, streaming bubble lifecycle, multi-turn model context, or full local profile/restart stability; those remain Phase 17-20 work.

</domain>

<decisions>
## Implementation Decisions

### Conversation Read Model Boundary
- **D-01:** Create a new Conversation read-model boundary rather than extending the existing Session boundary directly. Downstream should prefer `pi-agent-client` conversation DTOs and an App-layer `ConversationQueryService` (or equivalently named use case) for recent sessions and transcripts.
- **D-02:** Keep REST paths session-centric: expose the conversation read model under existing session-oriented API shape, e.g. recent sessions and transcript as `/api/sessions/...` resources, rather than creating a new mobile-specific API or unrelated top-level product fork.
- **D-03:** Phase 16 Console integration should be a minimal proof hook only: enough for the Console bridge/tests to load recent summaries and typed transcript data, without formal Kimi-style layout/polish or full restore UX. Phase 17 owns the visible chat-first restore experience.
- **D-04:** Public JSON contracts must live in `pi-agent-client`/App boundaries, not Domain records. Domain remains JDK-only and framework/persistence/UI neutral.

### Transcript Source and Shape
- **D-05:** Use typed transcript projection tables/read models as the preferred source of truth for conversation transcript, with persisted run events retained as audit/replay/backfill inputs. Planning should avoid making raw run-event maps the UI transcript contract.
- **D-06:** Transcript role scope includes `user`, `assistant`, `tool`, and `error`. Tool/error items may be summarized/redacted, but they must remain first-class typed transcript items so Agent-specific context is not lost.
- **D-07:** Conversation messages should carry explicit status fields such as completed/failed/cancelled/partial/pending where relevant. Finish/error/cancel events are state transitions, not blank assistant messages.
- **D-08:** Restored transcript must preserve message role, text/summary, session/run refs, status, timestamps, ordering identity, metadata, and redaction/visibility information sufficient for Phase 17 restore and Phase 19 context assembly.

### Recent Session Summary Semantics
- **D-09:** Recent sessions sort by latest conversation activity. Session `lastActivityAt` should update when visible transcript activity changes, including user, assistant, tool, error, and terminal status activity as appropriate.
- **D-10:** Default session title is derived from the first user message and remains stable unless a future explicit rename feature is introduced. Do not auto-retitle on every latest prompt.
- **D-11:** Last-message preview uses the latest visible transcript item after visibility/redaction filtering. If no safe preview exists, fall back to a stable safe title/first-user-message-derived summary.
- **D-12:** Recent session status should be derived from the most recent relevant run/conversation state, e.g. idle/running/completed/failed/cancelled, rather than a generic always-open session status.

### Safety, Compatibility, and Scope
- **D-13:** Replace the old raw-map **session history/transcript contract** with typed conversation read-model contracts. Do not make restored conversation UI consume `List<Map<String,Object>>` history entries.
- **D-14:** Preserve existing run/event diagnostic endpoints where needed for runtime details and compatibility; they may remain raw/diagnostic, but the main chat transcript contract must not depend on them.
- **D-15:** Repository/query interfaces for conversation and run projection data must carry explicit ownership context (`RequestContext` plus session/run IDs as applicable), and infrastructure queries must enforce tenant/user/session/run filters at SQL/repository level.
- **D-16:** Golden tests should prove the typed DTO schema and transcript assembler behavior: transcript response uses typed `ConversationMessageDto`-style records rather than raw payload maps, and assembler cases cover user input, model deltas, finish, tool item, error/failure, cancellation/partial states.
- **D-17:** SQLite work is allowed only as minimal conversation-read-model adapter/contract alignment required by the user's preference to avoid a later fork. It must not expand into Phase 20 local profile productization, restart recovery, provider config stability, or full SQLite UX.

### the agent's Discretion
- Exact class names may vary if planner finds a stronger local convention, but semantics must remain a new Conversation read-model boundary.
- Exact pagination/cursor design is left to downstream research/planning, provided it is deterministic and not confused with run-event sequence cursors unless explicitly documented.
- Exact redaction helper reuse is left to implementation, but transcript previews and messages must follow existing conservative redaction discipline.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 16 Scope and Requirements
- `.planning/ROADMAP.md` §Phase 16 — Phase goal, dependencies, SESS-01/SESS-04 mapping, success criteria, and “no major visual polish yet” UI hint.
- `.planning/REQUIREMENTS.md` §Recent Sessions and Transcript Restore — SESS-01 through SESS-05; Phase 16 owns SESS-01 and SESS-04.
- `.planning/REQUIREMENTS.md` §Out of Scope — no React rewrite, no localStorage history source of truth, no branching/edit/regenerate, no vector memory/RAG.
- `.planning/STATE.md` — Current milestone and Phase 16 focus.

### v1.2 Research Inputs
- `.planning/research/PITFALLS.md` — Especially pitfalls #1, #11, #15, #17 for session restore, ownership leakage, typed transcript, and session title/ordering.
- `.planning/research/ARCHITECTURE.md` §Phase A — Conversation Read Model and Recent Sessions — Suggested build order and anti-patterns for raw event log, Vaadin in-memory history, and mobile-only APIs.
- `.planning/research/SUMMARY.md` §Phase 1: Conversation Read Model and Recent Sessions — Milestone sequencing rationale and deliverables.
- `.planning/research/FEATURES.md` CONV-002/CONV-003 and current codebase dependency table — Recent sessions and transcript restore expectations.

### Prior Phase Contracts
- `.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-CONTEXT.md` — RunEvent envelope, Session tree/context, Domain/client boundary, workspace/session/message concepts.
- `.planning/phases/02-cloud-server-persistence-sse-and-baseline-security/02-CONTEXT.md` — Session-centric REST shape, persist-then-emit, client DTO ownership, query/persistence boundaries.
- `.planning/phases/04-governed-tool-registry-workspace-and-invocation-pipeline/04-CONTEXT.md` — Tool lifecycle events, payload limits, preview/approval/audit/redaction constraints.
- `.planning/phases/05-agent-web-console-and-runtime-cockpit/05-CONTEXT.md` — Console/session/run UI seams and expectation that Vaadin consumes App/read-model data rather than static UI state.
- `.planning/phases/09-observability-policy-tenancy-and-production-hardening/09-CONTEXT.md` — Trace/correlation identity shape and redaction discipline.
- `.planning/phases/12-console-mobile-first-flow/12-CONTEXT.md` — Adapter-web bridge pattern and stable data-hook expectations for Console flows.
- `.planning/phases/13-runtime-cards-timeline-tool-and-approval-ux/13-CONTEXT.md` — Runtime/tool/approval rendering stays adapter-web/local and redacted; raw details are diagnostics, not transcript contract.

### Existing Code Contracts to Inspect
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/SessionRepository.java` — Current session repository lacks recent typed query support and returns raw map history.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/SessionQueryService.java` — Existing session query service seam.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/session/SessionHistoryResponse.java` — Current raw `List<Map<String,Object>>` history contract to replace for transcript use.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcSessionRepository.java` — Current sessions/session_entries JDBC implementation.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunProjectionRepository.java` — Current run projection query patterns, including ownership-filter gaps on list methods.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunEventStore.java` — Current event readback payload typing issue.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/SessionController.java` — Existing REST session endpoint shape.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/ConsoleRunExecutionBridge.java` and `AppConsoleRunExecutionBridge.java` — Minimal Console bridge hook location.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/console/SessionListPanel.java` and `ChatEventStreamPanel.java` — Current in-memory UI panels to avoid treating as source of truth.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `RequestContext` / `SecurityPrincipalContext`: canonical carrier for tenant/user ownership in App-layer queries.
- `SessionQueryService` and `SessionRepository`: existing session query seam to either extend behind a new Conversation boundary or delegate from the Conversation use case.
- `PageResponse<T>`: reusable response wrapper for recent session lists and transcript pagination, though cursor semantics may need adaptation.
- `RunEventDtoMapper` and `RuntimeDetailRedactor`: existing redaction/payload handling patterns useful for safe transcript previews and tool/error summaries.
- `ConsoleRunExecutionBridge` / `AppConsoleRunExecutionBridge`: established adapter-web seam for minimal Console read-model hook.
- `DeterministicIds`, `DeterministicClock`, fake repositories/test patterns in App tests: useful for golden transcript and ownership tests.

### Established Patterns
- COLA boundaries are enforced by ArchUnit: Domain must remain free of Spring/Jackson/Jakarta/Vaadin/DB dependencies; App may depend on client/domain but not infrastructure/adapter.
- REST APIs are session-centric, and previous phases prefer additive extension over endpoint churn.
- Vaadin Console state is adapter-web presentation state, not product history or source of truth.
- Runtime/event diagnostic details may exist as collapsible/raw diagnostics, while user-facing conversation transcript should be typed and reduced.

### Integration Points
- App: add a Conversation query use case returning typed recent session summaries and transcript responses.
- Client: add conversation DTOs for session summary, message role/status, transcript response, and metadata/redaction/visibility fields.
- Infrastructure: implement ownership-safe recent/transcript queries and patch any run projection/event query gaps needed by transcript assembly.
- Adapter Web REST: expose typed conversation read model through session-centric endpoints.
- Adapter Web Console: add minimal bridge methods to load recent sessions/transcript for proof tests; leave full UX to Phase 17.

</code_context>

<specifics>
## Specific Ideas

- User selected “新 Conversation 边界” for read model organization.
- User selected session-centric REST paths for the read model.
- User wants minimal Console proof hooks in Phase 16 rather than full UI polish.
- User selected projection-table-first transcript strategy, with four roles (`user`, `assistant`, `tool`, `error`) and message status fields.
- User selected latest conversation activity sorting, stable first-user-message-derived title, latest visible message preview, and status derived from recent run state.
- User initially preferred replacing old raw endpoints and doing minimal SQLite. This context narrows that to replacing raw session-history/transcript use as the chat contract while preserving diagnostic run/event endpoints, and allowing only minimal SQLite conversation-read-model alignment without Phase 20 local profile expansion.

</specifics>

<deferred>
## Deferred Ideas

- Full Kimi-style Console visual restore UX — Phase 17.
- Streaming assistant bubble lifecycle, pending/delta/terminal/cancel reducer, and real streaming validation — Phase 18.
- Multi-turn context assembly into model requests — Phase 19.
- Provider/model readiness, fallback labeling, local profile persistence/restart behavior, and full SQLite product stability — Phase 20.
- Conversation search, rename/archive/pin/delete, branching/edit/regenerate, export/import, prompt templates, RAG/long-term memory, and automatic paid-provider fallback — future requirements/out of scope.

</deferred>

---

*Phase: 16-conversation-read-model-and-recent-sessions*
*Context gathered: 2026-06-28*
