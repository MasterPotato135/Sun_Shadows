package com.limelight.mods;

public final class SignalRsrpGoodConfig {
    /**
     * Limiar RSRP (dBm) acima do qual o sinal LTE é considerado bom.
     * RSRP >= -95 dBm → restaura baseJumpFrameMode do usuário.
     */
    public static final int SIGNAL_RSRP_GOOD = -95;
}
