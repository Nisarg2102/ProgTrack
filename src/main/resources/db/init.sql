-- =============================================================
-- ProgTrack Database Initialization Script
-- Run this ONCE before starting the application for the first time.
-- The application uses spring.jpa.hibernate.ddl-auto=update
-- so tables are auto-created. This script only creates the DB.
-- =============================================================

CREATE DATABASE IF NOT EXISTS progtrack_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE progtrack_db;

-- The following tables are auto-created by Hibernate on first boot.
-- This file is provided as a reference / manual override if needed.

-- Verify setup:
-- SHOW TABLES;
-- SELECT * FROM users;
