CREATE INDEX idx_run_events_tool_lifecycle
    ON run_events(run_id, event_type, sequence)
    WHERE event_type LIKE 'tool.%';

CREATE INDEX idx_audit_tool_resource_timestamp
    ON audit_records(resource_type, resource_id, timestamp DESC)
    WHERE resource_type = 'tool';
