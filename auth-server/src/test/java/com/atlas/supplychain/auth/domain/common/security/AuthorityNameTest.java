package com.atlas.supplychain.auth.domain.common.security;

import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Module;
import com.atlas.supplychain.auth.domain.maintenance.authorization.model.Operation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorityNameTest {

    @Test
    void construyeAutoridadEnFormatoRecursoOperacion() {
        Operation operacion = Operation.builder().resource("USUARIOS").tag("CREAR").build();

        assertThat(AuthorityName.of(operacion)).isEqualTo("USUARIOS:CREAR");
    }

    @Test
    void normalizaEspaciosYAcentosDelRecurso() {
        Operation operacion = Operation.builder().resource("NORMAS ISO").tag("CONSULTAR").build();

        assertThat(AuthorityName.of(operacion)).isEqualTo("NORMAS_ISO:CONSULTAR");
    }

    /**
     * El fondo de la Spec 1.2: la autoridad no depende de cómo la empresa haya etiquetado el menú.
     * Si este test se rompe, es que alguien volvió a meter {@code Module} en la cadena de
     * seguridad.
     */
    @Test
    void laAutoridadNoCambiaAunqueLaOperacionCuelgueDeModulosConCualquierNombre() {
        Operation operacion = Operation.builder()
                .resource("USUARIOS")
                .tag("CREAR")
                .modules(Set.of(
                        Module.builder().idModule(1L).name("Personal").state(true).build(),
                        Module.builder().idModule(2L).name("Administración").state(true).build()))
                .build();

        assertThat(AuthorityName.of(operacion)).isEqualTo("USUARIOS:CREAR");
    }

    @Test
    void lanzaIllegalArgumentExceptionSiLaOperacionEsNula() {
        assertThatThrownBy(() -> AuthorityName.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lanzaIllegalArgumentExceptionSiElRecursoNormalizaAVacio() {
        Operation operacion = Operation.builder().resource("   ").tag("CREAR").build();

        assertThatThrownBy(() -> AuthorityName.of(operacion))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lanzaIllegalArgumentExceptionSiElTagDeLaOperacionNormalizaAVacio() {
        Operation operacion = Operation.builder().resource("USUARIOS").tag("").build();

        assertThatThrownBy(() -> AuthorityName.of(operacion))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
