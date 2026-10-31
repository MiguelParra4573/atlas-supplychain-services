package com.atlas.supplychain.auth.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.mapstruct.Mapper;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dependencias entre capas y entre módulos del arquitectura hexagonal (domain / application /
 * infrastructure), contra lo descrito en docs/plan.md (raíz del workspace). El por qué de cada
 * elección de harness está en docs/adr/013-harness-de-calidad.md.
 *
 * <p>Analiza solo {@code src/main}: los tests usan a propósito cosas que estas reglas prohíben
 * ({@code @Autowired} en campos, fixtures con {@code null} explícito, repositorios de Spring Data
 * inyectados directo en el test de integración). Mezclar tests y main acá haría las reglas
 * inútiles, no solo ruidosas.
 */
class ArquitecturaTests {

    private static final String BASE_PACKAGE = "com.atlas.supplychain.auth";

    // Anclados al prefijo completo del módulo, no al segmento suelto: "..application.." también
    // matchearía infrastructure.config.application o shared.infrastructure, que son paquetes de
    // configuración/DTO con ese nombre por coincidencia, no el módulo de la arquitectura.
    private static final String DOMAIN = BASE_PACKAGE + ".domain..";
    private static final String APPLICATION = BASE_PACKAGE + ".application..";
    private static final String INFRASTRUCTURE = BASE_PACKAGE + ".infrastructure..";
    private static final String INFRASTRUCTURE_REST = BASE_PACKAGE + ".infrastructure..in.rest..";

    private static final JavaClasses CLASES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages(BASE_PACKAGE);

    @Test
    void elDominioNoDependeDeNadaFueraDeSiMismo() {
        ArchRule regla =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(APPLICATION, INFRASTRUCTURE);
        regla.check(CLASES);
    }

    @Test
    void elDominioNoConoceSpringNiJpa() {
        ArchRule regla =
                noClasses()
                        .that()
                        .resideInAPackage(DOMAIN)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(
                                "org.springframework..",
                                "jakarta.persistence..",
                                "org.hibernate..");
        regla.check(CLASES);
    }

    @Test
    void laAplicacionNoDependeDeInfraestructura() {
        ArchRule regla =
                noClasses()
                        .that()
                        .resideInAPackage(APPLICATION)
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage(INFRASTRUCTURE);
        regla.check(CLASES);
    }

    @Test
    void laAplicacionTampocoConoceSpringNiJpa() {
        // El wiring de application/ se hace a mano con @Bean en
        // infrastructure/config/application/ApplicationServiceConfig -- ni siquiera
        // @Service/@Component aparecen en application/. Ver convenciones.md.
        ArchRule regla =
                noClasses()
                        .that()
                        .resideInAPackage(APPLICATION)
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(
                                "org.springframework..",
                                "jakarta.persistence..",
                                "org.hibernate..");
        regla.check(CLASES);
    }

    @Test
    void soloInfraestructuraImplementaRepositoriosDeSpringData() {
        // Los puertos *Repository (interfaces de dominio) viven en domain/**/repositories; solo
        // sus adaptadores, asignables a Repository de Spring Data, viven en infrastructure.
        classes()
                .that()
                .areAssignableTo(Repository.class)
                .should()
                .resideInAPackage(INFRASTRUCTURE)
                .check(CLASES);
    }

    @Test
    void soloInfraestructuraTieneEntidadesJpa() {
        classes()
                .that()
                .areAnnotatedWith(Entity.class)
                .should()
                .resideInAPackage(INFRASTRUCTURE)
                .check(CLASES);
    }

    @Test
    void soloInfraestructuraTieneControllersRest() {
        classes()
                .that()
                .areAnnotatedWith(RestController.class)
                .should()
                .resideInAPackage(INFRASTRUCTURE_REST)
                .check(CLASES);
    }

    @Test
    void soloInfraestructuraTieneMappersDeMapstruct() {
        classes()
                .that()
                .areAnnotatedWith(Mapper.class)
                .should()
                .resideInAPackage(INFRASTRUCTURE)
                .check(CLASES);
    }

    /**
     * Trampa de ArchUnit (ver convenciones.md § Reglas verificadas): una {@code ArchCondition}
     * propia usada con {@code noClasses().should(...)} tiene que marcar el hallazgo con {@code
     * satisfied}, nunca {@code violated} -- {@code noClasses()} invierte la bandera de cada evento,
     * así que una condición que reporta {@code violated} para toda clase hace que {@code
     * noClasses()} la dé por buena (cero violaciones), en silencio. Esta prueba fija las dos
     * direcciones con una condición trivial, para que un cambio de versión de ArchUnit que invierta
     * la semántica se note acá primero, no en una regla real.
     */
    @Test
    void noClassesInvierteLaBanderaDeLaCondicion() {
        ArchRule conViolatedNoAtrapaNada = noClasses().should(condicionTrivial(false));
        ArchRule conSatisfiedSiAtrapa = noClasses().should(condicionTrivial(true));

        // La trampa en acción: marcar el hallazgo como "violated" hace que noClasses() lo trague.
        conViolatedNoAtrapaNada.check(CLASES);

        // La forma correcta: "satisfied" es lo que noClasses() necesita para marcar la violación.
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> conSatisfiedSiAtrapa.check(CLASES))
                .isInstanceOf(AssertionError.class);
    }

    private static com.tngtech.archunit.lang.ArchCondition<
                    com.tngtech.archunit.core.domain.JavaClass>
            condicionTrivial(boolean comoSatisfied) {
        return new com.tngtech.archunit.lang.ArchCondition<
                com.tngtech.archunit.core.domain.JavaClass>("existir") {
            @Override
            public void check(
                    com.tngtech.archunit.core.domain.JavaClass item,
                    com.tngtech.archunit.lang.ConditionEvents events) {
                events.add(
                        comoSatisfied
                                ? com.tngtech.archunit.lang.SimpleConditionEvent.satisfied(
                                        item, "siempre satisfied")
                                : com.tngtech.archunit.lang.SimpleConditionEvent.violated(
                                        item, "siempre violated"));
            }
        };
    }
}
