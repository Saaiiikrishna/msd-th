-- V9__update_outbox_events_schema.sql
-- Updates outbox_events table to match the current OutboxEventEntity JPA entity

-- Add missing columns to outbox_events table
ALTER TABLE outbox_events 
ADD COLUMN last_error VARCHAR(2000),
ADD COLUMN next_retry_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN processing_started_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN published_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN kafka_offset BIGINT,
ADD COLUMN kafka_partition INTEGER,
ADD COLUMN last_retry_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING',
ADD COLUMN correlation_id VARCHAR(100),
ADD COLUMN causation_id VARCHAR(100),
ADD COLUMN message_id VARCHAR(100);

-- Add constraints for the new columns
ALTER TABLE outbox_events ADD CONSTRAINT chk_status 
    CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'));

-- Add indexes for performance
CREATE INDEX idx_outbox_events_status ON outbox_events(status, created_at);
CREATE INDEX idx_outbox_events_retry ON outbox_events(retry_count, created_at);
CREATE INDEX idx_outbox_events_correlation ON outbox_events(correlation_id) WHERE correlation_id IS NOT NULL;
CREATE INDEX idx_outbox_events_causation ON outbox_events(causation_id) WHERE causation_id IS NOT NULL;
CREATE INDEX idx_outbox_events_next_retry ON outbox_events(next_retry_at) WHERE next_retry_at IS NOT NULL;

-- Update existing records to have PENDING status
UPDATE outbox_events SET status = 'PENDING' WHERE status IS NULL;

-- Add comments for documentation
COMMENT ON COLUMN outbox_events.last_error IS 'Last error message if processing failed';
COMMENT ON COLUMN outbox_events.next_retry_at IS 'When to retry processing this event';
COMMENT ON COLUMN outbox_events.processing_started_at IS 'When processing of this event started';
COMMENT ON COLUMN outbox_events.published_at IS 'When this event was successfully published';
COMMENT ON COLUMN outbox_events.kafka_offset IS 'Kafka offset where this event was published';
COMMENT ON COLUMN outbox_events.kafka_partition IS 'Kafka partition where this event was published';
COMMENT ON COLUMN outbox_events.last_retry_at IS 'When this event was last retried';
COMMENT ON COLUMN outbox_events.status IS 'Current processing status of the event';
COMMENT ON COLUMN outbox_events.correlation_id IS 'Correlation ID for tracing related events';
COMMENT ON COLUMN outbox_events.causation_id IS 'ID of the event that caused this event';
COMMENT ON COLUMN outbox_events.message_id IS 'Unique message identifier';
