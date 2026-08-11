package com.limelight.mods;

public final class SignalRsrpMinValidConfig {
    /**
     * Valor mínimo de RSRP considerado válido para LTE (dBm).
     * Leituras acima de -30 dBm são improváveis em campo real e indicam
     * ausência de sinal LTE ou API não disponível; usa-se fallback getLevel().
     */
    public static final int SIGNAL_RSRP_MIN_VALID = -30;
}
