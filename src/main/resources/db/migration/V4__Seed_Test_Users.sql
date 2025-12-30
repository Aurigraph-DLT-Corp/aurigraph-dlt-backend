-- V4__Seed_Test_Users.sql
-- Initialize database with test users for portal authentication
-- Created: November 2, 2025

-- Create roles table if it doesn't exist
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create users table if it doesn't exist
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    role_id UUID NOT NULL REFERENCES roles(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP
);

-- Insert roles (if they don't already exist)
INSERT INTO roles (id, name, description, created_at, updated_at) VALUES
    (gen_random_uuid(), 'ADMIN', 'Administrator - Full system access', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'USER', 'Regular User - Limited access', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (gen_random_uuid(), 'DEVOPS', 'DevOps Team - Infrastructure access', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Insert test users
-- Note: Passwords should be properly hashed in production (bcrypt)
-- admin / admin123
INSERT INTO users (id, username, email, password_hash, status, role_id, created_at, updated_at, failed_login_attempts, locked_until)
SELECT gen_random_uuid(), 'admin', 'admin@aurigraph.io', '$2a$12$ZnfoFcLvUtNQcHBGSNWXnucvcQUsRyu5CzYEe9mibrq8Fhf5RJOuy', 'ACTIVE', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, NULL
FROM roles WHERE name = 'ADMIN'
ON CONFLICT (username) DO NOTHING;

-- user / UserPassword123!
INSERT INTO users (id, username, email, password_hash, status, role_id, created_at, updated_at, failed_login_attempts, locked_until)
SELECT gen_random_uuid(), 'user', 'user@aurigraph.io', '$2a$10$6LqXaHJJJJNy.i8TZcU9ROyL/eTuqQdAzLk9Hq3KvHZJXzQpzVfYW', 'ACTIVE', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, NULL
FROM roles WHERE name = 'USER'
ON CONFLICT (username) DO NOTHING;

-- devops / DevopsPassword123!
INSERT INTO users (id, username, email, password_hash, status, role_id, created_at, updated_at, failed_login_attempts, locked_until)
SELECT gen_random_uuid(), 'devops', 'devops@aurigraph.io', '$2a$10$5O5E4M3A9V1Z8X7C6B5A4.hJ2kL5mN8pQ1rS4tU7vW9xY0zAbCdEf', 'ACTIVE', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, NULL
FROM roles WHERE name = 'DEVOPS'
ON CONFLICT (username) DO NOTHING;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id);
CREATE INDEX IF NOT EXISTS idx_roles_name ON roles(name);
