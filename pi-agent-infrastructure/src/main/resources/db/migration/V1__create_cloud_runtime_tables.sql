CREATE TABLE sessions (
    tenant_id text NOT NULL,
    user_id text NOT NULL,
    session_id text PRIMARY KEY,
    workspace_id text NOT NULL,
    current_entry_id text,
    status text NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE session_entries (
    entry_id text PRIMARY KEY,
    session_id text NOT NULL REFERENCES sessions(session_id) ON DELETE CASCADE,
    parent_entry_id text,
    sequence bigint NOT NULL,
    payload_type text NOT NULL,
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL,
    UNIQUE (session_id, sequence)
);

CREATE TABLE runs (
    run_id text PRIMARY KEY,
    session_id text NOT NULL REFERENCES sessions(session_id) ON DELETE CASCADE,
    tenant_id text NOT NULL,
    user_id text NOT NULL,
    workspace_id text NOT NULL,
    status text NOT NULL,
    input_type text NOT NULL,
    input jsonb NOT NULL DEFAULT '{}'::jsonb,
    terminal_result jsonb,
    failure jsonb,
    trace_id text NOT NULL,
    correlation_id text NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    started_at timestamptz,
    finished_at timestamptz,
    cancel_requested_at timestamptz,
    cancel_reason text,
    checkpoint_ref text,
    resume_token text,
    last_event_sequence bigint NOT NULL DEFAULT 0,
    runtime_snapshot_ref text
);

CREATE TABLE run_events (
    event_id text PRIMARY KEY,
    run_id text NOT NULL,
    session_id text NOT NULL,
    tenant_id text NOT NULL,
    user_id text NOT NULL,
    workspace_id text NOT NULL,
    step_id text NOT NULL,
    sequence bigint NOT NULL,
    event_type text NOT NULL,
    timestamp timestamptz NOT NULL,
    trace_id text NOT NULL,
    correlation_id text NOT NULL,
    causation_id text NOT NULL,
    visibility text NOT NULL,
    redaction jsonb NOT NULL DEFAULT '{}'::jsonb,
    payload_schema text NOT NULL,
    payload_version int NOT NULL,
    payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT unique_run_event_sequence UNIQUE (run_id, sequence)
);

CREATE TABLE steps (
    run_id text NOT NULL,
    step_id text NOT NULL,
    status text NOT NULL,
    kind text NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    started_at timestamptz,
    finished_at timestamptz,
    summary jsonb NOT NULL DEFAULT '{}'::jsonb,
    PRIMARY KEY (run_id, step_id)
);

CREATE TABLE messages (
    message_id text PRIMARY KEY,
    session_id text NOT NULL,
    run_id text,
    role text NOT NULL,
    content jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL,
    visibility text NOT NULL DEFAULT 'USER',
    redaction jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE tool_calls (
    tool_call_id text PRIMARY KEY,
    run_id text NOT NULL,
    step_id text,
    tool_name text NOT NULL,
    status text NOT NULL,
    arguments jsonb NOT NULL DEFAULT '{}'::jsonb,
    result jsonb,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    started_at timestamptz,
    finished_at timestamptz,
    visibility text NOT NULL DEFAULT 'USER',
    redaction jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE audit_records (
    audit_id uuid PRIMARY KEY,
    tenant_id text NOT NULL,
    user_id text NOT NULL,
    action text NOT NULL,
    resource_type text NOT NULL,
    resource_id text NOT NULL,
    run_id text,
    session_id text,
    trace_id text NOT NULL,
    correlation_id text NOT NULL,
    timestamp timestamptz NOT NULL,
    details jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE run_queue (
    run_id text PRIMARY KEY,
    status text NOT NULL,
    available_at timestamptz NOT NULL,
    lease_owner text,
    lease_until timestamptz,
    attempt_count int NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL
);

CREATE INDEX idx_run_events_run_sequence ON run_events(run_id, sequence);
CREATE INDEX idx_run_events_session_run ON run_events(session_id, run_id);
CREATE INDEX idx_runs_session_created ON runs(session_id, created_at DESC);
CREATE INDEX idx_runs_tenant_user_status ON runs(tenant_id, user_id, status);
CREATE INDEX idx_runs_status_created ON runs(status, created_at);
CREATE INDEX idx_steps_run_created ON steps(run_id, created_at);
CREATE INDEX idx_messages_run_created ON messages(run_id, created_at);
CREATE INDEX idx_tool_calls_run_created ON tool_calls(run_id, created_at);
CREATE INDEX idx_audit_tenant_user_timestamp ON audit_records(tenant_id, user_id, timestamp DESC);
CREATE INDEX idx_audit_run ON audit_records(run_id);
CREATE INDEX idx_audit_correlation ON audit_records(correlation_id);
CREATE INDEX idx_run_queue_status_available ON run_queue(status, available_at);
