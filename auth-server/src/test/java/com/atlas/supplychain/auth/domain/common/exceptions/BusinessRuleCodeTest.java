package com.atlas.supplychain.auth.domain.common.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessRuleCodeTest {

    @Test
    void catalogoTieneExactamenteSeisValores() {
        assertThat(BusinessRuleCode.values()).hasSize(6);
    }

    @Test
    void tokenResetExpiradoColapsaAlCodigoPublicoDeTokenResetInvalido() {
        assertThat(BusinessRuleCode.TOKEN_RESET_EXPIRADO.getPublicCode())
                .isEqualTo(BusinessRuleCode.TOKEN_RESET_INVALIDO.name());
        assertThat(BusinessRuleCode.TOKEN_RESET_EXPIRADO.getPublicMessage())
                .isEqualTo(BusinessRuleCode.TOKEN_RESET_INVALIDO.getPublicMessage());
    }

    @Test
    void cadaValorTienePublicCodeIgualASuNombreSalvoTokenResetExpirado() {
        for (BusinessRuleCode code : BusinessRuleCode.values()) {
            if (code == BusinessRuleCode.TOKEN_RESET_EXPIRADO) continue;
            assertThat(code.getPublicCode()).isEqualTo(code.name());
        }
    }

    @Test
    void httpStatusDeRefreshTokenInvalidoEs401() {
        assertThat(BusinessRuleCode.REFRESH_TOKEN_INVALIDO.getHttpStatus()).isEqualTo(401);
    }
}
