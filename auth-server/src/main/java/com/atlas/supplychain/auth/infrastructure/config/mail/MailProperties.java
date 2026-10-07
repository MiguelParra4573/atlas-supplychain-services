package com.atlas.supplychain.auth.infrastructure.config.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "atlas.mail")
@Getter
@Setter
public class MailProperties {

    /** Remitente de los correos transaccionales. */
    private String from = "no-reply@atlas-supplychain.local";

    /** URL del frontend donde el usuario completa el reset; se le agrega {@code ?token=...}. */
    private String resetPasswordUrl = "http://localhost:5173/reset-password";
}
