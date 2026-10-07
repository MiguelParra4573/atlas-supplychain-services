# Atlas Supply Chain: services

Backend de la plataforma. El frontend vive en el repo hermano `atlas-supplychain-client`.

Plataforma modular de supply chain (catálogo, inventario, compras, producción, ventas, distribución y analítica) construida como proyecto de práctica de arquitectura: hexagonal, CQRS, Event Sourcing, sagas y Transactional Outbox. El plan completo está en [`docs/Plan de implementación plataforma Supply Chain (portafolio).md`](docs/Plan%20de%20implementaci%C3%B3n%20plataforma%20Supply%20Chain%20(portafolio).md).

## Estado

Fase 0 (fundamentos) en curso. Módulo existente: `auth-server` (IdP propio con JWT RS256 y JWKS).

## Requisitos

- JDK 25 (el wrapper de Gradle resuelve el toolchain)
- Docker Desktop corriendo (Testcontainers y `docker compose`)
- Git Bash o similar para `platform/keys/generate-keys.sh`

## Estructura

```text
auth-server/     microservicio IdP (los demás servicios se agregan como carpetas hermanas)
build-logic/     plugins de convención Gradle (atlas.java-conventions, atlas.spring-service)
platform/        infraestructura local: compose, init de Postgres, claves de desarrollo
docs/            plan, specs y ADRs
```

## Comandos

```bash
./gradlew :auth-server:test              # unitarios
./gradlew :auth-server:integrationTest   # integración (requiere Docker)
./gradlew build                                   # todo, incluye integrationTest
```

## Convenciones

- Un schema de Postgres y un usuario de BD por servicio; sin joins entre schemas.
- APIs bajo `/api/v1`, errores con `ProblemDetail` (RFC 9457).
- Secretos por variables de entorno (ver `.env.example`); las claves `.pem` no se versionan.
- Ramas por spec; commits en formato convencional (`feat:`, `fix:`, `docs:`, `chore:`).
