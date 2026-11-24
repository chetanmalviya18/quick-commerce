-- Seed default admin user for development and management
-- Email: admin@quickcommerce.com
-- Password: admin123 (BCrypt hashed)
INSERT INTO users (email, password_hash, role)
VALUES (
    'admin@quickcommerce.com',
    '$2a$10$/YBnZL1IjiDWJblSBDlnJu6PsBTSvdTS1wQJ4CXzOtpYmECymv25e',
    'ADMIN'
);
