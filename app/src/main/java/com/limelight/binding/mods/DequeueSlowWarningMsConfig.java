package com.limelight.mods;

public final class DequeueSlowWarningMsConfig {
    /**
     * Tempo em ms acima do qual um dequeueInputBuffer é considerado lento
     * e gera um aviso no log. Indica pressão no pipeline de decodificação.
     */
    public static final int DEQUEUE_SLOW_WARNING_MS = 20;
}
