-- Solo tests liquibase-it: Liquibase no crea el schema, lo crea platform/postgres/init en entornos reales.
CREATE SCHEMA IF NOT EXISTS auth;
