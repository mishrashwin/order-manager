-- Drop legacy users.verified column.
-- Verification state is managed by users.enabled.
-- Keep password_reset_tokens.verified (different use case) untouched.
ALTER TABLE users
DROP COLUMN IF EXISTS verified;


