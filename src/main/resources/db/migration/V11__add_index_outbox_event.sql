CREATE INDEX idx_outbox_event_status_processing_started_date
    ON outbox_event (status, processing_started_date);
