package com.atlas.supplychain.auth.infrastructure.config.security;

import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleCode;
import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultPasswordPolicyTest {

    private final DefaultPasswordPolicy policy = new DefaultPasswordPolicy(new PasswordPolicyProperties());

    @Test
    void aceptaContrasenaValida() {
        assertThatCode(() -> policy.validateOrThrow("Abcdef12#$"))
                .doesNotThrowAnyException();
    }

    @Test
    void rechazaPorLongitudInsuficiente() {
        assertThatThrownBy(() -> policy.validateOrThrow("Ab1#defg"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La contraseña debe tener al menos 10 caracteres.")
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(BusinessRuleCode.PASSWORD_NO_CUMPLE_POLITICA);
    }

    @Test
    void rechazaPorFaltaDeMayuscula() {
        assertThatThrownBy(() -> policy.validateOrThrow("abcdefgh1#"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La contraseña debe incluir al menos una mayúscula.")
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(BusinessRuleCode.PASSWORD_NO_CUMPLE_POLITICA);
    }

    @Test
    void rechazaPorFaltaDeMinuscula() {
        assertThatThrownBy(() -> policy.validateOrThrow("ABCDEFGH1#"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La contraseña debe incluir al menos una minúscula.")
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(BusinessRuleCode.PASSWORD_NO_CUMPLE_POLITICA);
    }

    @Test
    void rechazaPorFaltaDeDigito() {
        assertThatThrownBy(() -> policy.validateOrThrow("Abcdefgh#$"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La contraseña debe incluir al menos un número.")
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(BusinessRuleCode.PASSWORD_NO_CUMPLE_POLITICA);
    }

    @Test
    void rechazaPorFaltaDeCaracterEspecial() {
        assertThatThrownBy(() -> policy.validateOrThrow("Abcdefgh12"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La contraseña debe incluir al menos un caracter especial.")
                .extracting(ex -> ((BusinessRuleException) ex).getCode())
                .isEqualTo(BusinessRuleCode.PASSWORD_NO_CUMPLE_POLITICA);
    }

    @Test
    void umbralMinLengthEsParametrizablePorPropiedad() {
        PasswordPolicyProperties props = new PasswordPolicyProperties();
        props.setMinLength(4);
        DefaultPasswordPolicy policyConUmbralMenor = new DefaultPasswordPolicy(props);

        assertThatCode(() -> policyConUmbralMenor.validateOrThrow("Ab1#"))
                .doesNotThrowAnyException();
    }
}
