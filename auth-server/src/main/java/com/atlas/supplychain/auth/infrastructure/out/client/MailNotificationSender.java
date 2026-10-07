package com.atlas.supplychain.auth.infrastructure.out.client;

import com.atlas.supplychain.auth.application.security.contracts.NotificationSender;
import com.atlas.supplychain.auth.infrastructure.common.exceptions.NotificationNotSentException;
import com.atlas.supplychain.auth.infrastructure.config.mail.MailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Envía el correo de reset de password por SMTP. En local apunta a Mailpit (docker-compose), que
 * captura los correos sin entregarlos.
 */
@Component
@RequiredArgsConstructor
public class MailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final MailProperties properties;

    @Override
    public void enviarResetPassword(String destinatario, String nombreUsuario, String token, long minutosVigencia) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(destinatario);
        message.setSubject("Restablecer contraseña - Atlas Supply Chain");
        message.setText("""
                Hola %s,

                Recibimos una solicitud para restablecer tu contraseña. Usa este enlace:

                %s?token=%s

                El enlace vence en %d minutos. Si no lo solicitaste, ignora este correo.
                """.formatted(nombreUsuario, properties.getResetPasswordUrl(), token, minutosVigencia));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            throw new NotificationNotSentException("No se pudo enviar el correo de reset de password", e);
        }
    }
}
