DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'sessions' AND column_name = 'trace_id'
    ) THEN
        EXECUTE 'UPDATE sessions
                SET trace_id = lower(replace(trace_id, ''-'', ''''))
                WHERE trace_id ~* ''^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$''';
    END IF;
END $$;

UPDATE runs
SET trace_id = lower(replace(trace_id, '-', ''))
WHERE trace_id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';

UPDATE run_events
SET trace_id = lower(replace(trace_id, '-', ''))
WHERE trace_id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';

UPDATE audit_records
SET trace_id = lower(replace(trace_id, '-', ''))
WHERE trace_id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';

UPDATE run_queue
SET trace_id = lower(replace(trace_id, '-', ''))
WHERE trace_id ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';
