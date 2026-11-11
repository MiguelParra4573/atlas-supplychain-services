package com.atlas.supplychain.platform.commons.domain;

/**
 * Constantes de zona horaria (ADR-012): todo instante se guarda y se publica en UTC ({@code
 * Instant}); esta variable fija la única zona de presentación del sistema, configurable por
 * entorno, no por usuario ni por empresa.
 */
public final class ZonasHorarias {

    public static final String VARIABLE_ZONA_PRESENTACION = "APP_DISPLAY_TIMEZONE";

    private ZonasHorarias() {}
}
