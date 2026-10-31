// Convención base para todo módulo Java del monorepo: toolchain, BOM de Spring Boot,
// Lombok y JUnit Platform. Los servicios Spring Boot usan atlas.spring-service encima de esta.
//
// Harness de calidad (Spotless, javac estricto, Error Prone, NullAway, higiene de
// resolución, JaCoCo): el por qué de cada elección está en
// docs/adr/013-harness-de-calidad.md, en la raíz del workspace. El cómo, en
// convenciones.md de este repo.
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.math.BigDecimal

plugins {
    java
    jacoco
    id("com.diffplug.spotless")
    id("net.ltgt.errorprone")
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

    errorprone(libs.findLibrary("errorprone-core").get())
    errorprone(libs.findLibrary("nullaway").get())
}

// Higiene de resolución: equivalente de lo que en Maven da maven-enforcer. Sin esto,
// una LATEST o un SNAPSHOT externo hacen que el build de hoy no sea el build de hace
// tres meses. La versión de Java la fija el toolchain de arriba, así que no hace
// falta una regla de rango.
configurations.all {
    resolutionStrategy {
        failOnDynamicVersions()
        failOnChangingVersions()
    }
}

// Interruptores de escape. CI no pasa ninguno de los dos: CI siempre es estricto.
val fallarPorWarning = (findProperty("atlas.fallarPorWarning") as String?)?.toBoolean() ?: true
val sinCalidadEstricta = hasProperty("atlas.sinCalidadEstricta")

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
    // -processing fuera: con Lombok, MapStruct y QueryDSL activos a la vez es ruido
    // garantizado. deprecation y removal se mantienen: son la señal de la migración a
    // Spring 7.
    options.compilerArgs.add(
        "-Xlint:all,-processing,-serial,-this-escape,-missing-explicit-ctor,-requires-automatic,-requires-transitive-automatic",
    )
    if (fallarPorWarning) {
        options.compilerArgs.add("-Werror")
    }

    // Clases generadas por QueryDSL (Q*) y MapStruct (*Impl): ni Spotless ni Error
    // Prone las tienen que mirar, porque no las escribe nadie a mano.
    options.errorprone.excludedPaths.set(".*/generated/.*")
    options.errorprone.isEnabled.set(!sinCalidadEstricta)

    options.errorprone {
        // Chequeos de Javadoc apagados: con Lombok 1.18.42 en JDK 25, las anotaciones
        // generadas (getters, builders) producen Javadoc inválido que estos chequeos
        // leen como bug real y no lo es. Issue abierto:
        // https://github.com/projectlombok/lombok/issues/3940. Se retiran cuando esa
        // combinación quede resuelta en Lombok.
        check("InvalidLink", CheckSeverity.OFF)
        check("InvalidInlineTag", CheckSeverity.OFF)
        check("InvalidParam", CheckSeverity.OFF)
        check("InvalidThrows", CheckSeverity.OFF)
        check("EmptyBlockTag", CheckSeverity.OFF)
        check("MissingSummary", CheckSeverity.OFF)

        // NullAway: el alcance lo declara el código (un package-info.java @NullMarked
        // por paquete de dominio/aplicación), no una lista en el build.
        check("NullAway", CheckSeverity.ERROR)
        option("NullAway:OnlyNullMarked", "true")
        option("NullAway:JSpecifyMode", "false")
        option("NullAway:AcknowledgeRestrictiveAnnotations", "true")
        option("NullAway:CheckOptionalEmptiness", "true")
    }
}

// Error Prone no corre sobre src/test: los tests usan fixtures que pasan null a
// propósito y construyen estados inválidos para probar las validaciones.
tasks.named<JavaCompile>("compileTestJava") {
    options.errorprone.isEnabled.set(false)
}

// Formato. `./gradlew :modulo:spotlessApply` formatea; `spotlessCheck` (en `check`)
// falla el build. google-java-format en AOSP: 4 espacios de bloque, 8 de
// continuación, 100 columnas — lo que ya declara el .editorconfig del repo.
spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat(libs.findVersion("google-java-format").get().requiredVersion)
            .aosp()
            .reflowLongStrings()
            .formatJavadoc(true)
        formatAnnotations()
        // Sin importOrder ni removeUnusedImports: los hace el propio paso de
        // google-java-format (fixImports), y dos motores sobre el mismo archivo se
        // pelean en cada apply.
    }
    // El markdown no se formatea: lo gobierna el .editorconfig y nada más.
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
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
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

// Cobertura. El umbral se fija midiendo, nunca a ojo: ver el procedimiento en
// convenciones.md § Cobertura. Solo sube; bajarlo exige una entrada en un checkpoint
// o en el ADR con la razón.
jacoco {
    toolVersion = libs.findVersion("jacoco").get().requiredVersion
}

// Cableado, generado o entry point: un @SpringBootTest o un main() los cubre por el
// hecho de arrancar, sin probar nada. Q* es QueryDSL; *MapperImpl es MapStruct.
val jacocoExclusions = listOf(
    "**/*Application.class",
    "**/*Config.class",
    "**/*Config\$*.class",
    "**/Q*.class",
    "**/*MapperImpl.class",
)

val jacocoTestReport = tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"), integrationTest)
    executionData(tasks.named<Test>("test").get(), integrationTest.get())
    classDirectories.setFrom(
        files(classDirectories.files.map { fileTree(it) { exclude(jacocoExclusions) } }),
    )
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(jacocoTestReport)
    executionData(tasks.named<Test>("test").get(), integrationTest.get())
    classDirectories.setFrom(jacocoTestReport.get().classDirectories)
    // Medición del 2026-10-29 sobre test + integrationTest de auth-server: LINE 0,7301,
    // BRANCH 0,5354. Procedimiento en convenciones.md § Cobertura: el umbral va en la medición
    // redondeada hacia abajo al 0,05, nunca al 0,01. Solo sube; bajarlo exige una entrada con la
    // razón en services/.claude/checkpoints/ o en un ADR.
    violationRules {
        isFailOnViolation = true
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.70")
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.50")
            }
        }
    }
}

tasks.named("check") {
    dependsOn(integrationTest, "jacocoTestCoverageVerification")
}
