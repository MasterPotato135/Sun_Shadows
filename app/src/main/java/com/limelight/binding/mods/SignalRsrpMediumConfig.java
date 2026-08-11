package com.limelight.mods;

public final class SignalRsrpMediumConfig {
    /**
     * Limiar RSRP (dBm) acima do qual o sinal LTE é considerado médio.
     * -105 <= RSRP < -95 dBm → jumpFrameMode pelo menos LIGHT.
     */
    public static final int SIGNAL_RSRP_MEDIUM = -105;
}
