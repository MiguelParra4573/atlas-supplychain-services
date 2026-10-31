package com.atlas.supplychain.auth.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Buenas prácticas dentro de una clase, medidas y verificadas en el código real de este repo (ver
 * docs/adr/013-harness-de-calidad.md en la raíz del workspace para el por qué de cada una). Analiza
 * solo {@code src/main}; ver el Javadoc de {@link ArquitecturaTests} para por qué.
 *
 * <p>Cada regla nace verde porque el patrón que prohíbe ya mide 0 en el código: congela lo que ya
 * es cierto, no lo que debería serlo.
 */
class ConvencionesDeCodigoTests {

    private static final String BASE_PACKAGE = "com.atlas.supplychain.auth";

    // Ver el comentario equivalente en ArquitecturaTests: anclados al prefijo completo del
    // módulo, no al segmento suelto, porque infrastructure.config.application y
    // shared.infrastructure colisionan por nombre con application/infrastructure.
    private static final String DOMAIN = BASE_PACKAGE + ".domain..";
    private static final String APPLICATION = BASE_PACKAGE + ".application..";
    private static final String INFRASTRUCTURE = BASE_PACKAGE + ".infrastructure..";
    private static final String INFRASTRUCTURE_REST = BASE_PACKAGE + ".infrastructure..in.rest..";
    private static final String JWT_PACKAGE =
            BASE_PACKAGE + ".infrastructure.config.security.jwt..";

    private static final JavaClasses CLASES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages(BASE_PACKAGE);

    @Test
    void sinInyeccionPorCampo() {
        // La inyección es por constructor (@RequiredArgsConstructor de Lombok): hace la
        // dependencia visible y la clase testeable sin contenedor. Excluye las *MapperImpl que
        // genera MapStruct: no son código escrito a mano, se regeneran en cada build (quedan en
        // build/generated/, no en src/main/) y están marcadas @Generated con retención SOURCE,
        // que ArchUnit no puede leer del bytecode -- por eso se filtran por nombre, no por
        // anotación.
        ArchRule regla =
                com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields()
                        .that()
                        .areDeclaredInClassesThat()
                        .haveNameNotMatching(".*MapperImpl")
                        .should()
                        .beAnnotatedWith(Autowired.class)
                        .orShould()
                        .beAnnotatedWith(Value.class);
        regla.check(CLASES);
    }

    @Test
    void sinSystemOutNiSystemErr() {
        noClasses().should().getField(System.class, "out").check(CLASES);
        noClasses().should().getField(System.class, "err").check(CLASES);
    }

    @Test
    void sinPrintStackTrace() {
        noClasses()
                .should()
                .callMethodWhere(
                        describedAs(
                                "llama printStackTrace()",
                                call -> call.getTarget().getName().equals("printStackTrace")))
                .check(CLASES);
    }

    @Test
    void sinThreadSleep() {
        noClasses()
                .should()
                .callMethodWhere(
                        describedAs(
                                "llama Thread.sleep",
                                call ->
                                        call.getTarget()
                                                        .getOwner()
                                                        .getFullName()
                                                        .equals("java.lang.Thread")
                                                && call.getTarget().getName().equals("sleep")))
                .check(CLASES);
    }

    @Test
    void sinJavaUtilDateNiCalendarSalvoEnElPuenteConJjwt() {
        // jjwt 0.13 (JwtBuilder/ClaimsMutator) solo acepta java.util.Date; es la única excepción,
        // acotada al paquete donde se firma/valida el JWT. Ver JwtService y ServiceTokenIssuer.
        ArchRule regla =
                noClasses()
                        .that()
                        .resideOutsideOfPackage(JWT_PACKAGE)
                        .should()
                        .dependOnClassesThat()
                        .haveFullyQualifiedName("java.util.Date")
                        .orShould()
                        .dependOnClassesThat()
                        .haveFullyQualifiedName("java.util.Calendar")
                        .orShould()
                        .dependOnClassesThat()
                        .haveFullyQualifiedName("java.text.SimpleDateFormat");
        regla.check(CLASES);
    }

    @Test
    void elDominioNoLeeElRelojDelSistema() {
        // El instante entra por parámetro o por Clock; si no, la regla de negocio no se puede
        // probar sin dormir el test o mockear estático.
        ArchRule regla =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN)
                        .should()
                        .callMethodWhere(
                                describedAs(
                                        "lee el reloj del sistema",
                                        ConvencionesDeCodigoTests::esRelojDelSistema));
        regla.check(CLASES);
    }

    @Test
    void losRepositoriosDeSpringDataSoloSeUsanDesdeInfraestructuraOAplicacion() {
        ArchRule regla =
                noClasses()
                        .that()
                        .resideOutsideOfPackages(INFRASTRUCTURE, APPLICATION)
                        .should()
                        .dependOnClassesThat()
                        .areAssignableTo(org.springframework.data.repository.Repository.class);
        regla.check(CLASES);
    }

    @Test
    void losControllersNoDevuelvenEntidadesJpaDirecto() {
        // Límite declarado, no disimulado: esta regla mira el tipo de retorno CRUDO. Un
        // Mono<Entity> o ResponseEntity<Entity> no se detecta -- acá no hay reactividad ni ese
        // patrón hoy, pero si aparece, esta regla no lo atrapa por sí sola.
        ArchRule regla =
                noClasses()
                        .that()
                        .resideInAPackage(INFRASTRUCTURE_REST)
                        .should()
                        .dependOnClassesThat()
                        .areAnnotatedWith(jakarta.persistence.Entity.class);
        regla.check(CLASES);
    }

    @Test
    void nombresQueUbicanLaCapaControllerSoloEnInRest() {
        classes()
                .that()
                .haveSimpleNameEndingWith("Controller")
                .should()
                .resideInAPackage(INFRASTRUCTURE_REST)
                .check(CLASES);
    }

    @Test
    void nombresQueUbicanLaCapaServiceImplSoloEnApplication() {
        classes()
                .that()
                .haveSimpleNameEndingWith("ServiceImpl")
                .should()
                .resideInAPackage(APPLICATION)
                .check(CLASES);
    }

    private static boolean esRelojDelSistema(JavaMethodCall call) {
        String owner = call.getTarget().getOwner().getFullName();
        String name = call.getTarget().getName();
        if (owner.equals("java.time.Instant") && name.equals("now")) {
            return true;
        }
        if (owner.equals("java.time.LocalDateTime") && name.equals("now")) {
            return true;
        }
        if (owner.equals("java.lang.System") && name.equals("currentTimeMillis")) {
            return true;
        }
        return owner.equals("java.time.OffsetDateTime") && name.equals("now");
    }

    private static com.tngtech.archunit.base.DescribedPredicate<JavaMethodCall> describedAs(
            String description, java.util.function.Predicate<JavaMethodCall> predicate) {
        return com.tngtech.archunit.base.DescribedPredicate.describe(description, predicate::test);
    }
}
