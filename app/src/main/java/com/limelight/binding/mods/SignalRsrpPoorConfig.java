package com.limelight.mods;

public final class SignalRsrpPoorConfig {
    /**
     * Limiar RSRP (dBm) acima do qual o sinal LTE é considerado ruim (mas não crítico).
     * -115 <= RSRP < -105 dBm → jumpFrameMode pelo menos MEDIUM.
     * RSRP < -115 dBm → HEAVY (crítico).
     */
    public static final int SIGNAL_RSRP_POOR = -115;
}
