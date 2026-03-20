-- Increase mobile_number column length to accommodate E.164 format (+{countryCode}{localNumber})
-- E.164 max is 15 digits + 1 for '+' = 16 chars; using 20 for safety
ALTER TABLE users ALTER COLUMN mobile_number TYPE VARCHAR(20);
