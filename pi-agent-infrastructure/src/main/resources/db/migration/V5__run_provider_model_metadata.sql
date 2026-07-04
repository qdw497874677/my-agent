ALTER TABLE runs
    ADD COLUMN provider_metadata jsonb NOT NULL DEFAULT '{}'::jsonb;
