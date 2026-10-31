plugins {
    id("atlas.spring-service")
}

description = "IdP propio: usuarios, roles, permisos, menús y emisión de JWT RS256 (JWKS)."

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation(libs.springdoc.webmvc.ui)
    runtimeOnly("org.postgresql:postgresql")

    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(libs.lombok.mapstruct.binding)

    implementation(libs.querydsl.jpa)
    annotationProcessor(variantOf(libs.querydsl.apt) { classifier("jpa") })
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
    // querydsl-core referencia org.jetbrains.annotations.Range sin traerla como dependencia;
    // sin esto, javac -Xlint:classfile avisa en cada clase de QueryDSL en el classpath.
    compileOnly(libs.jetbrains.annotations)

    implementation(libs.commons.lang3)
    // La gestiona el BOM de Boot. Llega transitiva por spring-core, pero el código la
    // importa directo (@NullMarked en los package-info, @Nullable en los modelos), y
    // depender de un transitivo que se usa en código es la deuda que explota en el
    // upgrade que lo quita.
    implementation("org.jspecify:jspecify")

    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-amqp-test")
    testImplementation("org.springframework.boot:spring-boot-resttestclient")
    testImplementation("org.springframework.boot:spring-boot-starter-restclient")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-rabbitmq")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly("com.h2database:h2")
}
