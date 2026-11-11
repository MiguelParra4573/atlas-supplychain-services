package com.atlas.supplychain.platform.commons.domain;

import java.math.RoundingMode;

/**
 * Constantes de cantidades (ADR-012): escala y redondeo fijos para toda cantidad que se persiste o
 * se publica, en la unidad base del producto. La conversión de unidad de compra/venta a unidad base
 * ocurre una sola vez, en el borde de entrada (G-SCH-004).
 */
public final class Cantidades {

    /**
     * Tres decimales: cubre unidades discretas y fraccionarias (kg, litros) sin perder precisión.
     */
    public static final int ESCALA = 3;

    public static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private Cantidades() {}
}
