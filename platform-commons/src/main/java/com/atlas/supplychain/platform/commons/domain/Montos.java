package com.atlas.supplychain.platform.commons.domain;

import java.math.RoundingMode;

/**
 * Constantes de montos (ADR-012): moneda única para todo el sistema, sin conversión; escala y
 * redondeo fijos para precio, costo promedio y cualquier valor monetario derivado.
 */
public final class Montos {

    public static final int ESCALA = 2;

    public static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    /**
     * Variable de entorno con el código ISO 4217 de la moneda única del sistema (ej. {@code PEN}).
     */
    public static final String VARIABLE_CODIGO_MONEDA = "APP_CURRENCY_CODE";

    private Montos() {}
}
