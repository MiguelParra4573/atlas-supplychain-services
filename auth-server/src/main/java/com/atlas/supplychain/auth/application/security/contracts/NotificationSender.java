package com.atlas.supplychain.auth.application.security.contracts;

public interface NotificationSender {
    void enviarResetPassword(String destinatario, String nombreUsuario, String token, long minutosVigencia);
}
