ALTER TABLE users
    ADD COLUMN IF NOT EXISTS account_active BOOLEAN;

UPDATE users
SET account_active = TRUE
WHERE account_active IS NULL;

ALTER TABLE users
    ALTER COLUMN account_active SET DEFAULT TRUE,
    ALTER COLUMN account_active SET NOT NULL;

