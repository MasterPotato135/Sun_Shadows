package com.limelight.mods;

public final class CellularBitrateCeilingConfig {
    /**
     * Teto de bitrate (kbps) para links celulares (4G/5G).
     * Alinhado com MAX_BITRATE_FOR_4G para evitar throttling desnecessário
     * em links 4G/5G de boa qualidade.
     */
    public static final int CELLULAR_BITRATE_CEILING_KBPS = 8000;
}
