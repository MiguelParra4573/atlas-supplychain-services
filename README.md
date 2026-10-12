# Atlas Supply Chain: services

Backend de la plataforma. El frontend vive en el repo hermano `atlas-supplychain-client` y la
infraestructura local (el compose) en `atlas-supplychain-platform`; los tres se clonan en la misma
carpeta.

Plataforma modular de supply chain (catálogo, inventario, compras, producción, ventas, distribución y analítica) construida como proyecto de práctica de arquitectura: hexagonal, CQRS, Event Sourcing, sagas y Transactional Outbox. El plan completo está en [`docs/Plan de implementación plataforma Supply Chain (portafolio).md`](docs/Plan%20de%20implementaci%C3%B3n%20plataforma%20Supply%20Chain%20(portafolio).md).

## Estado

Fase 0 (fundamentos) en curso. Módulo existente: `auth-server` (IdP propio con JWT RS256 y JWKS).

## Requisitos

- JDK 25 (el wrapper de Gradle resuelve el toolchain)
- Podman corriendo, para el compose de `../platform/` (Testcontainers necesita además un socket compatible con Docker)
- Git Bash o similar para `../platform/keys/generate-keys.sh`

## Estructura

```text
auth-server/     microservicio IdP (los demás servicios se agregan como carpetas hermanas)
build-logic/     plugins de convención Gradle (atlas.java-conventions, atlas.spring-service)
docs/            plan, specs y ADRs
```

## Comandos

```bash
./gradlew :auth-server:test              # unitarios
./gradlew :auth-server:integrationTest   # integración (requiere Docker)
./gradlew build                                   # todo, incluye integrationTest
```

## Infraestructura local

El compose vive en el repo `atlas-supplychain-platform`, clonado al lado de este. Desde `../platform/`:

```bash
cp .env.example .env     # una vez; ajustar POSTGRES_PORT si 5432 está ocupado
podman compose --env-file .env up -d                         # Postgres, RabbitMQ, Mailpit, Redis
podman compose --env-file .env --profile app up -d --build   # + auth-server en contenedor
```

| Servicio | URL / puerto |
|---|---|
| auth-server | http://localhost:9000/api/v1 (login: `POST /auth/login` con `usuario`/`contrasena`) |
| Postgres 17 | `localhost:${POSTGRES_PORT}` (schema `auth`, usuario `auth_user`) |
| RabbitMQ (consola) | http://localhost:15672 (`atlas`/`atlas`) |
| Mailpit (bandeja) | http://localhost:8025 |

Para desarrollar sin contenedor de auth-server: levantar solo la infraestructura y correr `./gradlew :auth-server:bootRun` (con `LIQUIBASE_CONTEXTS=demo` para el usuario `admin.atlas` / `Admin#2026`; si cambiaste `POSTGRES_PORT`, también `DB_URL`). Las init de `../platform/postgres/init/` solo corren al crear el volumen; para repetirlas: `podman compose ... down -v`.

## Convenciones

- Un schema de Postgres y un usuario de BD por servicio; sin joins entre schemas.
- APIs bajo `/api/v1`, errores con `ProblemDetail` (RFC 9457).
- Secretos por variables de entorno (ver `.env.example`); las claves `.pem` no se versionan.
- Ramas por spec; commits en formato convencional (`feat:`, `fix:`, `docs:`, `chore:`).
