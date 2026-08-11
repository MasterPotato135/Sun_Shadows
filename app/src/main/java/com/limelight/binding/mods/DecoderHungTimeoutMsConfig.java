package com.limelight.mods;

public final class DecoderHungTimeoutMsConfig {
    /**
     * Tempo em ms sem resposta do decoder após o qual ele é considerado travado
     * e um DecoderHungException é lançado para forçar a recuperação.
     */
    public static final int DECODER_HUNG_TIMEOUT_MS = 5000;
}
