-- Clean up failed V19 migration from Flyway schema history
-- Run this manually in your database to allow V19 to be retried

-- First check if V19 entry exists in flyway_schema_history
-- SELECT * FROM flyway_schema_history WHERE version = '19';

-- If V19 exists with failed status, remove it
DELETE FROM flyway_schema_history WHERE version = '19';

-- This will allow V19 migration to run again when you restart the application
