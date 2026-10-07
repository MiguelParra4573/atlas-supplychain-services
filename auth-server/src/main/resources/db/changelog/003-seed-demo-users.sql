--liquibase formatted sql

--changeset atlas:003-seed-demo-users context:demo
-- Usuarios de demo (solo con LIQUIBASE_CONTEXTS=demo). Password de ambos: Admin#2026 (BCrypt).
-- Nunca usar estas credenciales fuera de desarrollo local.

INSERT INTO auth.users (email, failed_attempts, id_company, last_name, lock_until, name, password_changed_at, password_expires_at, password_hash, state, id_role, username, id_user) VALUES ('admin@atlas.local', 0, '01', 'Atlas', NULL, 'Admin', '2026-09-23 05:15:16.069764+00', NULL, '$2a$10$JC/pkjng3IBuA39YYFK7MOd/FqH5GZcNl6AY9ViYHm07chVRPUhX6', true, 2, 'admin.atlas', 1);
INSERT INTO auth.users (email, failed_attempts, id_company, last_name, lock_until, name, password_changed_at, password_expires_at, password_hash, state, id_role, username, id_user) VALUES ('demo.user@atlas.local', 0, '01', 'Demo', NULL, 'Usuario', '2026-09-23 05:15:16.069764+00', NULL, '$2a$10$JC/pkjng3IBuA39YYFK7MOd/FqH5GZcNl6AY9ViYHm07chVRPUhX6', true, 2, 'demo.user', 2);
SELECT pg_catalog.setval('auth.users_id_user_seq', 2, true);
