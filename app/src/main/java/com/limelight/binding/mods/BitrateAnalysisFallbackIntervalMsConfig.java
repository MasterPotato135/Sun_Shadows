package com.limelight.mods;

public final class BitrateAnalysisFallbackIntervalMsConfig {
    /**
     * Intervalo mínimo de análise de bitrate em ms usado como fallback quando
     * prefs.bitrateAnalysisIntervalMs <= 0. Evita análise em todo frame (~60x/s)
     * que causaria OOM por acúmulo de posts no bitrateHandler.
     */
    public static final long BITRATE_ANALYSIS_FALLBACK_INTERVAL_MS = 50L;
}
