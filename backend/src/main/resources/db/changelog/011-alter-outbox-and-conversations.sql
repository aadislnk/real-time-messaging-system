ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS last_error TEXT;

CREATE INDEX IF NOT EXISTS idx_user_blocks_lookup ON user_blocks (blocker_id, blocked_id);
CREATE INDEX IF NOT EXISTS idx_user_blocks_reverse ON user_blocks (blocked_id, blocker_id);
