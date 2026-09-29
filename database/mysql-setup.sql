CREATE DATABASE IF NOT EXISTS ecobridge
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Create a dedicated application user instead of connecting as root.
-- Run as a MySQL administrator. Replace the example password locally and never commit it.
CREATE USER IF NOT EXISTS 'ecobridge_app'@'localhost' IDENTIFIED BY 'replace-with-a-strong-password';
GRANT ALL PRIVILEGES ON ecobridge.* TO 'ecobridge_app'@'localhost';
FLUSH PRIVILEGES;
