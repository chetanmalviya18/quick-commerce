-- Email: admin@quickcommerce.com
-- Password: service123 (BCrypt hashed)
INSERT INTO users (email, password_hash, role)
VALUES (
           'order-service@internal.local',
           '$2a$12$eLJ.pDGm36npEyfGsEbYquRW5efBAOrvVA6D3hK8zTAyZh9lxSDJC',
           'SERVICE'
       );