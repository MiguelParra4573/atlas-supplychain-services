plugins {
    id("atlas.java-conventions")
    `java-library`
}

description =
    "Tipos y constantes compartidos entre servicios: envelope de eventos (ADR-003) y " +
        "unidades/moneda/zona horaria (ADR-012). Sin dependencia de Spring: cualquier módulo lo " +
        "importa, incluido uno que no sea un servicio Spring Boot."

dependencies {
    // api: el payload del envelope es JsonNode, tipo que todo consumidor del envelope necesita
    // ver en su propio classpath de compilación.
    api("tools.jackson.core:jackson-databind")
    // La gestiona el BOM de Boot; se declara porque el código la importa directo (@NullMarked).
    implementation("org.jspecify:jspecify")
}
