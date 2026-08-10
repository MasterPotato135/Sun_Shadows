//main/java/com/limelight/binding/video/VideoStats.java
package com.limelight.binding.video;

import android.os.SystemClock;

class VideoStats {

    long decoderTimeMs;
    long totalTimeMs;
    int totalFrames;
    int totalFramesReceived;
    int totalFramesRendered;
    int frameLossEvents;
    int framesLost;
    int framesDroppedByJumpFrame;
    int framesDroppedByLocalDeduplication;
    int framesDroppedForAudioContinuity;
    int framesSmoothedLocally;
    int framesAnalyzedForBitrate;
    int similarFramesDetected;
    char minHostProcessingLatency;
    char maxHostProcessingLatency;
    int totalHostProcessingLatency;
    int framesWithHostProcessingLatency;
    long measurementStartTimestamp;
    
    // Block compression + Adaptive processing stats
    int blocksProcessed;
    int blocksCopiedDirect;

    long blockAnalysisTimeMs;

    // Deduplicação de áreas
    int areaPatternsDetected;
    int framesReplacedByAreaDeduplication;
    long areaDeduplicationAnalysisTimeMs;

    // Latest-Frame-Wins: frames obsoletos descartados da outputBufferQueue
    int framesDroppedAsStaleOutput;

    // Pending-frames monitor: frames descartados na entrada por fila do decoder cheia
    int framesDroppedByPendingMonitor;

    void add(VideoStats other) {
        this.decoderTimeMs += other.decoderTimeMs;
        this.totalTimeMs += other.totalTimeMs;
        this.totalFrames += other.totalFrames;
        this.totalFramesReceived += other.totalFramesReceived;
        this.totalFramesRendered += other.totalFramesRendered;
        this.frameLossEvents += other.frameLossEvents;
        this.framesLost += other.framesLost;
        this.framesDroppedByJumpFrame += other.framesDroppedByJumpFrame;
        this.framesDroppedByLocalDeduplication += other.framesDroppedByLocalDeduplication;
        this.framesDroppedForAudioContinuity += other.framesDroppedForAudioContinuity;
        this.framesSmoothedLocally += other.framesSmoothedLocally;
        this.framesAnalyzedForBitrate += other.framesAnalyzedForBitrate;
        this.similarFramesDetected += other.similarFramesDetected;

        if (this.minHostProcessingLatency == 0) {
            this.minHostProcessingLatency = other.minHostProcessingLatency;
        } else {
            this.minHostProcessingLatency = (char) Math.min(this.minHostProcessingLatency, other.minHostProcessingLatency);
        }
        this.maxHostProcessingLatency = (char) Math.max(this.maxHostProcessingLatency, other.maxHostProcessingLatency);
        this.totalHostProcessingLatency += other.totalHostProcessingLatency;
        this.framesWithHostProcessingLatency += other.framesWithHostProcessingLatency;
        
        // Block compression stats
        this.blocksProcessed += other.blocksProcessed;
        this.blocksCopiedDirect += other.blocksCopiedDirect;
        this.blockAnalysisTimeMs += other.blockAnalysisTimeMs;

        // Deduplicação de áreas
        this.areaPatternsDetected += other.areaPatternsDetected;
        this.framesReplacedByAreaDeduplication += other.framesReplacedByAreaDeduplication;
        this.areaDeduplicationAnalysisTimeMs += other.areaDeduplicationAnalysisTimeMs;

        // Latest-Frame-Wins + Pending monitor
        this.framesDroppedAsStaleOutput += other.framesDroppedAsStaleOutput;
        this.framesDroppedByPendingMonitor += other.framesDroppedByPendingMonitor;

        if (this.measurementStartTimestamp == 0) {
            this.measurementStartTimestamp = other.measurementStartTimestamp;
        }

        assert other.measurementStartTimestamp >= this.measurementStartTimestamp;
    }

    void copy(VideoStats other) {
        this.decoderTimeMs = other.decoderTimeMs;
        this.totalTimeMs = other.totalTimeMs;
        this.totalFrames = other.totalFrames;
        this.totalFramesReceived = other.totalFramesReceived;
        this.totalFramesRendered = other.totalFramesRendered;
        this.frameLossEvents = other.frameLossEvents;
        this.framesLost = other.framesLost;
        this.framesDroppedByJumpFrame = other.framesDroppedByJumpFrame;
        this.framesDroppedByLocalDeduplication = other.framesDroppedByLocalDeduplication;
        this.framesDroppedForAudioContinuity = other.framesDroppedForAudioContinuity;
        this.framesSmoothedLocally = other.framesSmoothedLocally;
        this.framesAnalyzedForBitrate = other.framesAnalyzedForBitrate;
        this.similarFramesDetected = other.similarFramesDetected;
        this.minHostProcessingLatency = other.minHostProcessingLatency;
        this.maxHostProcessingLatency = other.maxHostProcessingLatency;
        this.totalHostProcessingLatency = other.totalHostProcessingLatency;
        this.framesWithHostProcessingLatency = other.framesWithHostProcessingLatency;
        this.measurementStartTimestamp = other.measurementStartTimestamp;
        
        // Block compression stats
        this.blocksProcessed = other.blocksProcessed;
        this.blocksCopiedDirect = other.blocksCopiedDirect;
        this.blockAnalysisTimeMs = other.blockAnalysisTimeMs;

        // Deduplicação de áreas
        this.areaPatternsDetected = other.areaPatternsDetected;
        this.framesReplacedByAreaDeduplication = other.framesReplacedByAreaDeduplication;
        this.areaDeduplicationAnalysisTimeMs = other.areaDeduplicationAnalysisTimeMs;

        // Latest-Frame-Wins + Pending monitor
        this.framesDroppedAsStaleOutput = other.framesDroppedAsStaleOutput;
        this.framesDroppedByPendingMonitor = other.framesDroppedByPendingMonitor;
    }

    void clear() {
        this.decoderTimeMs = 0;
        this.totalTimeMs = 0;
        this.totalFrames = 0;
        this.totalFramesReceived = 0;
        this.totalFramesRendered = 0;
        this.frameLossEvents = 0;
        this.framesLost = 0;
        this.framesDroppedByJumpFrame = 0;
        this.framesDroppedByLocalDeduplication = 0;
        this.framesDroppedForAudioContinuity = 0;
        this.framesSmoothedLocally = 0;
        this.framesAnalyzedForBitrate = 0;
        this.similarFramesDetected = 0;
        this.minHostProcessingLatency = 0;
        this.maxHostProcessingLatency = 0;
        this.totalHostProcessingLatency = 0;
        this.framesWithHostProcessingLatency = 0;
        this.measurementStartTimestamp = 0;
        
        // Block compression stats
        this.blocksProcessed = 0;
        this.blocksCopiedDirect = 0;
        this.blockAnalysisTimeMs = 0;

        // Deduplicação de áreas
        this.areaPatternsDetected = 0;
        this.framesReplacedByAreaDeduplication = 0;
        this.areaDeduplicationAnalysisTimeMs = 0;

        // Latest-Frame-Wins + Pending monitor
        this.framesDroppedAsStaleOutput = 0;
        this.framesDroppedByPendingMonitor = 0;
    }

    VideoStatsFps getFps() {
        float elapsed = (SystemClock.uptimeMillis() - this.measurementStartTimestamp) / (float) 1000;

        VideoStatsFps fps = new VideoStatsFps();
        if (elapsed > 0) {
            fps.totalFps = this.totalFrames / elapsed;
            fps.receivedFps = this.totalFramesReceived / elapsed;
            fps.renderedFps = this.totalFramesRendered / elapsed;
        }
        return fps;
    }
}

class VideoStatsFps {

    float totalFps;
    float receivedFps;
    float renderedFps;
}

/**
 * // main/java/com/limelight/binding/video/VideoStats.java
 * Analisador de compressão por blocos.
 * Detecta blocos uniformes (céu, paredes) e copia direto sem processamento.
 */
class BlockCompressionAnalyzer {
    private int blockSize;
    private int frameWidth;
    private int frameHeight;
    private static final int UNIFORMITY_THRESHOLD = 15;
    
    BlockCompressionAnalyzer(int blockSize, int frameWidth, int frameHeight) {
        this.blockSize = blockSize;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
    }
    
    // frameData contém bytes do bitstream comprimido convertidos para int (0-255).
    // Compara cada elemento amostrado com o valor central do bloco; se a diferença
    // absoluta ultrapassar UNIFORMITY_THRESHOLD, o bloco é considerado não-uniforme.
    boolean isBlockUniform(int[] frameData, int blockX, int blockY) {
        int startX = blockX * blockSize;
        int startY = blockY * blockSize;
        int endX = Math.min(startX + blockSize, frameWidth);
        int endY = Math.min(startY + blockSize, frameHeight);

        if (endX <= startX || endY <= startY) return true;

        int centerIdx = (startY + (endY - startY) / 2) * frameWidth + (startX + (endX - startX) / 2);
        if (centerIdx >= frameData.length) return true;

        int refVal = frameData[centerIdx]; // valor 0-255 direto

        for (int y = startY; y < endY; y += Math.max(1, blockSize / 4)) {
            for (int x = startX; x < endX; x += Math.max(1, blockSize / 4)) {
                int idx = y * frameWidth + x;
                if (idx >= frameData.length) continue;
                if (Math.abs(frameData[idx] - refVal) > UNIFORMITY_THRESHOLD) {
                    return false;
                }
            }
        }
        return true;
    }
}
class ProcessingMask {
    private final int columns;
    private final int rows;
    private final boolean[] needsProcessing;

    ProcessingMask(int frameWidth, int frameHeight, int blockSize) {
        this.columns = Math.max(1, (frameWidth + blockSize - 1) / blockSize);
        this.rows = Math.max(1, (frameHeight + blockSize - 1) / blockSize);
        this.needsProcessing = new boolean[columns * rows];
    }

    int getColumns() {
        return columns;
    }

    int getRows() {
        return rows;
    }

    void setBlock(int blockX, int blockY, boolean needsProcessing) {
        if (blockX < 0 || blockY < 0 || blockX >= columns || blockY >= rows) {
            return;
        }
        this.needsProcessing[blockY * columns + blockX] = needsProcessing;
    }

    boolean blockNeedsProcessing(int blockX, int blockY) {
        if (blockX < 0 || blockY < 0 || blockX >= columns || blockY >= rows) {
            return true;
        }
        return needsProcessing[blockY * columns + blockX];
    }

    int countBlocksNeedingProcessing() {
        int count = 0;
        for (boolean b : needsProcessing) {
            if (b) count++;
        }
        return count;
    }

    int getTotalBlocks() {
        return needsProcessing.length;
    }

    /**
     * Representação textual da máscara no estilo ■ (processar) / □ (copiar direto),
     * útil para overlays de debug/perf.
     */
    String toDebugString() {
        StringBuilder sb = new StringBuilder(rows * (columns + 1));
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < columns; x++) {
                sb.append(blockNeedsProcessing(x, y) ? '\u25A0' : '\u25A1');
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}

/**
 * // main/java/com/limelight/binding/video/VideoStats.java
 * Filtro adaptativo de nitidez.
 * Usa a ProcessingMask para aplicar mais nitidez em blocos com detalhe
 * e pouca (ou nenhuma) nitidez em blocos lisos/uniformes, economizando processamento.
 */
class AdaptiveSharpnessFilter {
    private static final int MIN_SHARPNESS = 0;
    private static final int MAX_SHARPNESS = 100;
    private final int baseSharpness;

    AdaptiveSharpnessFilter(int baseSharpness) {
        this.baseSharpness = Math.max(MIN_SHARPNESS, Math.min(MAX_SHARPNESS, baseSharpness));
    }

    /**
     * Retorna a força de nitidez (0-100) a ser aplicada num bloco específico,
     * de acordo com a máscara de processamento.
     */
    int getSharpnessForBlock(ProcessingMask mask, int blockX, int blockY) {
        if (mask == null) {
            return baseSharpness;
        }
        if (!mask.blockNeedsProcessing(blockX, blockY)) {
            // Bloco uniforme (céu, parede, neblina...): nitidez mínima/nenhuma
            return MIN_SHARPNESS;
        }
        // Bloco com detalhe: aplica nitidez cheia (ou reforçada)
        return Math.min(MAX_SHARPNESS, baseSharpness + 20);
    }

    /**
     * Calcula a nitidez média efetivamente aplicada num frame, considerando
     * quantos blocos foram processados vs copiados direto. Útil para stats/perf overlay.
     */
    float getAverageAppliedSharpness(ProcessingMask mask) {
        if (mask == null || mask.getTotalBlocks() == 0) {
            return baseSharpness;
        }
        int processed = mask.countBlocksNeedingProcessing();
        float processedRatio = processed / (float) mask.getTotalBlocks();
        return processedRatio * Math.min(MAX_SHARPNESS, baseSharpness + 20);
    }
}

/**
 * // main/java/com/limelight/binding/video/VideoStats.java
 * Deduplicador de áreas — descarte pré-decoder baseado em amostras do bitstream.
 *
 * Como funciona:
 * A cada (x) frames [checkInterval], compara a amostra de 48 bytes do frame atual
 * com as amostras dos (y) frames anteriores [lookbackFrames] armazenados em histórico
 * circular. Se pelo menos metade das sub-regiões da grade permanecerem estáveis
 * (similaridade de bytes acima do limiar) durante toda a janela, sinaliza que os
 * próximos (z) frames [replaceFrames] devem ser descartados ANTES de entrar no decoder
 * (via queueInputBuffer com size=0 em submitDecodeUnit).
 *
 * IMPORTANTE — o que realmente acontece ao descartar um frame:
 *   - O frame NÃO é decodificado → economia de CPU do decoder.
 *   - A superfície de vídeo continua exibindo o último frame decodificado (freeze local).
 *   - NÃO há substituição por "imagem genérica" ou síntese de conteúdo.
 *   - Os bytes do frame já foram recebidos pela rede → sem economia de banda.
 *   - A amostra compara bytes do bitstream comprimido, não pixels — é uma heurística.
 *
 * Esta é uma configuração independente do menu de filtros: cada variável (x, y, z,
 * limiar de similaridade e tamanho da grade de áreas) só é exposta/habilitada quando a
 * Deduplicação de Áreas está ativa (ver PreferenceConfiguration.areaDeduplicationEnabled).
 */
/**
 * Deduplicador de áreas — descarte pré-decoder baseado em amostras do bitstream.
 *
 * COMO FUNCIONA:
 * A amostra de FRAME_SAMPLE_SIZE bytes é dividida em (gridSize) segmentos contíguos,
 * cada um representando uma "área" do bitstream. O histórico circular guarda as últimas
 * (y) amostras. A cada chamada, cada área é comparada byte a byte com suas versões
 * anteriores: se a similaridade de uma área for >= threshold em TODOS os frames do
 * histórico, essa área é considerada estável.
 *
 * DECISÃO DE DESCARTE (frame inteiro):
 * O frame só é descartado pré-decoder quando a PROPORÇÃO de áreas estáveis no frame
 * atual é >= stableAreaRatioThreshold (configurado pelo chamador, ex: 90%).
 * Isso garante que uma única área em movimento impede o descarte — evitando que o
 * usuário veja freeze em frames onde partes da cena mudaram.
 *
 * CONFIANÇA ACUMULADA:
 * Mesmo com proporção suficiente, é exigida uma janela de CONFIDENCE_MAX confirmações
 * consecutivas antes de descartar. Isso elimina falsos positivos de cenas com micro-
 * variações recorrentes (partículas, cursor piscando).
 *
 * RESULTADO:
 * - Cena toda parada por N frames → descarta frame inteiro pré-decoder (economiza CPU)
 * - Qualquer área em movimento → não descarta (sem artefatos visuais)
 * - O último frame decodificado permanece na surface durante o freeze local
 * - Bytes já foram recebidos pela rede → sem economia de banda, apenas CPU de decode
 */
/**
 * // main/java/com/limelight/binding/video/VideoStats.java
 * Controlador de frame pacing baseado em histórico real de chegada de frames.
 *
 * Em vez de aplicar uma curva matemática fixa ao timestamp, mantém um histórico
 * circular dos últimos N intervalos entre frames e usa a média + jitter medidos
 * para decidir QUANDO cada frame deve ser apresentado.
 *
 * MODOS DE BUFFER:
 *   LOW_LATENCY  (buffer=1): apresenta imediatamente, sem suavização
 *   BALANCED     (buffer=2): suaviza jitter leve, latência +1 frame
 *   SMOOTH       (buffer=3): suaviza jitter intenso, latência +2 frames
 *
 * DETECÇÃO DE GAPS:
 *   - gap pequeno  (< 2× intervalo esperado): suaviza gradualmente
 *   - gap grande   (≥ 2× e < 5× esperado):   recupera rápido
 *   - gap enorme   (≥ 5× esperado / drop run): reset do scheduler
 *
 * SAÍDA:
 *   getRenderTimeNanos() retorna o timestamp de apresentação ajustado para
 *   uso em releaseOutputBuffer(index, renderTimeNanos). Nunca retorna um valor
 *   no passado — garante mínimo de "agora + 1ms" para não ser dropped pelo SurfaceFlinger.
 */
class FramePacingController {

    // Modos de buffer de apresentação
    static final int MODE_LOW_LATENCY = 1;
    static final int MODE_BALANCED    = 2;
    static final int MODE_SMOOTH      = 3;

    // Tamanho do histórico circular de intervalos entre frames
    private static final int HISTORY_SIZE = 16;

    // Limites de gap para classificação
    private static final float GAP_SMALL_MULTIPLIER  = 2.0f;
    private static final float GAP_LARGE_MULTIPLIER  = 5.0f;

    // Quanto do jitter permitimos antes de considerar "atrasado"
    private static final float JITTER_TOLERANCE_NS   = 4_000_000f; // 4ms

    // Mínimo de tempo no futuro para não ser dropped pelo SurfaceFlinger
    private static final long  MIN_AHEAD_NS          = 1_000_000L; // 1ms

    private final long[] intervalHistory = new long[HISTORY_SIZE];
    private int historyHead  = 0;
    private int historyCount = 0;

    private long lastArrivalNs = 0;
    private long baseRenderNs  = 0; // âncora do scheduler

    private int  mode;
    private long targetIntervalNs; // estimativa corrente do intervalo ideal

    // Stats expostas para o perf overlay
    volatile long   measuredAvgIntervalNs;
    volatile long   measuredJitterNs;
    volatile int    lateFrames;
    volatile int    earlyFrames;
    volatile int    dropGaps;

    FramePacingController(int mode, int displayRefreshRate) {
        setMode(mode, displayRefreshRate);
    }

    void setMode(int mode, int displayRefreshRate) {
        this.mode = Math.max(MODE_LOW_LATENCY, Math.min(MODE_SMOOTH, mode));
        // Estimativa inicial: display refresh rate como fallback
        this.targetIntervalNs = (displayRefreshRate > 0)
                ? (1_000_000_000L / displayRefreshRate)
                : 16_666_667L; // 60 fps fallback
    }

    /**
     * Registra a chegada de um novo frame (em nanos) e calcula o timestamp
     * de apresentação ideal.
     *
     * @param arrivalNs  System.nanoTime() no momento de chegada do frame
     * @return           timestamp para releaseOutputBuffer (sempre ≥ agora + 1ms)
     */
    long onFrameArrived(long arrivalNs) {
        // --- 1. Mede o intervalo desde o último frame ---
        if (lastArrivalNs > 0) {
            long interval = arrivalNs - lastArrivalNs;
            recordInterval(interval);
            classifyFrame(interval);
        }
        lastArrivalNs = arrivalNs;

        // --- 2. Recalcula stats ---
        updateStats();

        // --- 3. Avança o scheduler ---
        long avg = (historyCount > 0) ? measuredAvgIntervalNs : targetIntervalNs;
        if (avg > 0) targetIntervalNs = avg;

        if (baseRenderNs == 0) {
            // Primeira âncora
            baseRenderNs = arrivalNs;
        } else {
            baseRenderNs += targetIntervalNs;
        }

        // --- 4. Detecta gap e reage ---
        long drift = baseRenderNs - arrivalNs;
        long absGap = Math.abs(drift);

        if (absGap >= GAP_LARGE_MULTIPLIER * targetIntervalNs) {
            // Gap enorme: reset — provavelmente houve stall de rede ou pausa longa
            baseRenderNs = arrivalNs;
            dropGaps++;
        } else if (absGap >= GAP_SMALL_MULTIPLIER * targetIntervalNs) {
            // Gap grande: recupera metade da distância imediatamente
            baseRenderNs = arrivalNs + (drift > 0 ? targetIntervalNs / 2 : 0);
            dropGaps++;
        }
        // Gap pequeno: deixa o scheduler avançar naturalmente

        // --- 5. Aplica offset de buffer por modo ---
        long bufferOffsetNs = (mode - 1) * targetIntervalNs; // 0, 1×, 2× frame
        long renderNs = baseRenderNs + bufferOffsetNs;

        // --- 6. Garante mínimo no futuro ---
        long nowNs = System.nanoTime();
        if (renderNs < nowNs + MIN_AHEAD_NS) {
            renderNs = nowNs + MIN_AHEAD_NS;
            // Ressincroniza âncora para não acumular atraso
            baseRenderNs = renderNs - bufferOffsetNs;
        }

        return renderNs;
    }

    /** Versão sem registro de chegada — para uso no Choreographer callback. */
    long getRenderTimeNanos(long choreographerFrameNs) {
        if (baseRenderNs == 0) return choreographerFrameNs;
        long bufferOffsetNs = (mode - 1) * targetIntervalNs;
        long renderNs = baseRenderNs + bufferOffsetNs;
        long nowNs = System.nanoTime();
        if (renderNs < nowNs + MIN_AHEAD_NS) {
            renderNs = nowNs + MIN_AHEAD_NS;
        }
        return renderNs;
    }

    private void recordInterval(long intervalNs) {
        // Filtra intervalos impossíveis (< 1ms ou > 500ms) para não poluir a média
        if (intervalNs < 1_000_000L || intervalNs > 500_000_000L) return;
        intervalHistory[historyHead] = intervalNs;
        historyHead = (historyHead + 1) % HISTORY_SIZE;
        historyCount = Math.min(historyCount + 1, HISTORY_SIZE);
    }

    private void classifyFrame(long intervalNs) {
        if (historyCount == 0 || targetIntervalNs <= 0) return;
        float ratio = intervalNs / (float) targetIntervalNs;
        if (ratio > 1f + (JITTER_TOLERANCE_NS / targetIntervalNs)) {
            lateFrames++;
        } else if (ratio < 1f - (JITTER_TOLERANCE_NS / targetIntervalNs)) {
            earlyFrames++;
        }
    }

    private void updateStats() {
        if (historyCount == 0) return;
        long sum = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (int i = 0; i < historyCount; i++) {
            long v = intervalHistory[i];
            sum += v;
            if (v < min) min = v;
            if (v > max) max = v;
        }
        measuredAvgIntervalNs = sum / historyCount;
        measuredJitterNs      = max - min;
    }

    /** Reset completo — chamar junto com o reset do decoder. */
    void reset() {
        historyHead  = 0;
        historyCount = 0;
        lastArrivalNs = 0;
        baseRenderNs  = 0;
        lateFrames    = 0;
        earlyFrames   = 0;
        dropGaps      = 0;
        measuredAvgIntervalNs = 0;
        measuredJitterNs      = 0;
    }

    int getMode()               { return mode; }
    long getTargetIntervalNs()  { return targetIntervalNs; }
}

class AreaDeduplicator {

    private final int gridSize;
    private byte[][] history;
    private int historyCount;
    private int historyHead;

    // Bitmap de estabilidade por área, reutilizado entre chamadas para evitar alocação
    private boolean[] stableAreaBitmap;

    // Nível de confiança acumulado: exige CONFIDENCE_MAX confirmações consecutivas
    // antes de descartar, para eliminar falsos positivos de micro-variações.
    private int confidenceLevel = 0;
    private static final int CONFIDENCE_MAX = 3;

    AreaDeduplicator(int gridSize) {
        this.gridSize = Math.max(1, gridSize);
        this.stableAreaBitmap = new boolean[this.gridSize];
    }

    private void ensureHistoryCapacity(int lookbackFrames) {
        int capacity = Math.max(1, lookbackFrames);
        if (history == null || history.length != capacity) {
            history = new byte[capacity][];
            historyCount = 0;
            historyHead = 0;
        }
    }

    /**
     * Avalia cada área da amostra contra o histórico e preenche stableAreaBitmap.
     * Área i é estável se sua similaridade com TODOS os frames anteriores >= threshold.
     *
     * @return número de áreas estáveis (0..gridSize)
     */
    private int evaluateStableAreas(byte[] currentSample, int similarityThreshold) {
        if (currentSample == null || currentSample.length == 0 || historyCount == 0) {
            for (int i = 0; i < gridSize; i++) stableAreaBitmap[i] = false;
            return 0;
        }

        int areaLength = Math.max(1, currentSample.length / gridSize);
        int stableCount = 0;

        for (int area = 0; area < gridSize; area++) {
            int start = area * areaLength;
            int end = (area == gridSize - 1)
                    ? currentSample.length
                    : Math.min(currentSample.length, start + areaLength);
            if (end <= start) {
                stableAreaBitmap[area] = false;
                continue;
            }

            boolean stable = true;
            for (int h = 0; h < historyCount && stable; h++) {
                byte[] past = history[h];
                if (past == null || past.length != currentSample.length) {
                    stable = false;
                    break;
                }
                int matching = 0;
                int total = end - start;
                for (int idx = start; idx < end; idx++) {
                    if (currentSample[idx] == past[idx]) matching++;
                }
                if ((matching * 100) / total < similarityThreshold) {
                    stable = false;
                }
            }

            stableAreaBitmap[area] = stable;
            if (stable) stableCount++;
        }

        return stableCount;
    }

    /**
     * Retorna o bitmap de estabilidade da última análise.
     * stableAreaBitmap[i] == true → área i foi estável em toda a janela de lookback.
     * Válido apenas após chamar analyzeAndGetReplacementFrameCount().
     */
    boolean[] getStableAreaBitmap() {
        return stableAreaBitmap;
    }

    int getGridSize() {
        return gridSize;
    }

    /**
     * Analisa o frame atual e decide se ele deve ser descartado pré-decoder.
     *
     * A decisão é CONSERVADORA: o frame só é descartado quando a proporção de
     * áreas estáveis é >= stableAreaRatioPercent E isso se confirmou em
     * CONFIDENCE_MAX análises consecutivas. Uma única área em movimento impede
     * o descarte se stableAreaRatioPercent for 100.
     *
     * @param currentSample           amostra do bitstream do frame atual (128 bytes)
     * @param lookbackFrames          janela de histórico (y frames anteriores)
     * @param replaceFrames           frames a descartar quando padrão confirmado
     * @param similarityThreshold     limiar de similaridade por byte (%) por área
     * @param stableAreaRatioPercent  % mínima de áreas estáveis para descartar (0-100)
     *                                Ex: 100 = todas estáveis; 90 = 90% estáveis
     * @return replaceFrames se deve descartar, 0 caso contrário
     */
    int analyzeAndGetReplacementFrameCount(byte[] currentSample, int lookbackFrames,
                                            int replaceFrames, int similarityThreshold,
                                            int stableAreaRatioPercent) {
        ensureHistoryCapacity(lookbackFrames);

        // Avalia ANTES de inserir no histórico (evita auto-comparação)
        int stableAreas = evaluateStableAreas(currentSample, similarityThreshold);

        // Insere no histórico circular após a comparação
        if (currentSample != null) {
            history[historyHead] = currentSample;
            historyHead = (historyHead + 1) % history.length;
            historyCount = Math.min(historyCount + 1, history.length);
        }

        // Exige janela cheia antes de qualquer descarte
        if (historyCount < history.length || gridSize <= 0) {
            confidenceLevel = 0;
            return 0;
        }

        // Proporção de áreas estáveis no frame atual
        int stablePercent = (stableAreas * 100) / gridSize;
        int threshold = Math.max(0, Math.min(100, stableAreaRatioPercent));

        if (stablePercent >= threshold) {
            confidenceLevel = Math.min(CONFIDENCE_MAX, confidenceLevel + 1);
        } else {
            // Reage rápido a mudanças: decrementa em 2
            confidenceLevel = Math.max(0, confidenceLevel - 2);
        }

        if (confidenceLevel >= CONFIDENCE_MAX) {
            return Math.max(0, replaceFrames);
        }
        return 0;
    }
}