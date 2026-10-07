// Convención base para todo módulo Java del monorepo: toolchain, BOM de Spring Boot,
// Lombok y JUnit Platform. Los servicios Spring Boot usan atlas.spring-service encima de esta.
plugins {
    java
}

val libs = the<VersionCatalogsExtension>().named("libs")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.findVersion("java").get().requiredVersion)
    }
}

dependencies {
    val bom = platform(libs.findLibrary("spring-boot-dependencies").get())
    implementation(bom)
    annotationProcessor(bom)
    testImplementation(bom)
    testAnnotationProcessor(bom)

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

// Unitarios en `test`; los *IntegrationTest (Spring context, Testcontainers) en `integrationTest`.
val integrationTestPattern = "**/*IntegrationTest*"

tasks.named<Test>("test") {
    useJUnitPlatform()
    exclude(integrationTestPattern, "**/*ApplicationTests*")
}

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Tests de integración (contexto Spring y contenedores)."
    group = "verification"
    useJUnitPlatform()
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    include(integrationTestPattern, "**/*ApplicationTests*")
    shouldRunAfter(tasks.named("test"))
    configureContainerRuntime()
}

// Testcontainers corre sobre Docker. Si hay DOCKER_HOST (o -Patlas.docker.host) se respeta; si no,
// Testcontainers detecta el Docker local por su cuenta (Docker Desktop en Windows, socket en Linux/CI).
fun Test.configureContainerRuntime() {
    val dockerHost = providers.environmentVariable("DOCKER_HOST")
        .orElse(providers.gradleProperty("atlas.docker.host"))
        .orNull
    if (dockerHost != null) {
        environment("DOCKER_HOST", dockerHost)
    }
}

tasks.named("check") {
    dependsOn(integrationTest)
}
