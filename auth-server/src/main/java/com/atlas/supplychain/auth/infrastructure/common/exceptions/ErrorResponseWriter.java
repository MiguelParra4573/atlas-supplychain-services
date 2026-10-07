package com.atlas.supplychain.auth.infrastructure.common.exceptions;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Único punto que construye ErrorResponse, genera correlationId y loguea el detalle.
 * Lo usan tanto GlobalExceptionHandler como los manejadores de SecurityConfig (401/403
 * se emiten en la cadena de filtros, antes del DispatcherServlet, y no pasan por el
 * @RestControllerAdvice).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ErrorResponseWriter {

    public static final String LOCKED_MESSAGE =
            "Usuario bloqueado por múltiples intentos fallidos. Cambie de contraseña.";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private final ObjectMapper objectMapper;

    public ResponseEntity<ErrorResponse> respond(HttpStatus status, String publicMessage,
                                                  String code, Throwable ex) {
        return ResponseEntity.status(status).body(build(status, publicMessage, code, ex));
    }

    public void write(HttpServletResponse response, HttpStatus status, String publicMessage,
                       Throwable ex) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), build(status, publicMessage, null, ex));
    }

    private ErrorResponse build(HttpStatus status, String publicMessage, String code, Throwable ex) {
        String correlationId = resolveCorrelationId();
        logDetail(status, correlationId, ex);
        return new ErrorResponse(publicMessage, code, correlationId);
    }

    private String resolveCorrelationId() {
        String fromMdc = MDC.get(CORRELATION_ID_MDC_KEY);
        return (fromMdc != null && !fromMdc.isBlank()) ? fromMdc : UUID.randomUUID().toString();
    }

    private void logDetail(HttpStatus status, String correlationId, Throwable ex) {
        String detail = ex != null ? ex.getMessage() : null;
        if (status.is5xxServerError()) {
            log.error("correlationId={} status={} detail={}", correlationId, status.value(), detail, ex);
        } else {
            log.warn("correlationId={} status={} detail={}", correlationId, status.value(), detail);
        }
    }
}
