package com.limelight.mods;

import android.os.Handler;
import android.os.HandlerThread;

import com.limelight.LimeLog;
import com.limelight.nvstream.jni.MoonBridge;

/**
 * Gerencia o ajuste dinâmico de bitrate com base na similaridade de frames consecutivos.
 *
 * Encapsula:
 *   – A HandlerThread dedicada (antes {@code bitrateHandlerThread} no renderer).
 *   – O Handler para despachar {@code MoonBridge.requestBitrateChange()} fora da thread JNI.
 *   – A máquina de estados de redução/ramp-up de bitrate (antes inline em
 *     {@code analyzeFrameForLocalOptimizations()}).
 *
 * Uso:
 *   optimizer.start()                   → inicia a HandlerThread
 *   optimizer.getHandler()              → Handler para o Looper interno (usado por SignalMonitor)
 *   optimizer.onFrame(similarity, prefs)→ atualiza estado e despacha mudanças se necessário
 *   optimizer.reset(targetBitrate)      → restaura bitrate original (ex: após codec recovery)
 *   optimizer.stop()                    → para a HandlerThread com segurança
 *
 * Constantes lidas dos /mods/ para permitir ajuste sem recompilar o renderer.
 */
public class BitrateOptimizer {

    // ── Constantes centralizadas nos /mods/ ──────────────────────────────────
    private static final int BITRATE_REDUCE_THRESHOLD    = BitrateReduceThresholdConfig.BITRATE_REDUCE_THRESHOLD;
    private static final int BITRATE_RESTORE_THRESHOLD   = BitrateRestoreThresholdConfig.BITRATE_RESTORE_THRESHOLD;
    private static final int BITRATE_REDUCE_PERCENT      = BitrateReducePercentConfig.BITRATE_REDUCE_PERCENT;
    private static final int BITRATE_RAMPUP_STEP_PERCENT = BitrateRampupStepPercentConfig.BITRATE_RAMPUP_STEP_PERCENT;
    private static final int BITRATE_REDUCE_FLOOR_DIVISOR = BitrateReduceFloorDivisorConfig.BITRATE_REDUCE_FLOOR_DIVISOR;

    // ── Thread e Handler ─────────────────────────────────────────────────────
    private HandlerThread handlerThread;
    private volatile Handler handler;

    // ── Estado do bitrate dinâmico ───────────────────────────────────────────
    private int consecutiveSimilarFrames    = 0;
    private int consecutiveDissimilarFrames = 0;
    private volatile int currentDynamicBitrate = 0; // 0 = não inicializado
    private volatile boolean bitrateReduced    = false;

    public BitrateOptimizer() {
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    /**
     * Inicia a HandlerThread dedicada para chamadas de {@code requestBitrateChange()}.
     * Deve ser chamado em {@code setup()} do renderer, antes de qualquer chamada a
     * {@code onFrame()} ou {@code getHandler()}.
     */
    public void start() {
        handlerThread = new HandlerThread("Video - Bitrate");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    /**
     * Para a HandlerThread com segurança e bloqueia até que ela encerre.
     * Deve ser chamado em {@code cleanup()} do renderer, após parar o SignalMonitor.
     */
    public void stop() {
        if (handlerThread != null) {
            handlerThread.quitSafely();
            try {
                handlerThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            handlerThread = null;
            handler = null;
        }
    }

    /**
     * Retorna o Handler do Looper interno.
     * Usado pelo {@link SignalMonitor} para registrar o PhoneStateListener
     * no Looper correto (requerido pelo sistema Android).
     *
     * @throws IllegalStateException se chamado antes de {@link #start()}.
     */
    public Handler getHandler() {
        if (handler == null) {
            throw new IllegalStateException("BitrateOptimizer not started — call start() first");
        }
        return handler;
    }

    // ── Lógica de bitrate dinâmico ───────────────────────────────────────────

    /**
     * Processa a similaridade do frame atual e, se necessário, despacha uma mudança
     * de bitrate para o encoder remoto via {@code MoonBridge.requestBitrateChange()}.
     *
     * Deve ser chamado dentro de {@code analyzeFrameForLocalOptimizations()} no renderer,
     * apenas quando {@code prefs.bitrateOptimization} estiver ativo e o intervalo de
     * análise tiver decorrido.
     *
     * @param similarity     similaridade 0–100 retornada por {@link FrameSimilarityAnalyzer}.
     * @param targetBitrate  {@code prefs.bitrate} — bitrate alvo original configurado pelo usuário.
     * @param threshold      {@code prefs.frameSimilarityThreshold} — limiar de similaridade.
     * @param isIdr          verdadeiro se o frame atual for IDR (não reduz em IDRs).
     * @param optimizationEnabled se falso, só atualiza os contadores mas não despacha mudanças.
     */
    public void onFrame(int similarity, int targetBitrate, int threshold,
                        boolean isIdr, boolean optimizationEnabled) {
        if (targetBitrate <= 0) {
            LimeLog.warning("BitrateOptimizer: prefs.bitrate inválido (" + targetBitrate + "), ignorando frame");
            return;
        }

        boolean similar = (similarity >= threshold) && !isIdr;

        if (similar) {
            consecutiveSimilarFrames++;
            consecutiveDissimilarFrames = 0;
        } else {
            consecutiveDissimilarFrames++;
            consecutiveSimilarFrames = 0;
        }

        if (!optimizationEnabled) return;

        // Inicializa currentDynamicBitrate na primeira chamada válida
        if (currentDynamicBitrate <= 0) {
            currentDynamicBitrate = targetBitrate;
        }

        if (!bitrateReduced && consecutiveSimilarFrames >= BITRATE_REDUCE_THRESHOLD) {
            // Cena estável: reduz sobre o bitrate ALVO original (não o currentDynamic)
            // para evitar redução composta (ex: 10000 → 7000 → 4900 → ...).
            int reduced = targetBitrate * (100 - BITRATE_REDUCE_PERCENT) / 100;
            reduced = Math.max(reduced, targetBitrate / BITRATE_REDUCE_FLOOR_DIVISOR); // floor em 25%
            dispatchBitrateChange(reduced);
            currentDynamicBitrate = reduced;
            bitrateReduced = true;
            consecutiveSimilarFrames = 0; // evita nova redução imediata
            LimeLog.info("BitrateOptimizer: bitrate reduzido para " + reduced + " kbps (cena estável)");

        } else if (bitrateReduced && consecutiveDissimilarFrames >= BITRATE_RESTORE_THRESHOLD) {
            // Cena mudou: ramp-up gradual em direção ao alvo.
            // Não zera consecutiveDissimilarFrames entre degraus — acumulação contínua
            // garante que cada múltiplo de BITRATE_RESTORE_THRESHOLD sobe um degrau.
            // Zerar travaria o ramp-up em frames dinâmicos (starvation do encoder).
            int step = Math.max(1, targetBitrate * BITRATE_RAMPUP_STEP_PERCENT / 100);
            int next = Math.min(targetBitrate, currentDynamicBitrate + step);
            dispatchBitrateChange(next);
            currentDynamicBitrate = next;

            if (currentDynamicBitrate >= targetBitrate) {
                bitrateReduced = false;
                consecutiveDissimilarFrames = 0;
                consecutiveSimilarFrames    = 0;
                LimeLog.info("BitrateOptimizer: bitrate restaurado para " + targetBitrate + " kbps");
            } else {
                LimeLog.info("BitrateOptimizer: ramp-up " + currentDynamicBitrate + "/" + targetBitrate + " kbps");
            }
        }
    }

    /**
     * Reseta o estado do otimizador e restaura o bitrate original.
     * Deve ser chamado em {@code doCodecRecoveryIfRequired()} antes do reinício do decoder.
     *
     * @param targetBitrate bitrate alvo a ser restaurado (tipicamente {@code prefs.bitrate}).
     */
    public void reset(int targetBitrate) {
        if (bitrateReduced && targetBitrate > 0) {
            dispatchBitrateChange(targetBitrate);
        }
        consecutiveSimilarFrames    = 0;
        consecutiveDissimilarFrames = 0;
        currentDynamicBitrate       = 0;
        bitrateReduced              = false;
    }

    /** Retorna {@code true} se o bitrate está atualmente reduzido. */
    public boolean isBitrateReduced() {
        return bitrateReduced;
    }

    // ── Despacho interno ─────────────────────────────────────────────────────

    /**
     * Despacha {@code MoonBridge.requestBitrateChange(newBitrate)} no Handler interno.
     * Nunca deve ser chamado diretamente da thread de callback JNI (submitDecodeUnit)
     * — o despacho assíncrono garante a ausência de deadlock/reentrância no
     * moonlight-common-c.
     */
    private void dispatchBitrateChange(final int newBitrate) {
        if (newBitrate <= 0) {
            LimeLog.warning("BitrateOptimizer: valor inválido ignorado: " + newBitrate);
            return;
        }
        final Handler h = handler;
        if (h == null) {
            LimeLog.warning("BitrateOptimizer: handler não está pronto, mudança ignorada");
            return;
        }
        h.post(new Runnable() {
            @Override
            public void run() {
                try {
                    MoonBridge.requestBitrateChange(newBitrate);
                    LimeLog.info("BitrateOptimizer: bitrate aplicado → " + newBitrate + " kbps");
                } catch (UnsatisfiedLinkError e) {
                    // Biblioteca nativa não implementa requestBitrateChange; ignorar.
                    LimeLog.warning("BitrateOptimizer: requestBitrateChange indisponível: " + e.getMessage());
                }
            }
        });
    }
}
