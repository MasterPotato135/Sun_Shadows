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
    
    // Block compression + Adaptive processing + HUD detection stats
    int blocksProcessed;
    int blocksCopiedDirect;
    int hudElementsDetected;
    int hudRegionsSkipped;
    long blockAnalysisTimeMs;
    long hudDetectionTimeMs;

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
        this.hudElementsDetected += other.hudElementsDetected;
        this.hudRegionsSkipped += other.hudRegionsSkipped;
        this.blockAnalysisTimeMs += other.blockAnalysisTimeMs;
        this.hudDetectionTimeMs += other.hudDetectionTimeMs;

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
        this.hudElementsDetected = other.hudElementsDetected;
        this.hudRegionsSkipped = other.hudRegionsSkipped;
        this.blockAnalysisTimeMs = other.blockAnalysisTimeMs;
        this.hudDetectionTimeMs = other.hudDetectionTimeMs;

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
        this.hudElementsDetected = 0;
        this.hudRegionsSkipped = 0;
        this.blockAnalysisTimeMs = 0;
        this.hudDetectionTimeMs = 0;

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
    
    boolean isBlockUniform(int[] frameData, int blockX, int blockY) {
        int startX = blockX * blockSize;
        int startY = blockY * blockSize;
        int endX = Math.min(startX + blockSize, frameWidth);
        int endY = Math.min(startY + blockSize, frameHeight);
        
        if (endX <= startX || endY <= startY) return true;
        
        int centerPixelIdx = (startY + blockSize / 2) * frameWidth + (startX + blockSize / 2);
        if (centerPixelIdx >= frameData.length) return true;
        
        int refPixel = frameData[centerPixelIdx];
        int refR = (refPixel >> 16) & 0xFF;
        int refG = (refPixel >> 8) & 0xFF;
        int refB = refPixel & 0xFF;
        
        for (int y = startY; y < endY; y += Math.max(1, blockSize / 4)) {
            for (int x = startX; x < endX; x += Math.max(1, blockSize / 4)) {
                int pixelIdx = y * frameWidth + x;
                if (pixelIdx >= frameData.length) continue;
                
                int pixel = frameData[pixelIdx];
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                
                if (Math.abs(r - refR) + Math.abs(g - refG) + Math.abs(b - refB) > UNIFORMITY_THRESHOLD) {
                    return false;
                }
            }
        }
        return true;
    }
}

/**
 * // main/java/com/limelight/binding/video/VideoStats.java
 * Detctor de HUD adaptativo.
 * Identifica elementos HUD (menus, placar) que repetem frame-to-frame.
 * Aplica menor resolução nessas áreas para economizar bandwidth.
 */
class HudDetector {
    private int frameWidth;
    private int frameHeight;
    private int[] previousFrameHash;
    private static final int REPEATFRAME_THRESHOLD = (int) (0.95f * 255);  // 95% similar
    
    HudDetector(int frameWidth, int frameHeight) {
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.previousFrameHash = null;
    }
    
    boolean isHudRegion(int[] frameData, int regionX, int regionY, int regionSize) {
        if (previousFrameHash == null) {
            previousFrameHash = new int[frameData.length];
            System.arraycopy(frameData, 0, previousFrameHash, 0, frameData.length);
            return false;
        }
        
        int startIdx = regionY * frameWidth + regionX;
        int identicalPixels = 0;
        int totalPixels = 0;
        
        for (int y = 0; y < regionSize && regionY + y < frameHeight; y++) {
            for (int x = 0; x < regionSize && regionX + x < frameWidth; x++) {
                int idx = startIdx + y * frameWidth + x;
                if (idx < frameData.length && frameData[idx] == previousFrameHash[idx]) {
                    identicalPixels++;
                }
                totalPixels++;
            }
        }
        
        // Copia frame atual para comparação próxima
        System.arraycopy(frameData, 0, previousFrameHash, 0, frameData.length);
        
        return totalPixels > 0 && (identicalPixels / (float) totalPixels) > 0.85f;
    }

    /**
     * Calcula, com base em quantas vezes uma região se repetiu recentemente,
     * o quanto sua resolução pode ser reduzida (0-100%).
     * Regiões que repetem muito (ex.: HUD, minimapa, contador de munição)
     * recebem mais redução; regiões pouco repetitivas recebem pouca ou nenhuma.
     */
    int computeResolutionReduction(int repeatStreak, int maxReductionPercent) {
        if (repeatStreak <= 0) {
            return 0;
        }
        // Cresce rapidamente nos primeiros repeats e satura em maxReductionPercent
        int reduction = Math.min(maxReductionPercent, repeatStreak * 10);
        return reduction;
    }
}

/**
 * // main/java/com/limelight/binding/video/VideoStats.java
 * Máscara de processamento por blocos.
 * Guarda, para cada bloco do frame, se ele precisa de processamento (■, há detalhes)
 * ou pode ser copiado direto (□, uniforme). Evita gastar CPU em céu, paredes, neblina, menus etc.
 */
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
 * Deduplicador de áreas.
 *
 * Como funciona:
 * A cada (x) frames [checkInterval], olha para os (y) frames anteriores [lookbackFrames]
 * armazenados em um histórico circular e tenta detectar se existe um padrão local
 * (uma área da amostra do frame que se repete de forma praticamente idêntica ao longo
 * dessa janela). Se um padrão for encontrado, os próximos (z) frames [replaceFrames]
 * passam a ser tratados como "genéricos": ao invés de serem processados/renderizados
 * normalmente, eles reaproveitam a última imagem conhecida (que já reflete o movimento
 * recente das áreas vizinhas), economizando processamento/decodificação enquanto a área
 * permanecer estável.
 *
 * Esta é uma configuração independente do menu de filtros: cada variável (x, y, z,
 * limiar de similaridade e tamanho da grade de áreas) só é exposta/habilitada quando a
 * Deduplicação de Áreas está ativa (ver PreferenceConfiguration.areaDeduplicationEnabled).
 */
class AreaDeduplicator {

    private final int gridSize;
    private byte[][] history;
    private int historyCount;
    private int historyHead;

    AreaDeduplicator(int gridSize) {
        this.gridSize = Math.max(1, gridSize);
    }

    /**
     * Garante que o histórico circular comporta (y) frames anteriores.
     * Chamado sempre que prefs.areaDedupLookbackFrames (y) muda.
     */
    private void ensureHistoryCapacity(int lookbackFrames) {
        int capacity = Math.max(1, lookbackFrames);
        if (history == null || history.length != capacity) {
            history = new byte[capacity][];
            historyCount = 0;
            historyHead = 0;
        }
    }

    /**
     * Divide a amostra do frame em (gridSize) áreas e compara cada área do frame atual
     * com a mesma área nos (y) frames anteriores guardados no histórico.
     *
     * @return quantidade de áreas (0-gridSize) que se mantiveram estáveis (mesmo padrão
     *         local) durante toda a janela de lookback.
     */
    private int countStableAreas(byte[] currentSample, int similarityThreshold) {
        if (currentSample == null || currentSample.length == 0 || historyCount == 0) {
            return 0;
        }

        int areaLength = Math.max(1, currentSample.length / gridSize);
        int stableAreas = 0;

        for (int area = 0; area < gridSize; area++) {
            int start = area * areaLength;
            int end = Math.min(currentSample.length, start + areaLength);
            if (end <= start) {
                continue;
            }

            boolean stableAcrossWindow = true;
            for (int i = 0; i < historyCount; i++) {
                byte[] past = history[i];
                if (past == null || past.length != currentSample.length) {
                    stableAcrossWindow = false;
                    break;
                }

                int matching = 0;
                int total = end - start;
                for (int idx = start; idx < end; idx++) {
                    if (currentSample[idx] == past[idx]) {
                        matching++;
                    }
                }

                int areaSimilarity = (matching * 100) / total;
                if (areaSimilarity < similarityThreshold) {
                    stableAcrossWindow = false;
                    break;
                }
            }

            if (stableAcrossWindow) {
                stableAreas++;
            }
        }

        return stableAreas;
    }

    /**
     * Executa a análise de padrão local (chamada a cada x frames) e, em caso de padrão
     * encontrado, retorna quantos frames devem ser substituídos pela imagem genérica.
     *
     * @param currentSample     amostra do frame atual
     * @param lookbackFrames    (y) quantos frames anteriores considerar
     * @param replaceFrames     (z) quantos frames substituir quando um padrão for achado
     * @param similarityThreshold limiar (%) de similaridade por área para considerá-la estável
     * @return (z) se um padrão local foi detectado, ou 0 caso contrário
     */
    int analyzeAndGetReplacementFrameCount(byte[] currentSample, int lookbackFrames,
                                            int replaceFrames, int similarityThreshold) {
        ensureHistoryCapacity(lookbackFrames);

        int stableAreas = countStableAreas(currentSample, similarityThreshold);

        // Guarda o frame atual no histórico circular de (y) frames anteriores.
        if (currentSample != null) {
            history[historyHead] = currentSample;
            historyHead = (historyHead + 1) % history.length;
            historyCount = Math.min(historyCount + 1, history.length);
        }

        // Padrão local = pelo menos metade das áreas estáveis durante toda a janela,
        // e só é considerado depois que o histórico está completamente preenchido
        // (ou seja, já temos os (y) frames anteriores necessários para comparar).
        boolean windowFull = historyCount >= (history != null ? history.length : Integer.MAX_VALUE);
        boolean patternFound = windowFull && gridSize > 0 && stableAreas >= (gridSize + 1) / 2;

        return patternFound ? Math.max(0, replaceFrames) : 0;
    }
}