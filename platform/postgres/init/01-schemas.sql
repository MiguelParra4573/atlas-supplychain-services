-- Se ejecuta una sola vez, al crear el volumen de Postgres (docker-entrypoint-initdb.d).
-- Un schema y un usuario de BD por servicio; sin acceso cruzado entre schemas.
-- Valores de desarrollo local: nunca usar en producción.

-- auth-server
CREATE ROLE auth_user LOGIN PASSWORD 'auth_pass';
CREATE SCHEMA auth AUTHORIZATION auth_user;
ALTER ROLE auth_user SET search_path = auth;
REVOKE ALL ON SCHEMA public FROM auth_user;

-- Para servicios nuevos: copiar el bloque anterior con su propio rol y schema.
