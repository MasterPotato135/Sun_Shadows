package com.limelight.mods;

/**
 * Centraliza a amostragem de bytes de frame e o cálculo de similaridade estrutural
 * entre frames consecutivos do bitstream comprimido.
 *
 * Esta lógica foi extraída de MediaCodecDecoderRenderer para permitir reutilização,
 * testabilidade isolada e configuração via constantes dos /mods/.
 *
 * Uso típico:
 *   analyzer.sampleAndCompare(data, length) → int similaridade 0–100
 *   analyzer.reset()                         → limpa baseline (ex: após recovery)
 */
public class FrameSimilarityAnalyzer {

    // ── Constantes centralizadas nos /mods/ ──────────────────────────────────
    private static final int FRAME_SAMPLE_SIZE   = FrameSampleSizeConfig.FRAME_SAMPLE_SIZE;
    private static final int SIMILARITY_BLOCKS   = SimilarityBlocksConfig.SIMILARITY_BLOCKS;
    private static final int SIMILARITY_HEADER_BYTES = SimilarityHeaderBytesConfig.SIMILARITY_HEADER_BYTES;

    // ── Estado interno ───────────────────────────────────────────────────────
    private byte[] lastFrameSample;

    public FrameSimilarityAnalyzer() {
        this.lastFrameSample = null;
    }

    /**
     * Amostra {@code length} bytes de {@code data} de forma distribuída e compara
     * com a amostra do frame anterior.
     *
     * @return similaridade 0 (completamente diferente) a 100 (idêntico), ou 0
     *         se ainda não houver frame anterior (primeira chamada após reset).
     */
    public int sampleAndCompare(byte[] data, int length) {
        byte[] current = sampleFrameBytes(data, length);
        int similarity = (lastFrameSample != null)
                ? getEncodedFrameSimilarity(lastFrameSample, current)
                : 0;
        lastFrameSample = current;
        return similarity;
    }

    /**
     * Retorna a última amostra de frame coletada, ou null se não houver.
     * Útil para componentes externos (ex: BlockCompressionAnalyzer) que precisam
     * da mesma amostra sem reprocessar os dados brutos.
     */
    public byte[] getLastSample() {
        return lastFrameSample;
    }

    /**
     * Reseta a baseline de comparação. Deve ser chamado após recovery do codec
     * para forçar nova referência no próximo frame.
     */
    public void reset() {
        lastFrameSample = null;
    }

    // ── Implementação privada ────────────────────────────────────────────────

    /**
     * Coleta FRAME_SAMPLE_SIZE bytes distribuídos linearmente pelo bitstream.
     * Distribuição uniforme reduz viés para o início/fim do buffer.
     */
    private byte[] sampleFrameBytes(byte[] data, int length) {
        byte[] sample = new byte[FRAME_SAMPLE_SIZE];
        if (length <= 0) {
            return sample;
        }
        for (int i = 0; i < sample.length; i++) {
            int offset = (int)(((long) i * (length - 1)) / Math.max(1, sample.length - 1));
            sample[i] = data[offset];
        }
        return sample;
    }

    /**
     * Similaridade estrutural entre duas amostras de bitstream comprimido.
     *
     * Divide o payload em {@code SIMILARITY_BLOCKS} blocos e extrai três métricas por bloco:
     *   1. Média     — captura mudanças globais de energia no bloco (peso 50%).
     *   2. Variância — captura mudanças de complexidade/textura (peso 25%).
     *   3. Gradiente — captura mudanças de borda entre bytes adjacentes (peso 25%).
     *
     * O cabeçalho NAL (primeiros {@code SIMILARITY_HEADER_BYTES}) contribui com 20%
     * do score final via comparação exata — diferenças ali indicam mudança de tipo de
     * frame, não de conteúdo visual.
     *
     * @return 0 (completamente diferente) a 100 (idêntico).
     */
    private int getEncodedFrameSimilarity(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 0;
        }

        // Cabeçalho NAL: comparação exata
        int headerEnd = Math.min(SIMILARITY_HEADER_BYTES, a.length);
        int headerHits = 0;
        for (int i = 0; i < headerEnd; i++) {
            if (a[i] == b[i]) headerHits++;
        }
        int hdrScore100 = (headerEnd > 0) ? (headerHits * 100 / headerEnd) : 100;

        // Payload: análise estrutural por blocos
        int payloadStart = headerEnd;
        int payloadLen   = a.length - payloadStart;
        if (payloadLen <= 0) {
            return hdrScore100;
        }

        int blockLen = Math.max(1, payloadLen / SIMILARITY_BLOCKS);
        long totalBlockScore = 0;
        int blocksUsed = 0;

        for (int bi = 0; bi < SIMILARITY_BLOCKS; bi++) {
            int start = payloadStart + bi * blockLen;
            int end   = (bi == SIMILARITY_BLOCKS - 1) ? a.length : Math.min(start + blockLen, a.length);
            if (end <= start) continue;
            int len = end - start;

            // 1) Média
            long sumA = 0, sumB = 0;
            for (int i = start; i < end; i++) {
                sumA += (a[i] & 0xFF);
                sumB += (b[i] & 0xFF);
            }
            float meanA = sumA / (float) len;
            float meanB = sumB / (float) len;
            int meanSim = Math.max(0, 100 - (int)(Math.abs(meanA - meanB) * 100f / 255f));

            // 2) Desvio padrão (proxy de variância)
            float varA = 0, varB = 0;
            for (int i = start; i < end; i++) {
                float da = (a[i] & 0xFF) - meanA;
                float db = (b[i] & 0xFF) - meanB;
                varA += da * da;
                varB += db * db;
            }
            float stdDiff = Math.abs((float) Math.sqrt(varA / len) - (float) Math.sqrt(varB / len));
            int varSim = Math.max(0, 100 - (int)(stdDiff * 100f / 127.5f));

            // 3) Gradiente médio entre bytes adjacentes
            long gradA = 0, gradB = 0;
            for (int i = start; i < end - 1; i++) {
                gradA += Math.abs((a[i] & 0xFF) - (a[i + 1] & 0xFF));
                gradB += Math.abs((b[i] & 0xFF) - (b[i + 1] & 0xFF));
            }
            float gDiff = (len > 1)
                    ? Math.abs(gradA / (float)(len - 1) - gradB / (float)(len - 1))
                    : 0f;
            int gradSim = Math.max(0, 100 - (int)(gDiff * 100f / 255f));

            // Combina: média 50%, variância 25%, gradiente 25%
            totalBlockScore += meanSim * 2 + varSim + gradSim; // max 400 por bloco
            blocksUsed++;
        }

        if (blocksUsed == 0) return hdrScore100;

        int payloadScore = (int)(totalBlockScore * 100L / ((long) blocksUsed * 400));
        // Header: 20%, Payload: 80%
        return (hdrScore100 * 20 + payloadScore * 80) / 100;
    }
}
