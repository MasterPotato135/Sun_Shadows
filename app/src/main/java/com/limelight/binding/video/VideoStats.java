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