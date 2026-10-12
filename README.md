# Atlas Supply Chain: services

Backend de la plataforma. El frontend vive en el repo hermano `atlas-supplychain-client`.

Plataforma modular de supply chain (catálogo, inventario, compras, producción, ventas, distribución y analítica) construida como proyecto de práctica de arquitectura: hexagonal, CQRS, Event Sourcing, sagas y Transactional Outbox. El plan completo está en [`docs/Plan de implementación plataforma Supply Chain (portafolio).md`](docs/Plan%20de%20implementaci%C3%B3n%20plataforma%20Supply%20Chain%20(portafolio).md).

## Estado

Fase 0 (fundamentos) en curso. Módulo existente: `auth-server` (IdP propio con JWT RS256 y JWKS).

## Requisitos

- JDK 25 (el wrapper de Gradle resuelve el toolchain)
- Podman corriendo (Testcontainers necesita además un socket compatible con Docker)
- Git Bash o similar para los scripts de desarrollo

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

El `compose.yaml` que levanta Postgres, RabbitMQ, Mailpit, Redis y el `auth-server` no vive en este
repo, porque lo comparten el backend y el frontend. Las instrucciones están en `docs/entorno-local.md`.

| Servicio | URL / puerto |
|---|---|
| auth-server | http://localhost:9000/api/v1 (login: `POST /auth/login` con `usuario`/`contrasena`) |
| Postgres 17 | `localhost:${POSTGRES_PORT}` (schema `auth`, usuario `auth_user`) |
| RabbitMQ (consola) | http://localhost:15672 |
| Mailpit (bandeja) | http://localhost:8025 |

## Convenciones

- Un schema de Postgres y un usuario de BD por servicio; sin joins entre schemas.
- APIs bajo `/api/v1`, errores con `ProblemDetail` (RFC 9457).
- Secretos por variables de entorno (ver `.env.example`); las claves `.pem` no se versionan.
- Ramas por spec; commits en formato convencional (`feat:`, `fix:`, `docs:`, `chore:`).
