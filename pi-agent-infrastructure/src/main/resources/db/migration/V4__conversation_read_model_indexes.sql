-- Phase 16 Plan 03: Conversation read-model query indexes (additive only).
-- These composite indexes back the ownership-aware, tenant/user-scoped recent
-- session, session-owned run, and session/run event transcript queries
-- (decisions D-09, D-11, D-12, D-15). No existing migration or table is
-- rewritten; V1 already provides run_id/session-scoped indexes used by the
-- diagnostic replay path.

-- Recent sessions: tenant/user ownership filter ordered by latest activity.
CREATE INDEX IF NOT EXISTS idx_sessions_tenant_user_updated
    ON sessions(tenant_id, user_id, updated_at DESC);

-- Session-owned runs: tenant/user/session filter ordered by creation for the
-- transcript assembler's stable run ordering (D-09, D-16).
CREATE INDEX IF NOT EXISTS idx_runs_tenant_user_session_created
    ON runs(tenant_id, user_id, session_id, created_at);

-- Ownership-safe transcript event read path: tenant/user/session/run filter
-- ordered by sequence so the conversation read model never fetches events by
-- runId alone (D-05, D-15). The existing idx_run_events_session_run remains for
-- the diagnostic path; this index adds the ownership columns.
CREATE INDEX IF NOT EXISTS idx_run_events_owner_run_sequence
    ON run_events(tenant_id, user_id, session_id, run_id, sequence);
