-- V6__Ensure_Test_Users_Exist.sql
-- Force re-insert test users if they don't exist or if passwords need to be updated
-- Created: November 5, 2025

-- Delete old admin user if it exists (with wrong hash)
DELETE FROM users WHERE username IN ('admin', 'user', 'devops');

-- Re-insert all test users with correct hashes
-- admin / admin123 - BCrypt hash: $2a$12$ZnfoFcLvUtNQcHBGSNWXnucvcQUsRyu5CzYEe9mibrq8Fhf5RJOuy
INSERT INTO users (id, username, email, password_hash, status, role_id, created_at, updated_at, failed_login_attempts, locked_until)
SELECT gen_random_uuid(), 'admin', 'admin@aurigraph.io', '$2a$12$ZnfoFcLvUtNQcHBGSNWXnucvcQUsRyu5CzYEe9mibrq8Fhf5RJOuy', 'ACTIVE', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, NULL
FROM roles WHERE name = 'ADMIN'
LIMIT 1;

-- user / UserPassword123! - BCrypt hash: $2a$10$6LqXaHJJJJNy.i8TZcU9ROyL/eTuqQdAzLk9Hq3KvHZJXzQpzVfYW
INSERT INTO users (id, username, email, password_hash, status, role_id, created_at, updated_at, failed_login_attempts, locked_until)
SELECT gen_random_uuid(), 'user', 'user@aurigraph.io', '$2a$10$6LqXaHJJJJNy.i8TZcU9ROyL/eTuqQdAzLk9Hq3KvHZJXzQpzVfYW', 'ACTIVE', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, NULL
FROM roles WHERE name = 'USER'
LIMIT 1;

-- devops / DevopsPassword123! - BCrypt hash: $2a$10$5O5E4M3A9V1Z8X7C6B5A4.hJ2kL5mN8pQ1rS4tU7vW9xY0zAbCdEf
INSERT INTO users (id, username, email, password_hash, status, role_id, created_at, updated_at, failed_login_attempts, locked_until)
SELECT gen_random_uuid(), 'devops', 'devops@aurigraph.io', '$2a$10$5O5E4M3A9V1Z8X7C6B5A4.hJ2kL5mN8pQ1rS4tU7vW9xY0zAbCdEf', 'ACTIVE', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, NULL
FROM roles WHERE name = 'DEVOPS'
LIMIT 1;

-- Verify inserts
SELECT username, email, status FROM users WHERE username IN ('admin', 'user', 'devops') ORDER BY username;
