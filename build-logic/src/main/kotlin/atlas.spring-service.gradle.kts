// Convención para un microservicio Spring Boot: plugin de Boot, starters comunes a todos los
// servicios (web, validación, actuator, métricas) y nombre del jar igual al del módulo.
plugins {
    id("atlas.java-conventions")
    id("org.springframework.boot")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    // developmentOnly no hereda de implementation, así que necesita el BOM por su cuenta.
    developmentOnly(platform(libs.findLibrary("spring-boot-dependencies").get()))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName = "${project.name}.jar"
}

tasks.named<Jar>("jar") {
    enabled = false
}
