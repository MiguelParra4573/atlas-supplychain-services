-- Solo tests: crea el schema auth y la secuencia de ids cortos de empresa, que Hibernate
-- (ddl-auto: create-drop) no genera porque no cuelga de ningún @GeneratedValue. En entornos
-- reales ambos los crean platform/postgres/init y el changelog de Liquibase.
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SEQUENCE IF NOT EXISTS auth.company_id_seq START WITH 1 INCREMENT BY 1;
