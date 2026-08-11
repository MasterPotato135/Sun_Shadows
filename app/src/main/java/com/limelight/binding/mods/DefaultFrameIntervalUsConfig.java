package com.limelight.mods;

public final class DefaultFrameIntervalUsConfig {
    /**
     * Intervalo padrão entre frames em microssegundos, assumindo 60 Hz.
     * Usado como valor inicial de frameIntervalUs antes de setup() calcular
     * o valor real a partir do refreshRate do display.
     */
    public static final long DEFAULT_FRAME_INTERVAL_US = 16667L;
}
