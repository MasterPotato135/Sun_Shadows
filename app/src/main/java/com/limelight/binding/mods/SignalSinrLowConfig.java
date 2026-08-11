package com.limelight.mods;

public final class SignalSinrLowConfig {
    /**
     * Limiar SINR (unidade: décimos de dB, ex: 30 = 3.0 dB) abaixo do qual
     * o canal é considerado muito ruidoso. Quando SINR < este valor,
     * o jumpFrameMode sobe um nível adicional além do mapeado pelo RSRP.
     */
    public static final int SIGNAL_SINR_LOW = 30;
}
