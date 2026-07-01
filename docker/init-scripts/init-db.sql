-- PostgreSQL init script for Keycloak
-- This runs automatically when the container first starts

-- Ensure UTF8 encoding
SET client_encoding = 'UTF8';

-- Create extensions (optional but useful)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- The keycloak database and user are created via env vars
-- This script can be used for additional setup
