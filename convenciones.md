# Convenciones de código — atlas-supplychain-services

Cómo se escribe el código de este repo y qué lo verifica. El **por qué** de cada elección está en
`docs/adr/013-harness-de-calidad.md` (raíz del workspace); acá está el **cómo**.

La arquitectura —qué depende de qué— es otro documento: `docs/plan.md` en la raíz del workspace,
§"Estructura de repositorios" y §"Microservicios y responsabilidades".

## Puesta a punto

Una vez por clon:

```bash
git config core.hooksPath .githooks                      # activa los hooks de git
git config blame.ignoreRevsFile .git-blame-ignore-revs    # saca el reformateo del blame
```

Hacen falta además **JDK 25** (el wrapper de Gradle resuelve el toolchain por su cuenta, incluso
provisionándolo si no está instalado) y **Podman o Docker corriendo** para
`./gradlew :auth-server:integrationTest`, porque esos tests levantan Postgres y RabbitMQ reales con
Testcontainers.

## Los gates que rompen el build

| Gate | Task de Gradle | Qué verifica | Cómo se arregla |
| --- | --- | --- | --- |
| Higiene de resolución | implícito en cualquier resolución de dependencias | Sin versiones dinámicas (`LATEST`, rangos) ni `SNAPSHOT` de terceros | Fijar una versión concreta en `gradle/libs.versions.toml` |
| `spotlessCheck` | `check` | Formato de todo `src/**/*.java` | `./gradlew :auth-server:spotlessApply` |
| `-Xlint:all` + `-Werror` (javac) | `compileJava`/`compileTestJava` | Cualquier warning de javac, salvo la lista de exclusiones del plugin de convención | Arreglar el warning; como último recurso, el escape de abajo |
| Error Prone | `compileJava` | Bugs verificables (`UnusedVariable`, `StringCaseLocaleUsage`, `DefaultCharset`, `ReturnValueIgnored`…) | Arreglar; si es un falso positivo genuino, `@SuppressWarnings("NombreDelCheck")` con el por qué al lado |
| NullAway | `compileJava` | Nulos en los paquetes `@NullMarked` (`domain/` y `application/`) | Anotar `@Nullable` lo que de verdad puede ser nulo, o corregir el flujo |
| ArchUnit | `test` | Dependencias entre capas y las convenciones de código de este documento | Ver los dos archivos de reglas, abajo |
| `jacocoTestCoverageVerification` | `check` | Umbral de cobertura | Escribir el test que falta |

`spotlessCheck` corre en segundos, antes de que Testcontainers baje ninguna imagen; por eso
`pre-commit` solo corre ese.

## Formato

**google-java-format en modo AOSP**: 4 espacios de bloque, 8 de continuación, 100 columnas. No se
discute el estilo; lo aplica Spotless.

```bash
./gradlew :auth-server:spotlessApply    # formatea
./gradlew :auth-server:spotlessCheck    # verifica (lo corre check)
```

El `.editorconfig` declara el mismo ancho, así que el IDE y el formateador dicen lo mismo.

Sin pasos de `importOrder` ni `removeUnusedImports` separados: los hace el propio paso de
google-java-format (`fixImports`), y dos motores sobre el mismo archivo se pelean en cada `apply`.
El markdown **no** se formatea.

Un commit de reformateo masivo **no lleva ningún otro cambio, nunca**, y su SHA va a
`.git-blame-ignore-revs`.

## Nulabilidad

Se usa **JSpecify** (`org.jspecify:jspecify`), igual que Spring Framework 7.

El alcance de NullAway lo declara el código, no el build: un `package-info.java` por paquete que
se quiera analizar.

```java
@org.jspecify.annotations.NullMarked
package com.atlas.supplychain.auth.domain.maintenance.authorization;
```

Dentro de un paquete `@NullMarked`, todo es no-nulo salvo lo que se marque con
`@org.jspecify.annotations.Nullable`. Paquetes anotados: los 32 de `domain/` y `application/`. Los
adaptadores de `infrastructure/` se dejan **sin anotar a propósito** — repositorios con métodos
abstractos, converters, `@Configuration` con beans que rellena el framework — porque ahí NullAway
produce falsos positivos. Error Prone, en cambio, **sí** corre en los adaptadores: es donde viven
los bugs que atrapa.

Una excepción documentada: `infrastructure/config/security/jwt/` usa `java.util.Date` a propósito
porque la API de jjwt 0.13 (`JwtBuilder`/`ClaimsMutator`) solo acepta `Date`, no `Instant`.

## Cobertura

```bash
./gradlew :auth-server:test :auth-server:integrationTest :auth-server:jacocoTestReport
# reporte en auth-server/build/reports/jacoco/test/html/index.html
```

Umbral vigente: `jacocoTestCoverageVerification` en `atlas.java-conventions.gradle.kts`, hoy en
**0,70 de LINE** y **0,50 de BRANCH**, con `isFailOnViolation = true`: rompen el build.

Procedimiento para fijarlo: correr `jacocoTestReport`, leer el reporte, y poner la medición
**redondeada hacia abajo al 0,05**. No al 0,01: un umbral exacto rompe el build con un refactor de
tres líneas, y entonces lo primero que se aprende del harness es a bajarle el número.

**Medición del 2026-10-29**, con los 72 archivos de test ya existentes más `integrationTest`: LINE
0,7301 y BRANCH 0,5354. LINE queda en 0,70 y BRANCH en 0,50, los dos redondeados hacia abajo.

**El umbral solo sube.** Subirlo es parte del cierre de cada fase del plan: se lee el reporte y se
redondea hacia abajo el valor nuevo. Bajarlo pide una entrada en
`services/.claude/checkpoints/` o en un ADR, con la razón.

`BRANCH` importa más que `LINE` para reglas de negocio: una transición de estado con cuatro ramas
cubierta en una línea de test da `LINE` alto y `BRANCH` bajo, y `BRANCH` es la verdad.

## Las reglas verificadas

Dos archivos en `auth-server/src/test/.../architecture/`, con dos propósitos distintos:

| Archivo | Qué gobierna |
| --- | --- |
| `ArquitecturaTests.java` | Dependencias entre capas (`domain` / `application` / `infrastructure`) y entre módulos |
| `ConvencionesDeCodigoTests.java` | Buenas prácticas dentro de una clase, las de este documento |

Los dos analizan **solo `src/main`** (`DO_NOT_INCLUDE_TESTS`): los tests usan fixtures que pasan
`null` a propósito y repositorios de Spring Data inyectados directo en el test de integración, y
eso es legítimo. Ese filtro no es un detalle, es el invariante que hace seguras las reglas.

Lo que `ConvencionesDeCodigoTests` prohíbe:

- **Inyección por campo** (`@Autowired`, `@Value` en campos), salvo en las `*MapperImpl` que genera
  MapStruct — no son código escrito a mano, se regeneran en cada build.
- `System.out`, `System.err`, `printStackTrace`.
- `java.util.Date`, `Calendar`, `SimpleDateFormat`, salvo en `infrastructure/config/security/jwt/`
  (ver "Nulabilidad" arriba).
- `Thread.sleep`.
- Leer la hora del sistema (`Instant.now`, `LocalDateTime.now`, `OffsetDateTime.now`,
  `System.currentTimeMillis`) desde `..domain..`: el instante entra como parámetro o por un
  `Clock`, o la regla no se puede probar sin dormir el test.
- Repositorios de Spring Data usados fuera de `infrastructure/` o `application/`.
- Entidades JPA devueltas desde `..in.rest..`. Límite declarado, no disimulado: la regla mira el
  tipo de retorno crudo; un envoltorio genérico se le escapa.
- Nombres que no ubican la capa: `*Controller` fuera de `..in.rest..`, `*ServiceImpl` fuera de
  `application/`.

Si una regla molesta, se discute este documento y se cambia la regla. No se desactiva.

Un límite anotado en el código mismo de `ArquitecturaTests`/`ConvencionesDeCodigoTests`: los
paquetes están anclados al **prefijo completo** del módulo
(`com.atlas.supplychain.auth.application..`), no al segmento suelto (`..application..`), porque
`infrastructure.config.application` y `shared.infrastructure` colisionan por nombre con
`application`/`infrastructure` sin ser ese módulo. Se descubrió corriendo las reglas contra el
código real, no leyendo la documentación de ArchUnit.

Y una trampa de ArchUnit que vale escribir, porque cuesta horas encontrarla sin saberlo: una
`ArchCondition` propia usada con `noClasses().should(...)` tiene que registrar el hallazgo con
`SimpleConditionEvent.satisfied`, **no** con `violated`. `noClasses()` envuelve la condición en una
negación que da vuelta la bandera de cada evento, así que con `violated` la regla pasa sobre código
que la viola, sin dar ninguna señal. `ArquitecturaTests#noClassesInvierteLaBanderaDeLaCondicion`
fija las dos direcciones con una condición trivial, para que un cambio de versión de ArchUnit que
invierta la semántica se note ahí primero.

## Commits

Convencionales, validado por `.githooks/commit-msg`:

```
<tipo>(<alcance opcional>): <asunto en minúscula, hasta 80 caracteres>
```

Tipos: `build`, `chore`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `style`, `test`. El cuerpo
y el pie quedan libres, **salvo** los trailers `Co-Authored-By:` y `Claude-Session:`, que este hook
rechaza: es una regla de este workspace, no una convención general. Los mensajes que genera git
solo (`Merge`, `Revert`, `fixup!`, `squash!`) no se validan.

El `pre-commit` corre `spotlessCheck` y sale en milisegundos si el commit no toca `.java` ni
`.kts`. **Verifica, nunca arregla**: un hook que reescribe archivos con staging parcial deja el
índice desincronizado y commitea algo que nadie leyó.

## Escapes documentados

Dos, para el día de un upgrade de Boot o de JDK. **CI no los pasa nunca**, así que CI siempre es
estricto.

```bash
./gradlew build -Patlas.fallarPorWarning=false   # apaga -Werror
./gradlew build -Patlas.sinCalidadEstricta        # apaga Error Prone y NullAway
```

## Trampas conocidas

- **`org.gradle.jvmargs` lleva los `--add-exports`/`--add-opens` de `jdk.compiler`** que
  google-java-format necesita dentro del propio daemon de Gradle (JDK 16+). Sin ellos, Spotless
  falla con `IllegalAccessError`. Si se borran, se rompe el formateador.
- **google-java-format está pineado en 1.36.1, no en el latest.** 1.37.0 quita
  `JavaFormatterOptions.Style.valueOf(String)`, que Spotless 8.10.x todavía llama, y revienta con
  `NoSuchMethodError` en cada archivo. Se sube junto con una versión de Spotless que ya lo soporte,
  no antes.
- **Error Prone no corre sobre `compileTestJava`.** Los tests pasan `null` a propósito para probar
  validaciones, y usan Mockito/fixtures que Error Prone marcaría como bugs sin serlo.
- **Los chequeos de Javadoc de Error Prone están apagados** (`InvalidLink`, `InvalidInlineTag`,
  `InvalidParam`, `InvalidThrows`, `EmptyBlockTag`, `MissingSummary`). Lombok 1.18.42 en JDK 25
  genera Javadoc que esos chequeos leen como inválido en getters/builders
  ([issue de Lombok](https://github.com/projectlombok/lombok/issues/3940)). Se retiran cuando esa
  combinación quede resuelta en una versión nueva de Lombok.
- **Las clases generadas (QueryDSL `Q*`, MapStruct `*Impl`) están excluidas** de Error Prone
  (`excludedPaths` sobre `.*/generated/.*`) y de JaCoCo (`**/Q*.class`, `**/*MapperImpl.class`). No
  son código escrito a mano.
- **`fetchCount()` de QueryDSL está deprecado.** El reemplazo no deprecado es clonar la query y
  contar con `Wildcard.count`: `query.clone().select(Wildcard.count).fetchOne()`. Ver
  `AbstractQueryDslRepository` y `CompanyRepositoryImpl`.
