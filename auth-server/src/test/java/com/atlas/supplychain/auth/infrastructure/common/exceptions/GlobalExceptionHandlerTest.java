package com.atlas.supplychain.auth.infrastructure.common.exceptions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import tools.jackson.databind.ObjectMapper;
import com.atlas.supplychain.auth.domain.common.exceptions.ServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final ErrorResponseWriter errorResponseWriter = new ErrorResponseWriter(new ObjectMapper());
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(errorResponseWriter);

    private ListAppender<ILoggingEvent> logAppender;
    private Logger writerLogger;

    @BeforeEach
    void capturarLogs() {
        writerLogger = (Logger) LoggerFactory.getLogger(ErrorResponseWriter.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        writerLogger.addAppender(logAppender);
    }

    @AfterEach
    void limpiarLogs() {
        writerLogger.detachAppender(logAppender);
    }

    @Test
    void fallbackDevuelve500ConMensajeGenericoSinCodeYConCorrelationIdPresente() {
        var response = handler.handleGeneric(new RuntimeException("detalle interno sensible"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("Error interno del servidor.");
        assertThat(body.message()).doesNotContain("detalle interno sensible");
        assertThat(body.code()).isNull();
        assertThat(body.correlationId()).isNotBlank();
    }

    @Test
    void fallbackLogueaElMismoCorrelationIdConStacktraceEnNivelError() {
        var response = handler.handleGeneric(new RuntimeException("detalle interno sensible"));
        String correlationId = response.getBody().correlationId();

        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent event = logAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage()).contains(correlationId);
        assertThat(event.getThrowableProxy().getMessage()).isEqualTo("detalle interno sensible");
    }

    /**
     * domain.common.exceptions.ServiceException (no org.hibernate.service.spi.ServiceException)
     * mapeada a 400 con mensaje genérico -- el import de Hibernate se eliminó del handler.
     */
    @Test
    void serviceExceptionDeDominioDevuelve400ConMensajeGenericoSinDetalleInterno() {
        var response = handler.handleServiceException(new ServiceException("No se pudo procesar el token", null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.message()).isEqualTo("No se pudo completar la solicitud.");
        assertThat(body.code()).isNull();
        assertThat(body.correlationId()).isNotBlank();
    }
}
