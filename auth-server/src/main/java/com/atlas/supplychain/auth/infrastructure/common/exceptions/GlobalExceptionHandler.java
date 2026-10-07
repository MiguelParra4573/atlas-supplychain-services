package com.atlas.supplychain.auth.infrastructure.common.exceptions;

import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleCode;
import com.atlas.supplychain.auth.domain.common.exceptions.BusinessRuleException;
import com.atlas.supplychain.auth.domain.common.exceptions.DuplicateRecordException;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.domain.common.exceptions.ExistingDependenciesException;
import com.atlas.supplychain.auth.domain.common.exceptions.ServiceException;
import com.atlas.supplychain.auth.domain.common.exceptions.StoredProcedureException;
import com.atlas.supplychain.auth.infrastructure.config.security.jwt.InvalidServiceSecretException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorResponseWriter errorResponseWriter;

    // ========= AUTH / SECURITY =========

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(LockedException ex) {
        return errorResponseWriter.respond(HttpStatus.LOCKED, ErrorResponseWriter.LOCKED_MESSAGE, null, ex);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return errorResponseWriter.respond(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos", null, ex);
    }

    @ExceptionHandler(InvalidServiceSecretException.class)
    public ResponseEntity<ErrorResponse> handleInvalidServiceSecret(InvalidServiceSecretException ex) {
        return errorResponseWriter.respond(HttpStatus.UNAUTHORIZED, "Servicio o secreto inválido", null, ex);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return errorResponseWriter.respond(HttpStatus.UNAUTHORIZED, "No autorizado", null, ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return errorResponseWriter.respond(HttpStatus.FORBIDDEN,
                "Acceso denegado. No tienes permisos para realizar esta acción.", null, ex);
    }

    // ========= VALIDATION =========

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return errorResponseWriter.respond(HttpStatus.BAD_REQUEST,
                mensaje.isBlank() ? "Datos inválidos" : mensaje, null, ex);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String mensaje = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        return errorResponseWriter.respond(HttpStatus.BAD_REQUEST,
                mensaje.isBlank() ? "Datos inválidos" : mensaje, null, ex);
    }

    // ========= DOMAIN / BUSINESS =========

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        return errorResponseWriter.respond(HttpStatus.NOT_FOUND, "Recurso no encontrado.", null, ex);
    }

    @ExceptionHandler(DuplicateRecordException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateRecord(DuplicateRecordException ex) {
        return errorResponseWriter.respond(HttpStatus.CONFLICT, "El registro ya existe.", null, ex);
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(ServiceException ex) {
        return errorResponseWriter.respond(HttpStatus.BAD_REQUEST, "No se pudo completar la solicitud.", null, ex);
    }

    @ExceptionHandler(ExistingDependenciesException.class)
    public ResponseEntity<ErrorResponse> handleExistingDependencies(ExistingDependenciesException ex) {
        return errorResponseWriter.respond(HttpStatus.CONFLICT,
                "No se puede eliminar el registro debido a dependencias existentes.", null, ex);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        BusinessRuleCode code = ex.getCode();
        return errorResponseWriter.respond(
                HttpStatus.valueOf(code.getHttpStatus()), code.getPublicMessage(), code.getPublicCode(), ex);
    }

    @ExceptionHandler(StoredProcedureException.class)
    public ResponseEntity<ErrorResponse> handleStoredProcedure(StoredProcedureException ex) {
        return errorResponseWriter.respond(HttpStatus.CONFLICT, "Error en procedimiento almacenado.", null, ex);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return errorResponseWriter.respond(HttpStatus.CONFLICT,
                "No se puede completar la operación porque existen datos relacionados.", null, ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return errorResponseWriter.respond(HttpStatus.BAD_REQUEST, "Solicitud inválida.", null, ex);
    }

    // ========= ROUTING =========

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        return errorResponseWriter.respond(HttpStatus.NOT_FOUND, "Recurso no encontrado.", null, ex);
    }

    // ========= FALLBACK 500 =========

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return errorResponseWriter.respond(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor.", null, ex);
    }
}
