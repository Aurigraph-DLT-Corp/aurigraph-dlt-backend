-- V5__Fix_User_Default_Values.sql
-- Update existing user records to set default values for login attempt tracking

UPDATE users SET failed_login_attempts = 0 WHERE failed_login_attempts IS NULL;
UPDATE users SET locked_until = NULL WHERE locked_until IS NULL;

-- Ensure the column is NOT NULL with proper default
ALTER TABLE users ALTER COLUMN failed_login_attempts SET DEFAULT 0;
ALTER TABLE users ALTER COLUMN failed_login_attempts SET NOT NULL;
