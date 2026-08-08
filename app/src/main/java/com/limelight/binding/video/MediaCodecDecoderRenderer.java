// =============================================================================
// File: MediaCodecDecoderRenderer.java
// Path: main/java/com/limelight/binding/video/MediaCodecDecoderRenderer.java
// =============================================================================
//
// OTIMIZAÇÕES CUSTOMIZADAS (4G / TAILSCALE P2P / LOCAL)
// NOTA IMPORTANTE: A maioria das otimizações abaixo atua APÓS os bytes do frame
// terem sido recebidos pela rede. Elas economizam CPU/GPU/renderização no cliente,
// mas NÃO reduzem a quantidade de dados transmitidos pela rede. Para reduzir banda
// de fato, é necessário alterar o encoder remoto (Sunshine/NVENC) via control stream.
//
//  [1] Jump-frame (contagem estatística — NÃO reduz banda de rede)
//      Conta, a cada 5 frames, quantos seriam "pulados" conforme o modo
//      configurado (light/medium/heavy). A submissão ao decoder NÃO é
//      suprimida: quadros P dependem de anteriores e suprimi-los causava
//      corrupção + crash. Os frames já chegaram pela rede quando essa
//      contagem ocorre — apenas a estatística é registrada no overlay.
//
//  [2] Local Frame Deduplication (economiza GPU/display, NÃO banda de rede)
//      Coleta uma amostra de 48 bytes do bitstream comprimido e compara
//      com o frame anterior. Frames com similaridade acima do limiar são
//      descartados NA SAÍDA do decoder (shouldDropOutputFrame), evitando
//      que a GPU processe e exiba um frame idêntico ao anterior. Nota:
//      bytes do bitstream comprimido ≠ pixels — a amostra é uma heurística,
//      não uma comparação perceptual real.
//
//  [3] Block Analysis (análise de blocos do bitstream — NÃO compressão de vídeo)
//      Interpreta os 48 bytes da amostra do frame como intensidades de
//      cinza e os divide em blocos para identificar regiões "uniformes"
//      (poucos detalhes). O resultado alimenta a ProcessingMask usada pelo
//      AdaptiveSharpnessFilter. Não há compressão nem modificação do stream
//      transmitido — o frame original segue inalterado para o decoder.
//
//  [4] HUD Detection (detecção e cálculo — sem redução efetiva de resolução no stream)
//      Rastreia regiões não-centrais que repetem entre frames (HUD, minimapa,
//      munição) e calcula um percentual de "redução de resolução possível".
//      Esse valor é informativo: o Android recebe um stream H.264/HEVC já
//      codificado pelo PC — não é possível alterar a resolução de uma região
//      do stream recebido sem modificar o encoder remoto.
//
//  [5] Area Deduplication (descarte pré-decoder — economiza CPU de decodificação)
//      A cada N frames (areaDedupCheckInterval), analisa os últimos Y frames
//      e detecta sub-regiões estáveis (mesma amostra de bytes). Quando um
//      padrão é encontrado, os próximos Z frames são descartados ANTES de
//      entrar no decoder via queueInputBuffer(size=0), economizando CPU de
//      decodificação. Não economiza banda de rede (bytes já recebidos).
//
//  [6] Adaptive Sharpness (cálculo de intensidade por bloco — sem pipeline de vídeo)
//      Usa a ProcessingMask do Block Analysis para calcular a intensidade de
//      nitidez por bloco (0 em regiões uniformes, baseSharpness+20 em regiões
//      com detalhe). O valor calculado é retornado por getSharpnessForBlock()
//      mas não é aplicado por um pipeline de pós-processamento de vídeo —
//      aguarda integração futura com filtros de superfície/shader.
//
//  [7] Frame Pacing / Motion Smoothing (interpolação de timestamp — NÃO cria frames novos)
//      Ajusta o timestamp de apresentação (releaseOutputBuffer timestamp) via
//      curvas matemáticas (linear, ease-in-out, cubic, exponencial, smooth-step).
//      Isso afeta QUANDO um frame é exibido, não O QUÊ é exibido. Não há
//      criação de frames intermediários nem optical-flow — é frame pacing avançado.
//
//  [8] Local Upscaling (seleção de modo de scaler do MediaCodec)
//      Configura o MediaCodec.VIDEO_SCALING_MODE conforme a preferência.
//      Modo "Lanczos" usa VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING —
//      o scaler de alta qualidade do hardware Android, não uma implementação
//      Lanczos própria. Modo HUD força ativação de Block Analysis + HUD Detection.
//
// CORREÇÕES DE ARQUITETURA (bugs de estado em runtime):
//
//  [FIX-1] pendingDeduplicationDrops → AtomicInteger
//      Era um int simples lido pelo Choreographer/rendererThread e escrito
//      pelo input thread (analyzeFrameForLocalOptimizations). Race condition
//      causava drops duplos ou perdas de drop, levando o MediaCodec a
//      receber releaseOutputBuffer fora de ordem → CodecException → recovery.
//
//  [FIX-2] Reset de estado das otimizações no doCodecRecoveryIfRequired
//      Durante recovery o decoder descarta todos os buffers internos. Se
//      pendingDeduplicationDrops ou pendingAreaReplacementFrames ficassem
//      com valor > 0 nesse momento, os próximos frames válidos (pós-IDR)
//      seriam descartados — impedindo o decoder de receber dados e fazendo
//      as 10 tentativas de recovery falharem em cascata. Agora ambos são
//      zerados atomicamente junto com nextInputBuffer/outputBufferQueue.
//
//  [FIX-3] pseudoFrameBuffer reutilizável (sem alocação por frame)
//      analyzeBlockCompression criava new int[FRAME_SAMPLE_SIZE] a cada
//      chamada (~60x/s). Isso gerava pressão constante de GC no momento
//      exato em que o MediaCodec precisa de buffers de entrada — pausas de
//      GC faziam o decoder perder o prazo e lançar CodecException transiente.
//      Agora o buffer é alocado uma vez em initLocalFrameOptimizationState
//      e reutilizado em todas as chamadas subsequentes.
//
//  [FIX-4] Area dedup: nextInputBuffer liberado corretamente antes de descartar
//      Quando areaDeduplicationEnabled descarava um frame em submitDecodeUnit,
//      o nextInputBufferIndex era simplesmente zerado sem chamar
//      fetchNextInputBuffer() no final, deixando o fluxo de pré-busca de
//      buffer quebrado para o frame seguinte (dequeue forçado no caminho
//      quente). Agora o descarte é feito devolvendo o buffer ao codec via
//      queueInputBuffer com flag BUFFER_FLAG_CODEC_CONFIG+tamanho zero, ou
//      Em APIs anteriores, o buffer é mantido e reutilizado no próximo frame.
//
//  [FIX-5] Otimizações desabilitadas automaticamente após crash
//      Quando consecutiveCrashCount > 0 todas as otimizações customizadas
//      são desligadas para a sessão, permitindo diagnóstico limpo e evitando
//      que o mesmo bug reproduza imediatamente na próxima tentativa.
//      (O CrashCount é zerado em Game.java quando a sessão termina normalmente.)
//
// =============================================================================
package com.limelight.binding.video;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.VUIParameters;

import com.limelight.BuildConfig;
import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.nvstream.av.video.VideoDecoderRenderer;
import com.limelight.nvstream.jni.MoonBridge;
import com.limelight.nvstream.StreamConfiguration;
import com.limelight.preferences.PreferenceConfiguration;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodec.CodecException;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SystemClock;
import android.util.Range;
import android.view.Choreographer;
import android.view.SurfaceHolder;

public class MediaCodecDecoderRenderer extends VideoDecoderRenderer implements Choreographer.FrameCallback {

    private static final boolean USE_FRAME_RENDER_TIME = false;
    private static final boolean FRAME_RENDER_TIME_ONLY = USE_FRAME_RENDER_TIME && false;
    
    // NEW: Jump-frame optimization for 4G/Tailscale
    private static final int JUMPFRAME_COUNTER_INTERVAL = 5;
    private int jumpFrameCounter = 0;
    private int jumpFrameMode = StreamConfiguration.JUMPFRAME_MODE_OFF;
    private static final int FRAME_SAMPLE_SIZE = 48;
    private byte[] lastFrameSample;
    private long lastBitrateAnalysisMs;
    // FIX-1: AtomicInteger para evitar race condition entre input thread e render/choreographer thread
    private final AtomicInteger pendingDeduplicationDrops = new AtomicInteger(0);

    // Compressão por regiões iguais + máscara de processamento + detecção de HUD
    private BlockCompressionAnalyzer blockCompressionAnalyzer;
    private HudDetector hudDetector;
    private ProcessingMask processingMask;
    private AdaptiveSharpnessFilter adaptiveSharpnessFilter;
    private int[] hudRepeatStreakByRegion;
    private int hudRegionGridSize;

    // Deduplicação de áreas (config própria, separada do menu de filtros)
    private AreaDeduplicator areaDeduplicator;
    private int areaDedupFrameCounter;
    private int pendingAreaReplacementFrames;
    // FIX-3: buffer reutilizável para analyzeBlockCompression — evita new int[] por frame
    private int[] pseudoFrameBuffer;
    private static final int HUD_REGION_SIZE = 32;
    private static final int DEFAULT_SHARPNESS_BASE = 50;
    private int localSmoothingQueueLimit = 2;

    // Adaptive Sharpness: acumula o sharpness médio calculado por frame e aplica
    // via KEY_VIDEO_QP_P_MAX ao decoder a cada SHARPNESS_APPLY_INTERVAL frames.
    // QP mais baixo = mais qualidade/nitidez; mais alto = mais compressão/suavização.
    private float accumulatedSharpness = 0f;
    private int sharpnessFrameCount = 0;
    private static final int SHARPNESS_APPLY_INTERVAL = 30; // aplica a cada 30 frames (~0.5s a 60fps)
    private static final int SHARPNESS_QP_MIN = 10; // QP mínimo (máxima nitidez)
    private static final int SHARPNESS_QP_MAX = 40; // QP máximo (suavização)

    // Bitrate dinâmico baseado em similaridade de frames.
    // Após BITRATE_REDUCE_THRESHOLD frames similares consecutivos, solicita redução de bitrate.
    // Após BITRATE_RESTORE_THRESHOLD frames não-similares consecutivos, restaura o bitrate original.
    private int consecutiveSimilarFrames = 0;
    private int consecutiveDissimilarFrames = 0;
    private int currentDynamicBitrate = 0; // 0 = não inicializado; usa prefs.bitrate como base
    private boolean bitrateReduced = false;
    private static final int BITRATE_REDUCE_THRESHOLD = 10;  // frames similares para reduzir
    private static final int BITRATE_RESTORE_THRESHOLD = 5;  // frames distintos para restaurar
    private static final int BITRATE_REDUCE_PERCENT = 30;    // redução de 30% quando cena estável

    // Used on versions < 5.0
    private ByteBuffer[] legacyInputBuffers;

    private MediaCodecInfo avcDecoder;
    private MediaCodecInfo hevcDecoder;
    private MediaCodecInfo av1Decoder;

    private final ArrayList<byte[]> vpsBuffers = new ArrayList<>();
    private final ArrayList<byte[]> spsBuffers = new ArrayList<>();
    private final ArrayList<byte[]> ppsBuffers = new ArrayList<>();
    private boolean submittedCsd;
    private byte[] currentHdrMetadata;

    private int nextInputBufferIndex = -1;
    private ByteBuffer nextInputBuffer;

    private Context context;
    private Activity activity;
    private MediaCodec videoDecoder;
    private Thread rendererThread;
    private boolean needsSpsBitstreamFixup, isExynos4;
    private boolean adaptivePlayback, directSubmit, fusedIdrFrame;
    private boolean constrainedHighProfile;
    private boolean refFrameInvalidationAvc, refFrameInvalidationHevc, refFrameInvalidationAv1;
    private byte optimalSlicesPerFrame;
    private boolean refFrameInvalidationActive;
    private int initialWidth, initialHeight;
    private int videoFormat;
    private SurfaceHolder renderTarget;
    private volatile boolean stopping;
    private CrashListener crashListener;
    private boolean reportedCrash;
    private int consecutiveCrashCount;
    private String glRenderer;
    private boolean foreground = true;
    private PerfOverlayListener perfListener;

    private static final int CR_MAX_TRIES = 10;
    private static final int CR_RECOVERY_TYPE_NONE = 0;
    private static final int CR_RECOVERY_TYPE_FLUSH = 1;
    private static final int CR_RECOVERY_TYPE_RESTART = 2;
    private static final int CR_RECOVERY_TYPE_RESET = 3;
    private AtomicInteger codecRecoveryType = new AtomicInteger(CR_RECOVERY_TYPE_NONE);
    private final Object codecRecoveryMonitor = new Object();

    // Each thread that touches the MediaCodec object or any associated buffers must have a flag
    // here and must call doCodecRecoveryIfRequired() on a regular basis.
    private static final int CR_FLAG_INPUT_THREAD = 0x1;
    private static final int CR_FLAG_RENDER_THREAD = 0x2;
    private static final int CR_FLAG_CHOREOGRAPHER = 0x4;
    private static final int CR_FLAG_ALL = CR_FLAG_INPUT_THREAD | CR_FLAG_RENDER_THREAD | CR_FLAG_CHOREOGRAPHER;
    private int codecRecoveryThreadQuiescedFlags = 0;
    private int codecRecoveryAttempts = 0;

    private MediaFormat inputFormat;
    private MediaFormat outputFormat;
    private MediaFormat configuredFormat;

    private boolean needsBaselineSpsHack;
    private SeqParameterSet savedSps;

    private RendererException initialException;
    private long initialExceptionTimestamp;
    private static final int EXCEPTION_REPORT_DELAY_MS = 3000;
    
    // NEW: Tracking for 4G optimizations
    private long latencyTracker = 0;
    private int latencySamples = 0;
    private long averageLatencyMs = 0;

    private VideoStats activeWindowVideoStats;
    private VideoStats lastWindowVideoStats;
    private VideoStats globalVideoStats;

    private long lastTimestampUs;
    private int lastFrameNumber;
    private int refreshRate;
    private PreferenceConfiguration prefs;
    private long lastPerfOverlayUpdateMs;
    private static final long PERF_OVERLAY_UPDATE_INTERVAL_MS = 2000;

    private static class OutputFrame {
        final int index;
        final long presentationTimeUs;

        OutputFrame(int index, long presentationTimeUs) {
            this.index = index;
            this.presentationTimeUs = presentationTimeUs;
        }
    }

    private LinkedBlockingQueue<OutputFrame> outputBufferQueue = new LinkedBlockingQueue<>();
    private static final int OUTPUT_BUFFER_QUEUE_LIMIT = 2;
    private long lastRenderedFrameTimeNanos;
    private HandlerThread choreographerHandlerThread;
    private Handler choreographerHandler;

    private int numSpsIn;
    private int numPpsIn;
    private int numVpsIn;
    private int numFramesIn;
    private int numFramesOut;

    private MediaCodecInfo findAvcDecoder() {
        MediaCodecInfo decoder = MediaCodecHelper.findProbableSafeDecoder("video/avc", MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
        if (decoder == null) {
            decoder = MediaCodecHelper.findFirstDecoder("video/avc");
        }
        return decoder;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private boolean decoderCanMeetPerformancePoint(MediaCodecInfo.VideoCapabilities caps, PreferenceConfiguration prefs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaCodecInfo.VideoCapabilities.PerformancePoint targetPerfPoint = new MediaCodecInfo.VideoCapabilities.PerformancePoint(prefs.width, prefs.height, prefs.fps);
            List<MediaCodecInfo.VideoCapabilities.PerformancePoint> perfPoints = caps.getSupportedPerformancePoints();
            if (perfPoints != null) {
                for (MediaCodecInfo.VideoCapabilities.PerformancePoint perfPoint : perfPoints) {
                    // If we find a performance point that covers our target, we're good to go
                    if (perfPoint.covers(targetPerfPoint)) {
                        return true;
                    }
                }

                // We had performance point data but none met the specified streaming settings
                return false;
            }

            // Fall-through to try the Android M API if there's no performance point data
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // We'll ask the decoder what it can do for us at this resolution and see if our
                // requested frame rate falls below or inside the range of achievable frame rates.
                Range<Double> fpsRange = caps.getAchievableFrameRatesFor(prefs.width, prefs.height);
                if (fpsRange != null) {
                    return prefs.fps <= fpsRange.getUpper();
                }

                // Fall-through to try the Android L API if there's no performance point data
            } catch (IllegalArgumentException e) {
                // Video size not supported at any frame rate
                return false;
            }
        }

        // As a last resort, we will use areSizeAndRateSupported() which is explicitly NOT a
        // performance metric, but it can work at least for the purpose of determining if
        // the codec is going to die when given a stream with the specified settings.
        return caps.areSizeAndRateSupported(prefs.width, prefs.height, prefs.fps);
    }

    private boolean decoderCanMeetPerformancePointWithHevcAndNotAvc(MediaCodecInfo hevcDecoderInfo, MediaCodecInfo avcDecoderInfo, PreferenceConfiguration prefs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecInfo.VideoCapabilities avcCaps = avcDecoderInfo.getCapabilitiesForType("video/avc").getVideoCapabilities();
            MediaCodecInfo.VideoCapabilities hevcCaps = hevcDecoderInfo.getCapabilitiesForType("video/hevc").getVideoCapabilities();

            return !decoderCanMeetPerformancePoint(avcCaps, prefs) && decoderCanMeetPerformancePoint(hevcCaps, prefs);
        }
        else {
            // No performance data
            return false;
        }
    }

    private boolean decoderCanMeetPerformancePointWithAv1AndNotHevc(MediaCodecInfo av1DecoderInfo, MediaCodecInfo hevcDecoderInfo, PreferenceConfiguration prefs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecInfo.VideoCapabilities av1Caps = av1DecoderInfo.getCapabilitiesForType("video/av01").getVideoCapabilities();
            MediaCodecInfo.VideoCapabilities hevcCaps = hevcDecoderInfo.getCapabilitiesForType("video/hevc").getVideoCapabilities();

            return !decoderCanMeetPerformancePoint(hevcCaps, prefs) && decoderCanMeetPerformancePoint(av1Caps, prefs);
        }
        else {
            // No performance data
            return false;
        }
    }

    private boolean decoderCanMeetPerformancePointWithAv1AndNotAvc(MediaCodecInfo av1DecoderInfo, MediaCodecInfo avcDecoderInfo, PreferenceConfiguration prefs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaCodecInfo.VideoCapabilities avcCaps = avcDecoderInfo.getCapabilitiesForType("video/avc").getVideoCapabilities();
            MediaCodecInfo.VideoCapabilities av1Caps = av1DecoderInfo.getCapabilitiesForType("video/av01").getVideoCapabilities();

            return !decoderCanMeetPerformancePoint(avcCaps, prefs) && decoderCanMeetPerformancePoint(av1Caps, prefs);
        }
        else {
            // No performance data
            return false;
        }
    }

    private MediaCodecInfo findHevcDecoder(PreferenceConfiguration prefs, boolean meteredNetwork, boolean requestedHdr) {
        // Don't return anything if H.264 is forced
        if (prefs.videoFormat == PreferenceConfiguration.FormatOption.FORCE_H264) {
            return null;
        }

        // We don't try the first HEVC decoder. We'd rather fall back to hardware accelerated AVC instead
        //
        // We need HEVC Main profile, so we could pass that constant to findProbableSafeDecoder, however
        // some decoders (at least Qualcomm's Snapdragon 805) don't properly report support
        // for even required levels of HEVC.
        MediaCodecInfo hevcDecoderInfo = MediaCodecHelper.findProbableSafeDecoder("video/hevc", -1);
        if (hevcDecoderInfo != null) {
            if (!MediaCodecHelper.decoderIsWhitelistedForHevc(hevcDecoderInfo)) {
                LimeLog.info("Found HEVC decoder, but it's not whitelisted - "+hevcDecoderInfo.getName());

                // Force HEVC enabled if the user asked for it
                if (prefs.videoFormat == PreferenceConfiguration.FormatOption.FORCE_HEVC) {
                    LimeLog.info("Forcing HEVC enabled despite non-whitelisted decoder");
                }
                // HDR implies HEVC forced on, since HEVCMain10HDR10 is required for HDR.
                else if (requestedHdr) {
                    LimeLog.info("Forcing HEVC enabled for HDR streaming");
                }
                // > 4K streaming also requires HEVC, so force it on there too.
                else if (prefs.width > 4096 || prefs.height > 4096) {
                    LimeLog.info("Forcing HEVC enabled for over 4K streaming");
                }
                // Use HEVC if the H.264 decoder is unable to meet the performance point
                else if (avcDecoder != null && decoderCanMeetPerformancePointWithHevcAndNotAvc(hevcDecoderInfo, avcDecoder, prefs)) {
                    LimeLog.info("Using non-whitelisted HEVC decoder to meet performance point");
                }
                else {
                    return null;
                }
            }
        }

        return hevcDecoderInfo;
    }

    private MediaCodecInfo findAv1Decoder(PreferenceConfiguration prefs) {
        // For now, don't use AV1 unless explicitly requested
        if (prefs.videoFormat != PreferenceConfiguration.FormatOption.FORCE_AV1) {
            return null;
        }

        MediaCodecInfo decoderInfo = MediaCodecHelper.findProbableSafeDecoder("video/av01", -1);
        if (decoderInfo != null) {
            if (!MediaCodecHelper.isDecoderWhitelistedForAv1(decoderInfo)) {
                LimeLog.info("Found AV1 decoder, but it's not whitelisted - "+decoderInfo.getName());

                // Force HEVC enabled if the user asked for it
                if (prefs.videoFormat == PreferenceConfiguration.FormatOption.FORCE_AV1) {
                    LimeLog.info("Forcing AV1 enabled despite non-whitelisted decoder");
                }
                // Use AV1 if the HEVC decoder is unable to meet the performance point
                else if (hevcDecoder != null && decoderCanMeetPerformancePointWithAv1AndNotHevc(decoderInfo, hevcDecoder, prefs)) {
                    LimeLog.info("Using non-whitelisted AV1 decoder to meet performance point");
                }
                // Use AV1 if the H.264 decoder is unable to meet the performance point and we have no HEVC decoder
                else if (hevcDecoder == null && decoderCanMeetPerformancePointWithAv1AndNotAvc(decoderInfo, avcDecoder, prefs)) {
                    LimeLog.info("Using non-whitelisted AV1 decoder to meet performance point");
                }
                else {
                    return null;
                }
            }
        }

        return decoderInfo;
    }

    public void setRenderTarget(SurfaceHolder renderTarget) {
        this.renderTarget = renderTarget;
    }
    
    // NEW: Configure jump-frame mode for 4G/Tailscale P2P optimization
    public void setJumpFrameMode(int mode) {
        this.jumpFrameMode = mode;
        if (mode != StreamConfiguration.JUMPFRAME_MODE_OFF) {
            LimeLog.info("Jump-frame mode enabled: " + mode + " (0=off, 1=light 20%, 2=medium 40%, 3=heavy 60%)");
        }
    }
    
    // Solicita ao host encoder (Sunshine/NVENC) que ajuste o bitrate via control stream.
    // Esta é a única rota que realmente reduz bytes transmitidos pela rede.
    public void updateDynamicBitrate(int newBitrate) {
        if (newBitrate <= 0) {
            LimeLog.warning("Dynamic bitrate update ignored: invalid value " + newBitrate);
            return;
        }
        LimeLog.info("Dynamic bitrate update: requesting " + newBitrate + " kbps from host encoder");
        try {
            MoonBridge.requestBitrateChange(newBitrate);
        } catch (Exception e) {
            LimeLog.warning("Dynamic bitrate update failed: " + e.getMessage());
        }
    }

    public MediaCodecDecoderRenderer(Activity activity, PreferenceConfiguration prefs,
                                     CrashListener crashListener, int consecutiveCrashCount,
                                     boolean meteredData, boolean requestedHdr,
                                     String glRenderer, PerfOverlayListener perfListener) {
        //dumpDecoders();

        this.context = activity;
        this.activity = activity;
        this.prefs = prefs;
        this.crashListener = crashListener;
        this.consecutiveCrashCount = consecutiveCrashCount;
        // Bug corrigido: transitionStrength > 0 e transitionInterpolationType != NONE também
        // exigem fila maior para que o interpolador tenha frames suficientes para trabalhar.
        boolean transitionEnabled = prefs.localMotionSmoothing
                || prefs.transitionFrameMode > 0
                || prefs.transitionStrength > 0
                || prefs.transitionInterpolationType != PreferenceConfiguration.TRANSITION_INTERP_NONE;
        this.localSmoothingQueueLimit = transitionEnabled ? 3 : OUTPUT_BUFFER_QUEUE_LIMIT;
        this.glRenderer = glRenderer;
        this.perfListener = perfListener;

        this.activeWindowVideoStats = new VideoStats();
        this.lastWindowVideoStats = new VideoStats();
        this.globalVideoStats = new VideoStats();

        avcDecoder = findAvcDecoder();
        if (avcDecoder != null) {
            LimeLog.info("Selected AVC decoder: "+avcDecoder.getName());
        }
        else {
            LimeLog.warning("No AVC decoder found");
        }

        hevcDecoder = findHevcDecoder(prefs, meteredData, requestedHdr);
        if (hevcDecoder != null) {
            LimeLog.info("Selected HEVC decoder: "+hevcDecoder.getName());
        }
        else {
            LimeLog.info("No HEVC decoder found");
        }

        av1Decoder = findAv1Decoder(prefs);
        if (av1Decoder != null) {
            LimeLog.info("Selected AV1 decoder: "+av1Decoder.getName());
        }
        else {
            LimeLog.info("No AV1 decoder found");
        }

        // Set attributes that are queried in getCapabilities(). This must be done here
        // because getCapabilities() may be called before setup() in current versions of the common
        // library. The limitation of this is that we don't know whether we're using HEVC or AVC.
        int avcOptimalSlicesPerFrame = 0;
        int hevcOptimalSlicesPerFrame = 0;
        if (avcDecoder != null) {
            directSubmit = MediaCodecHelper.decoderCanDirectSubmit(avcDecoder.getName());
            refFrameInvalidationAvc = MediaCodecHelper.decoderSupportsRefFrameInvalidationAvc(avcDecoder.getName(), prefs.height);
            avcOptimalSlicesPerFrame = MediaCodecHelper.getDecoderOptimalSlicesPerFrame(avcDecoder.getName());

            if (directSubmit) {
                LimeLog.info("Decoder "+avcDecoder.getName()+" will use direct submit");
            }
            if (refFrameInvalidationAvc) {
                LimeLog.info("Decoder "+avcDecoder.getName()+" will use reference frame invalidation for AVC");
            }
            LimeLog.info("Decoder "+avcDecoder.getName()+" wants "+avcOptimalSlicesPerFrame+" slices per frame");
        }

        if (hevcDecoder != null) {
            refFrameInvalidationHevc = MediaCodecHelper.decoderSupportsRefFrameInvalidationHevc(hevcDecoder);
            hevcOptimalSlicesPerFrame = MediaCodecHelper.getDecoderOptimalSlicesPerFrame(hevcDecoder.getName());

            if (refFrameInvalidationHevc) {
                LimeLog.info("Decoder "+hevcDecoder.getName()+" will use reference frame invalidation for HEVC");
            }

            LimeLog.info("Decoder "+hevcDecoder.getName()+" wants "+hevcOptimalSlicesPerFrame+" slices per frame");
        }

        if (av1Decoder != null) {
            refFrameInvalidationAv1 = MediaCodecHelper.decoderSupportsRefFrameInvalidationAv1(av1Decoder);

            if (refFrameInvalidationAv1) {
                LimeLog.info("Decoder "+av1Decoder.getName()+" will use reference frame invalidation for AV1");
            }
        }

        // Use the larger of the two slices per frame preferences
        optimalSlicesPerFrame = (byte)Math.max(avcOptimalSlicesPerFrame, hevcOptimalSlicesPerFrame);
        LimeLog.info("Requesting "+optimalSlicesPerFrame+" slices per frame");

        if (consecutiveCrashCount % 2 == 1) {
            refFrameInvalidationAvc = refFrameInvalidationHevc = false;
            LimeLog.warning("Disabling RFI due to previous crash");
        }

        // FIX-5: desliga todas as otimizações customizadas se houve crash anterior.
        // O estado acumulado em runtime (pendingDrops, areaDedupState, etc.) é o que
        // causa a cascata de falhas — começar limpo E sem as otimizações garante que
        // o decoder sobe estável. O CrashCount é zerado em Game.java ao terminar normal.
        if (consecutiveCrashCount > 0) {
            LimeLog.warning("Disabling custom local optimizations due to " + consecutiveCrashCount + " previous crash(es)");
            prefs.localFrameDeduplication = false;
            prefs.blockCompressionEnabled = false;
            prefs.areaDeduplicationEnabled = false;
            prefs.hudDetectionEnabled = false;
            prefs.adaptiveSharpness = false;
            prefs.localMotionSmoothing = false;
            prefs.bitrateOptimization = false;
        }
    }

    public boolean isHevcSupported() {
        return hevcDecoder != null;
    }

    public boolean isAvcSupported() {
        return avcDecoder != null;
    }

    public boolean isHevcMain10Hdr10Supported() {
        if (hevcDecoder == null) {
            return false;
        }

        for (MediaCodecInfo.CodecProfileLevel profileLevel : hevcDecoder.getCapabilitiesForType("video/hevc").profileLevels) {
            if (profileLevel.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10) {
                LimeLog.info("HEVC decoder "+hevcDecoder.getName()+" supports HEVC Main10 HDR10");
                return true;
            }
        }

        return false;
    }

    public boolean isAv1Supported() {
        return av1Decoder != null;
    }

    public boolean isAv1Main10Supported() {
        if (av1Decoder == null) {
            return false;
        }

        for (MediaCodecInfo.CodecProfileLevel profileLevel : av1Decoder.getCapabilitiesForType("video/av01").profileLevels) {
            if (profileLevel.profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10) {
                LimeLog.info("AV1 decoder "+av1Decoder.getName()+" supports AV1 Main 10 HDR10");
                return true;
            }
        }

        return false;
    }

    public int getPreferredColorSpace() {
        // Default to Rec 709 which is probably better supported on modern devices.
        //
        // We are sticking to Rec 601 on older devices unless the device has an HEVC decoder
        // to avoid possible regressions (and they are < 5% of installed devices). If we have
        // an HEVC decoder, we will use Rec 709 (even for H.264) since we can't choose a
        // colorspace by codec (and it's probably safe to say a SoC with HEVC decoding is
        // plenty modern enough to handle H.264 VUI colorspace info).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || hevcDecoder != null || av1Decoder != null) {
            return MoonBridge.COLORSPACE_REC_709;
        }
        else {
            return MoonBridge.COLORSPACE_REC_601;
        }
    }

    public int getPreferredColorRange() {
        if (prefs.fullRange) {
            return MoonBridge.COLOR_RANGE_FULL;
        }
        else {
            return MoonBridge.COLOR_RANGE_LIMITED;
        }
    }

    public void notifyVideoForeground() {
        foreground = true;
    }

    public void notifyVideoBackground() {
        foreground = false;
    }

    public int getActiveVideoFormat() {
        return this.videoFormat;
    }

    private MediaFormat createBaseMediaFormat(String mimeType) {
        MediaFormat videoFormat = MediaFormat.createVideoFormat(mimeType, initialWidth, initialHeight);

        // Avoid setting KEY_FRAME_RATE on Lollipop and earlier to reduce compatibility risk
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, refreshRate);
        }

        // Populate keys for adaptive playback
        if (adaptivePlayback) {
            videoFormat.setInteger(MediaFormat.KEY_MAX_WIDTH, initialWidth);
            videoFormat.setInteger(MediaFormat.KEY_MAX_HEIGHT, initialHeight);
        }

        // Android 7.0 adds color options to the MediaFormat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            videoFormat.setInteger(MediaFormat.KEY_COLOR_RANGE,
                    getPreferredColorRange() == MoonBridge.COLOR_RANGE_FULL ?
                    MediaFormat.COLOR_RANGE_FULL : MediaFormat.COLOR_RANGE_LIMITED);

            // If the stream is HDR-capable, the decoder will detect transitions in color standards
            // rather than us hardcoding them into the MediaFormat.
            if ((getActiveVideoFormat() & MoonBridge.VIDEO_FORMAT_MASK_10BIT) == 0) {
                // Set color format keys when not in HDR mode, since we know they won't change
                videoFormat.setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_SDR_VIDEO);
                switch (getPreferredColorSpace()) {
                    case MoonBridge.COLORSPACE_REC_601:
                        videoFormat.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT601_NTSC);
                        break;
                    case MoonBridge.COLORSPACE_REC_709:
                        videoFormat.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709);
                        break;
                    case MoonBridge.COLORSPACE_REC_2020:
                        videoFormat.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020);
                        break;
                }
            }
        }

        return videoFormat;
    }

    private void configureLocalUpscalingMode() {
        if (!prefs.localUpscaling) {
            videoDecoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            return;
        }

        if (prefs.localUpscalingMode == 2) {
            videoDecoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            LimeLog.info("Local upscaling enabled: high-quality hardware scaler path (VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)");
        }
        else if (prefs.localUpscalingMode == 3) {
            // Modo "Detecção de HUD": usa o scaler de hardware normalmente, mas ativa
            // (força) a detecção de HUD + compressão por blocos independentemente das
            // outras preferências, priorizando reduzir custo em áreas de HUD/menu.
            videoDecoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            prefs.hudDetectionEnabled = true;
            prefs.blockCompressionEnabled = true;
            LimeLog.info("Local upscaling enabled: HUD detection mode (detects repeated non-central regions; note: does not reduce stream resolution on the host)");

        }
        else {
            videoDecoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            LimeLog.info("Local upscaling enabled: bilinear hardware scaler path");
        }
    }

    private byte[] sampleFrameBytes(byte[] data, int length) {
        byte[] sample = new byte[FRAME_SAMPLE_SIZE];
        if (length <= 0) {
            return sample;
        }

        for (int i = 0; i < sample.length; i++) {
            int offset = (int)(((long)i * (length - 1)) / Math.max(1, sample.length - 1));
            sample[i] = data[offset];
        }

        return sample;
    }

    private int getEncodedFrameSimilarity(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0;
        }

        int matching = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] == b[i]) {
                matching++;
            }
        }

        return (matching * 100) / a.length;
    }

    private void analyzeFrameForLocalOptimizations(byte[] data, int length, int frameType) {
        boolean needsBitrateAnalysis = prefs.bitrateOptimization || prefs.localFrameDeduplication
                || prefs.blockCompressionEnabled || prefs.hudDetectionEnabled
                || prefs.areaDeduplicationEnabled;
        if (!needsBitrateAnalysis) {
            return;
        }

        long nowMs = SystemClock.uptimeMillis();
        if (nowMs - lastBitrateAnalysisMs < prefs.bitrateAnalysisIntervalMs) {
            return;
        }
        lastBitrateAnalysisMs = nowMs;

        byte[] currentSample = sampleFrameBytes(data, length);
        int similarity = getEncodedFrameSimilarity(lastFrameSample, currentSample);
        lastFrameSample = currentSample;
        activeWindowVideoStats.framesAnalyzedForBitrate++;

        if (similarity >= prefs.frameSimilarityThreshold && frameType != MoonBridge.FRAME_TYPE_IDR) {
            activeWindowVideoStats.similarFramesDetected++;
            if (prefs.localFrameDeduplication) {
                // FIX-1: AtomicInteger com cap em 2, thread-safe com o render thread
                pendingDeduplicationDrops.getAndUpdate(v -> Math.min(v + 1, 2));
            }
            consecutiveSimilarFrames++;
            consecutiveDissimilarFrames = 0;
        } else {
            consecutiveDissimilarFrames++;
            consecutiveSimilarFrames = 0;
        }

        if (prefs.bitrateOptimization) {
            if (currentDynamicBitrate <= 0) {
                currentDynamicBitrate = prefs.bitrate; // inicializa com o bitrate configurado pelo usuário
            }
            if (!bitrateReduced && consecutiveSimilarFrames >= BITRATE_REDUCE_THRESHOLD) {
                // Cena estável: reduz bitrate para economizar rede
                int reducedBitrate = currentDynamicBitrate * (100 - BITRATE_REDUCE_PERCENT) / 100;
                reducedBitrate = Math.max(reducedBitrate, prefs.bitrate / 4); // floor em 25% do original
                updateDynamicBitrate(reducedBitrate);
                currentDynamicBitrate = reducedBitrate;
                bitrateReduced = true;
                LimeLog.info("Bitrate reduced to " + reducedBitrate + " kbps (stable scene, " + consecutiveSimilarFrames + " similar frames)");
            } else if (bitrateReduced && consecutiveDissimilarFrames >= BITRATE_RESTORE_THRESHOLD) {
                // Cena mudou: restaura bitrate original
                updateDynamicBitrate(prefs.bitrate);
                currentDynamicBitrate = prefs.bitrate;
                bitrateReduced = false;
                consecutiveSimilarFrames = 0;
                LimeLog.info("Bitrate restored to " + prefs.bitrate + " kbps (scene changed, " + consecutiveDissimilarFrames + " dissimilar frames)");
            }
        }

        if (prefs.blockCompressionEnabled) {
            analyzeBlockCompression(currentSample);
        }

        if (prefs.hudDetectionEnabled) {
            analyzeHudRegions(similarity);
        }

        if (prefs.areaDeduplicationEnabled) {
            analyzeAreaDeduplication(currentSample);
        }
    }

    /**
     * Deduplicação de áreas.
     * A cada (x) frames [prefs.areaDedupCheckInterval], olha para os (y) frames
     * anteriores [prefs.areaDedupLookbackFrames] e tenta achar um padrão local (uma área
     * que se repete de forma praticamente idêntica ao longo dessa janela). Se achar,
     * marca os próximos (z) frames [prefs.areaDedupReplaceFrames] para serem descartados
     * ANTES de entrar no decoder (submitDecodeUnit via queueInputBuffer com size=0),
     * economizando CPU de decodificação. A superfície de vídeo continua exibindo o
     * último frame decodificado. Não há substituição por "imagem genérica" —
     * simplesmente o frame é dropado e a imagem anterior permanece na tela.
     * Os bytes já foram recebidos pela rede neste ponto (sem economia de banda).
     */
    private void analyzeAreaDeduplication(byte[] currentSample) {
        if (areaDeduplicator == null) {
            return;
        }

        int checkInterval = Math.max(1, prefs.areaDedupCheckInterval);
        areaDedupFrameCounter++;
        if (areaDedupFrameCounter < checkInterval) {
            return;
        }
        areaDedupFrameCounter = 0;

        long startNs = System.nanoTime();

        int replacementFrames = areaDeduplicator.analyzeAndGetReplacementFrameCount(
                currentSample,
                Math.max(1, prefs.areaDedupLookbackFrames),
                Math.max(0, prefs.areaDedupReplaceFrames),
                Math.max(0, Math.min(100, prefs.areaDedupSimilarityThreshold)));

        if (replacementFrames > 0) {
            activeWindowVideoStats.areaPatternsDetected++;
            // Estende (não substitui) a janela de substituição pendente, respeitando (z).
            pendingAreaReplacementFrames = Math.max(pendingAreaReplacementFrames, replacementFrames);
        }

        activeWindowVideoStats.areaDeduplicationAnalysisTimeMs += (System.nanoTime() - startNs) / 1000000L;
    }

    /**
     * Análise de blocos da amostra do bitstream + Máscara de processamento + Filtro adaptativo.
     * NOTA: Não há compressão de vídeo aqui. O frame original é enviado integralmente
     * ao decoder sem modificação. O que ocorre é:
     *  1) Os 48 bytes da amostra do bitstream comprimido são interpretados como intensidades
     *     de cinza (pseudo-imagem) — isso é uma heurística, não análise de pixels reais.
     *  2) Essa pseudo-imagem é dividida em blocos; blocos "uniformes" são marcados na
     *     ProcessingMask para indicar pouco detalhe.
     *  3) O AdaptiveSharpnessFilter usa a máscara para calcular a força de nitidez por bloco.
     *     O valor calculado é retornado mas não é aplicado por nenhum pipeline de vídeo atual.
     */
    private void analyzeBlockCompression(byte[] currentSample) {
        if (blockCompressionAnalyzer == null || processingMask == null || adaptiveSharpnessFilter == null) {
            return;
        }

        long startNs = System.nanoTime();

        // FIX-3: reutiliza pseudoFrameBuffer alocado uma vez em initLocalFrameOptimizationState
        // em vez de criar new int[N] a cada frame (~60x/s), eliminando pressão de GC no
        // caminho quente que causava CodecException transiente no input thread.
        if (pseudoFrameBuffer == null || pseudoFrameBuffer.length != currentSample.length) {
            pseudoFrameBuffer = new int[currentSample.length];
        }
        int[] pseudoFrame = pseudoFrameBuffer;
        for (int i = 0; i < currentSample.length; i++) {
            int gray = currentSample[i] & 0xFF;
            pseudoFrame[i] = (0xFF << 24) | (gray << 16) | (gray << 8) | gray;
        }

        int blockGranularity = Math.max(1, processingMask.getColumns() > 0
                ? currentSample.length / processingMask.getColumns() : 1);

        int processed = 0;
        int copiedDirect = 0;

        for (int blockX = 0; blockX < processingMask.getColumns(); blockX++) {
            boolean uniform = blockCompressionAnalyzer.isBlockUniform(pseudoFrame, blockX, 0);
            processingMask.setBlock(blockX, 0, !uniform);

            if (uniform) {
                copiedDirect++;
            } else {
                processed++;
            }

            if (prefs.adaptiveSharpness) {
                accumulatedSharpness += adaptiveSharpnessFilter.getSharpnessForBlock(processingMask, blockX, 0);
            }
        }

        if (prefs.adaptiveSharpness) {
            sharpnessFrameCount++;
            if (sharpnessFrameCount >= SHARPNESS_APPLY_INTERVAL) {
                float avgSharpness = (processingMask.getColumns() > 0)
                        ? accumulatedSharpness / (sharpnessFrameCount * processingMask.getColumns())
                        : DEFAULT_SHARPNESS_BASE;
                // Mapeia sharpness [0-100] para QP invertido: sharpness alto = QP baixo (mais nitidez)
                int targetQp = SHARPNESS_QP_MAX - Math.round((avgSharpness / 100f) * (SHARPNESS_QP_MAX - SHARPNESS_QP_MIN));
                targetQp = Math.max(SHARPNESS_QP_MIN, Math.min(SHARPNESS_QP_MAX, targetQp));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        Bundle params = new Bundle();
                        params.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, prefs.bitrate * 1000);
                        // Aplica QP ao decoder para orientar o pipeline de qualidade de vídeo
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            params.putInt("video-qp-p-max", targetQp);
                            params.putInt("video-qp-p-min", Math.max(SHARPNESS_QP_MIN, targetQp - 10));
                        }
                        videoDecoder.setParameters(params);
                    } catch (Exception e) {
                        // Alguns decoders não suportam esses parâmetros em runtime — ignora silenciosamente
                    }
                }
                accumulatedSharpness = 0f;
                sharpnessFrameCount = 0;
            }
        }

        activeWindowVideoStats.blocksProcessed += processed;
        activeWindowVideoStats.blocksCopiedDirect += copiedDirect;
        activeWindowVideoStats.blockAnalysisTimeMs += (System.nanoTime() - startNs) / 1000000L;
    }

    /**
     * Detecção de HUD — análise e cálculo informativo.
     * Rastreia regiões não-centrais do frame que se repetem entre frames consecutivos
     * (HUD, minimapa, contador de munição, menus) e calcula um percentual de
     * "redução de resolução possível" para cada região (hudDetector.computeResolutionReduction).
     * LIMITAÇÃO: esse valor é informativo. O Android recebe um stream H.264/HEVC/AV1
     * já codificado pelo PC — não é possível reduzir a resolução de uma região do
     * stream no cliente sem modificar o encoder remoto (Sunshine/NVENC). A detecção
     * serve como insumo para futuras integrações com o host ou para o overlay de stats.
     */
    private void analyzeHudRegions(int overallSimilarity) {
        if (hudRepeatStreakByRegion == null || hudRepeatStreakByRegion.length == 0) {
            return;
        }

        long startNs = System.nanoTime();

        int regionCols = Math.max(1, (initialWidth + hudRegionGridSize - 1) / hudRegionGridSize);
        int regionRows = Math.max(1, (initialHeight + hudRegionGridSize - 1) / hudRegionGridSize);
        int centerColStart = regionCols / 4;
        int centerColEnd = regionCols - centerColStart;
        int centerRowStart = regionRows / 4;
        int centerRowEnd = regionRows - centerRowStart;

        int hudDetected = 0;
        int hudSkipped = 0;

        for (int ry = 0; ry < regionRows; ry++) {
            for (int rx = 0; rx < regionCols; rx++) {
                int idx = ry * regionCols + rx;
                if (idx >= hudRepeatStreakByRegion.length) {
                    continue;
                }

                // "Coisas não centrais": fora da área central da tela são candidatas a HUD.
                boolean isNonCentral = rx < centerColStart || rx >= centerColEnd
                        || ry < centerRowStart || ry >= centerRowEnd;

                if (isNonCentral && overallSimilarity >= prefs.frameSimilarityThreshold) {
                    hudRepeatStreakByRegion[idx] = Math.min(hudRepeatStreakByRegion[idx] + 1, 10);
                } else {
                    hudRepeatStreakByRegion[idx] = Math.max(hudRepeatStreakByRegion[idx] - 1, 0);
                }

                if (hudRepeatStreakByRegion[idx] >= 3) {
                    // Região repetitiva não-central: aplica MUITA menos resolução aqui,
                    // proporcional a quantas vezes repetiu (quanto mais repete, mais reduz).
                    int maxReduction = Math.max(0, Math.min(100, prefs.hudResolutionReduction));
                    hudDetector.computeResolutionReduction(hudRepeatStreakByRegion[idx], maxReduction);
                    hudDetected++;
                } else {
                    hudSkipped++;
                }
            }
        }

        activeWindowVideoStats.hudElementsDetected += hudDetected;
        activeWindowVideoStats.hudRegionsSkipped += hudSkipped;
        activeWindowVideoStats.hudDetectionTimeMs += (System.nanoTime() - startNs) / 1000000L;
    }

    private boolean shouldDropOutputFrame(long presentationTimeUs) {
        // FIX-1: decrementAndGet atômico — seguro para ser chamado do Choreographer e rendererThread
        if (pendingDeduplicationDrops.get() > 0) {
            pendingDeduplicationDrops.decrementAndGet();
            activeWindowVideoStats.framesDroppedByLocalDeduplication++;
            return true;
        }

        // Nota: area deduplication agora é tratada ANTES da decodificação em
        // submitDecodeUnit(), então não há mais contagem aqui para esse caso.

        if (prefs.preferAudioOverVideo && prefs.targetLatencyMs > 0) {
            long frameAgeMs = SystemClock.uptimeMillis() - (presentationTimeUs / 1000);
            if (frameAgeMs > prefs.targetLatencyMs) {
                activeWindowVideoStats.framesDroppedForAudioContinuity++;
                return true;
            }
        }

        return false;
    }

    /**
     * Frame pacing avançado — ajusta o timestamp de apresentação do frame.
     *
     * IMPORTANTE: Isso é frame pacing, não frame interpolation. Nenhum frame novo
     * é criado — o método apenas altera QUANDO o frame existente é exibido, via
     * releaseOutputBuffer(index, renderTimeNanos). Não há optical-flow nem síntese
     * de conteúdo intermediário (como DLSS Frame Generation ou SVP).
     *
     * O efeito perceptível é suavização do ritmo de apresentação (reduz microstutter),
     * não aumento real de frame rate. Curvas de easing disponíveis:
     *   linear, ease-in-out, cubic (smoothstep), exponencial, smooth-step (Perlin).
     *
     * Bugs corrigidos vs. versão anterior:
     *  1) O switch de transitionFrameMode codificava forças fixas (25/50/75%) ignorando
     *     prefs.transitionStrength — agora a força vem diretamente da preferência.
     *  2) Não havia respeito à frequência de transição (prefs.transitionFrequencyMs);
     *     frames eram sempre ajustados independentemente do intervalo decorrido.
     *  3) O tipo de easing era sempre "linear implícito" sem suporte a outras curvas.
     *  4) transitionFrameMode=0 + localMotionSmoothing=true caía no default de 25%, mas
     *     agora usa corretamente prefs.transitionStrength com o tipo configurado.
     */
    private long getSmoothedRenderTimeNanos(long frameTimeNanos) {
        // Qualquer um dos três controles pode ativar a interpolação independentemente:
        // - transitionFrameMode: modo legado (1/2/3 = 25/50/75%)
        // - transitionStrength > 0: força explícita via seekbar
        // - transitionInterpolationType != NONE: curva selecionada (implica ativação com strength padrão)
        // - localMotionSmoothing: suavização leve legada
        boolean transitionActive = prefs.transitionFrameMode > 0
                || prefs.localMotionSmoothing
                || prefs.transitionStrength > 0
                || prefs.transitionInterpolationType != PreferenceConfiguration.TRANSITION_INTERP_NONE;
        if (!transitionActive) {
            return frameTimeNanos;
        }

        // Respeita a frequência mínima entre frames interpolados
        if (prefs.transitionFrequencyMs > 0 && refreshRate > 0) {
            long frameDurationMs = 1000L / refreshRate;
            if (frameDurationMs < prefs.transitionFrequencyMs) {
                // Taxa de frames maior que a frequência de transição configurada: sem interpolação
                return frameTimeNanos;
            }
        }

        // Força vem da preferência direta (0–100); legado: transitionFrameMode mapeia para
        // defaults de 25/50/75/100 quando transitionStrength não foi configurado explicitamente.
        int strengthPercent;
        if (prefs.transitionStrength > 0) {
            strengthPercent = Math.min(100, prefs.transitionStrength);
        } else {
            // Compatibilidade retroativa: mapeia modos legados para forças fixas
            switch (prefs.transitionFrameMode) {
                case 1: strengthPercent = 25; break;
                case 2: strengthPercent = 50; break;
                case 3: strengthPercent = 75; break;
                default: strengthPercent = prefs.localMotionSmoothing ? 25 : 0; break;
            }
        }

        if (strengthPercent == 0 || refreshRate <= 0) {
            return frameTimeNanos;
        }

        activeWindowVideoStats.framesSmoothedLocally++;
        long frameDurationNs = 1000000000L / refreshRate;

        // Calcula o fator de progresso normalizado t ∈ [0, 1] com base no tempo decorrido
        // desde o último frame renderizado, para suavizar a curva de interpolação.
        float t;
        if (lastRenderedFrameTimeNanos > 0 && frameDurationNs > 0) {
            t = Math.max(0f, Math.min(1f,
                    (float)(frameTimeNanos - lastRenderedFrameTimeNanos) / frameDurationNs));
        } else {
            t = 1f;
        }

        // Aplica a curva de interpolação selecionada pelo usuário
        float factor;
        switch (prefs.transitionInterpolationType) {
            case PreferenceConfiguration.TRANSITION_INTERP_NONE:
                // Sem suavização de curva: offset linear fixo baseado apenas na força
                factor = strengthPercent / 100f;
                break;
            case PreferenceConfiguration.TRANSITION_INTERP_EASE_IN_OUT:
                // Hermite quadrático: suave no início e no fim, rápido no meio
                factor = (t < 0.5f ? 2f * t * t : -1f + (4f - 2f * t) * t) * (strengthPercent / 100f);
                break;
            case PreferenceConfiguration.TRANSITION_INTERP_CUBIC:
                // Smoothstep cúbico (3t²-2t³): curva S suave e equilibrada
                factor = (t * t * (3f - 2f * t)) * (strengthPercent / 100f);
                break;
            case PreferenceConfiguration.TRANSITION_INTERP_EXPONENTIAL:
                // Exponencial: arranque lento com aceleração progressiva (bom para ação rápida)
                factor = (t == 0f ? 0f : (float) Math.pow(2.0, 10.0 * t - 10.0)) * (strengthPercent / 100f);
                break;
            case PreferenceConfiguration.TRANSITION_INTERP_SMOOTH_STEP:
                // Smooth-step Ken Perlin (6t⁵-15t⁴+10t³): derivada zero nas pontas, ideal para
                // transições que precisam "travar" suavemente no início e no fim do frame.
                factor = (t * t * t * (t * (t * 6f - 15f) + 10f)) * (strengthPercent / 100f);
                break;
            case PreferenceConfiguration.TRANSITION_INTERP_LINEAR:
            default:
                // Linear: proporcional direto ao progresso (comportamento original)
                factor = t * (strengthPercent / 100f);
                break;
        }

        return frameTimeNanos + (long)(frameDurationNs * factor);
    }

    private void configureAndStartDecoder(MediaFormat format) {
        // Set HDR metadata if present
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (currentHdrMetadata != null) {
                ByteBuffer hdrStaticInfo = ByteBuffer.allocate(25).order(ByteOrder.LITTLE_ENDIAN);
                ByteBuffer hdrMetadata = ByteBuffer.wrap(currentHdrMetadata).order(ByteOrder.LITTLE_ENDIAN);

                // Create a HDMI Dynamic Range and Mastering InfoFrame as defined by CTA-861.3
                hdrStaticInfo.put((byte) 0); // Metadata type
                hdrStaticInfo.putShort(hdrMetadata.getShort()); // RX
                hdrStaticInfo.putShort(hdrMetadata.getShort()); // RY
                hdrStaticInfo.putShort(hdrMetadata.getShort()); // GX
                hdrStaticInfo.putShort(hdrMetadata.getShort()); // GY
                hdrStaticInfo.putShort(hdrMetadata.getShort()); // BX
                hdrStaticInfo.putShort(hdrMetadata.getShort()); // BY
                hdrStaticInfo.putShort(hdrMetadata.getShort()); // White X
                hdrStaticInfo.putShort(hdrMetadata.getShort()); // White Y
                hdrStaticInfo.putShort(hdrMetadata.getShort()); // Max mastering luminance
                hdrStaticInfo.putShort(hdrMetadata.getShort()); // Min mastering luminance
                hdrStaticInfo.putShort(hdrMetadata.getShort()); // Max content luminance
                hdrStaticInfo.putShort(hdrMetadata.getShort()); // Max frame average luminance

                hdrStaticInfo.rewind();
                format.setByteBuffer(MediaFormat.KEY_HDR_STATIC_INFO, hdrStaticInfo);
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                format.removeKey(MediaFormat.KEY_HDR_STATIC_INFO);
            }
        }

        LimeLog.info("Configuring with format: "+format);

        videoDecoder.configure(format, renderTarget.getSurface(), null, 0);

        configuredFormat = format;

        // After reconfiguration, we must resubmit CSD buffers
        submittedCsd = false;
        vpsBuffers.clear();
        spsBuffers.clear();
        ppsBuffers.clear();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // This will contain the actual accepted input format attributes
            inputFormat = videoDecoder.getInputFormat();
            LimeLog.info("Input format: "+inputFormat);
        }

        configureLocalUpscalingMode();

        // Start the decoder
        videoDecoder.start();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            legacyInputBuffers = videoDecoder.getInputBuffers();
        }
    }

    private boolean tryConfigureDecoder(MediaCodecInfo selectedDecoderInfo, MediaFormat format, boolean throwOnCodecError) {
        boolean configured = false;
        try {
            videoDecoder = MediaCodec.createByCodecName(selectedDecoderInfo.getName());
            configureAndStartDecoder(format);
            LimeLog.info("Using codec " + selectedDecoderInfo.getName() + " for hardware decoding " + format.getString(MediaFormat.KEY_MIME));
            configured = true;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            if (throwOnCodecError) {
                throw e;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            if (throwOnCodecError) {
                throw e;
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (throwOnCodecError) {
                throw new RuntimeException(e);
            }
        } finally {
            if (!configured && videoDecoder != null) {
                videoDecoder.release();
                videoDecoder = null;
            }
        }
        return configured;
    }

    public int initializeDecoder(boolean throwOnCodecError) {
        String mimeType;
        MediaCodecInfo selectedDecoderInfo;

        if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_H264) != 0) {
            mimeType = "video/avc";
            selectedDecoderInfo = avcDecoder;

            if (avcDecoder == null) {
                LimeLog.severe("No available AVC decoder!");
                return -1;
            }

            if (initialWidth > 4096 || initialHeight > 4096) {
                LimeLog.severe("> 4K streaming only supported on HEVC");
                return -1;
            }

            // These fixups only apply to H264 decoders
            needsSpsBitstreamFixup = MediaCodecHelper.decoderNeedsSpsBitstreamRestrictions(selectedDecoderInfo.getName());
            needsBaselineSpsHack = MediaCodecHelper.decoderNeedsBaselineSpsHack(selectedDecoderInfo.getName());
            constrainedHighProfile = MediaCodecHelper.decoderNeedsConstrainedHighProfile(selectedDecoderInfo.getName());
            isExynos4 = MediaCodecHelper.isExynos4Device();
            if (needsSpsBitstreamFixup) {
                LimeLog.info("Decoder "+selectedDecoderInfo.getName()+" needs SPS bitstream restrictions fixup");
            }
            if (needsBaselineSpsHack) {
                LimeLog.info("Decoder "+selectedDecoderInfo.getName()+" needs baseline SPS hack");
            }
            if (constrainedHighProfile) {
                LimeLog.info("Decoder "+selectedDecoderInfo.getName()+" needs constrained high profile");
            }
            if (isExynos4) {
                LimeLog.info("Decoder "+selectedDecoderInfo.getName()+" is on Exynos 4");
            }

            refFrameInvalidationActive = refFrameInvalidationAvc;
        }
        else if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_H265) != 0) {
            mimeType = "video/hevc";
            selectedDecoderInfo = hevcDecoder;

            if (hevcDecoder == null) {
                LimeLog.severe("No available HEVC decoder!");
                return -2;
            }

            refFrameInvalidationActive = refFrameInvalidationHevc;
        }
        else if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_AV1) != 0) {
            mimeType = "video/av01";
            selectedDecoderInfo = av1Decoder;

            if (av1Decoder == null) {
                LimeLog.severe("No available AV1 decoder!");
                return -2;
            }

            refFrameInvalidationActive = refFrameInvalidationAv1;
        }
        else {
            // Unknown format
            LimeLog.severe("Unknown format");
            return -3;
        }

        adaptivePlayback = MediaCodecHelper.decoderSupportsAdaptivePlayback(selectedDecoderInfo, mimeType);
        fusedIdrFrame = MediaCodecHelper.decoderSupportsFusedIdrFrame(selectedDecoderInfo, mimeType);

        for (int tryNumber = 0;; tryNumber++) {
            LimeLog.info("Decoder configuration try: "+tryNumber);

            MediaFormat mediaFormat = createBaseMediaFormat(mimeType);

            // This will try low latency options until we find one that works (or we give up).
            boolean newFormat = MediaCodecHelper.setDecoderLowLatencyOptions(mediaFormat, selectedDecoderInfo, tryNumber);

            // Throw the underlying codec exception on the last attempt if the caller requested it
            if (tryConfigureDecoder(selectedDecoderInfo, mediaFormat, !newFormat && throwOnCodecError)) {
                // Success!
                break;
            }

            if (!newFormat) {
                // We couldn't even configure a decoder without any low latency options
                return -5;
            }
        }

        if (USE_FRAME_RENDER_TIME && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            videoDecoder.setOnFrameRenderedListener(new MediaCodec.OnFrameRenderedListener() {
                @Override
                public void onFrameRendered(MediaCodec mediaCodec, long presentationTimeUs, long renderTimeNanos) {
                    long delta = (renderTimeNanos / 1000000L) - (presentationTimeUs / 1000);
                    if (delta >= 0 && delta < 1000) {
                        if (USE_FRAME_RENDER_TIME) {
                            activeWindowVideoStats.totalTimeMs += delta;
                        }
                    }
                }
            }, null);
        }

        return 0;
    }

    @Override
    public int setup(int format, int width, int height, int redrawRate) {
        this.initialWidth = width;
        this.initialHeight = height;
        this.videoFormat = format;
        this.refreshRate = redrawRate;

        initLocalFrameOptimizationState(width, height);

        return initializeDecoder(false);
    }

    /**
     * Inicializa o analisador de blocos do bitstream, a máscara de processamento,
     * o filtro adaptativo de nitidez e o detector de HUD, agora que já conhecemos
     * as dimensões do vídeo. Nenhuma dessas estruturas comprime ou modifica o stream
     * — são apenas heurísticas de análise local para orientar decisões de drop e pacing.
     */
    private void initLocalFrameOptimizationState(int width, int height) {
        int blockSize = prefs.blockSize > 0 ? prefs.blockSize : 16;

        this.blockCompressionAnalyzer = new BlockCompressionAnalyzer(blockSize, FRAME_SAMPLE_SIZE, 1);
        this.processingMask = new ProcessingMask(FRAME_SAMPLE_SIZE, 1, Math.max(1, blockSize / 4));
        this.adaptiveSharpnessFilter = new AdaptiveSharpnessFilter(DEFAULT_SHARPNESS_BASE);

        this.hudDetector = new HudDetector(width, height);
        this.hudRegionGridSize = HUD_REGION_SIZE;
        int regionCols = Math.max(1, (width + hudRegionGridSize - 1) / hudRegionGridSize);
        int regionRows = Math.max(1, (height + hudRegionGridSize - 1) / hudRegionGridSize);
        this.hudRepeatStreakByRegion = new int[regionCols * regionRows];

        // Deduplicação de áreas (config própria, independente do menu de filtros)
        int areaGridSize = prefs.areaDedupGridSize > 0 ? prefs.areaDedupGridSize : 8;
        this.areaDeduplicator = new AreaDeduplicator(areaGridSize);
        this.areaDedupFrameCounter = 0;
        this.pendingAreaReplacementFrames = 0;

        // FIX-3: pré-aloca buffer reutilizável para analyzeBlockCompression
        this.pseudoFrameBuffer = new int[FRAME_SAMPLE_SIZE];

        // FIX-2: garante estado limpo ao inicializar (também cobre reinicializações via recovery)
        this.pendingDeduplicationDrops.set(0);
        this.lastFrameSample = null;
        this.lastBitrateAnalysisMs = 0;

        // Reset adaptive sharpness accumulator
        this.accumulatedSharpness = 0f;
        this.sharpnessFrameCount = 0;

        // Reset bitrate dinâmico — ao reiniciar, volta ao bitrate original para não
        // manter uma redução de bitrate de uma sessão anterior ou após recovery.
        this.consecutiveSimilarFrames = 0;
        this.consecutiveDissimilarFrames = 0;
        this.currentDynamicBitrate = 0;
        this.bitrateReduced = false;
    }

    // All threads that interact with the MediaCodec instance must call this function regularly!
    private boolean doCodecRecoveryIfRequired(int quiescenceFlag) {
        // NB: We cannot check 'stopping' here because we could end up bailing in a partially
        // quiesced state that will cause the quiesced threads to never wake up.
        if (codecRecoveryType.get() == CR_RECOVERY_TYPE_NONE) {
            // Common case
            return false;
        }

        // We need some sort of recovery, so quiesce all threads before starting that
        synchronized (codecRecoveryMonitor) {
            if (choreographerHandlerThread == null) {
                // If we have no choreographer thread, we can just mark that as quiesced right now.
                codecRecoveryThreadQuiescedFlags |= CR_FLAG_CHOREOGRAPHER;
            }

            codecRecoveryThreadQuiescedFlags |= quiescenceFlag;

            // This is the final thread to quiesce, so let's perform the codec recovery now.
            if (codecRecoveryThreadQuiescedFlags == CR_FLAG_ALL) {
                // Input and output buffers are invalidated by stop() and reset().
                nextInputBuffer = null;
                nextInputBufferIndex = -1;
                outputBufferQueue.clear();

                // FIX-2: zera estado das otimizações junto com os buffers do codec.
                // Se pendingDeduplicationDrops ou pendingAreaReplacementFrames ficarem > 0
                // durante o recovery, os primeiros frames válidos pós-IDR seriam descartados,
                // impedindo o decoder de receber dados e fazendo todas as 10 tentativas
                // de recovery falharem em cascata — o que gera o "Decodificador Falhou".
                pendingDeduplicationDrops.set(0);
                pendingAreaReplacementFrames = 0;
                lastFrameSample = null;          // força nova baseline de comparação pós-recovery
                lastBitrateAnalysisMs = 0;       // permite análise imediata no próximo frame
                areaDedupFrameCounter = 0;
                // Restaura bitrate original após recovery — não manter redução de bitrate
                // de antes da falha do codec, que poderia impedir a recuperação correta.
                if (bitrateReduced) {
                    updateDynamicBitrate(prefs.bitrate);
                    currentDynamicBitrate = prefs.bitrate;
                    bitrateReduced = false;
                }
                consecutiveSimilarFrames = 0;
                consecutiveDissimilarFrames = 0;

                // If we just need a flush, do so now with all threads quiesced.
                if (codecRecoveryType.get() == CR_RECOVERY_TYPE_FLUSH) {
                    LimeLog.warning("Flushing decoder");
                    try {
                        videoDecoder.flush();
                        codecRecoveryType.set(CR_RECOVERY_TYPE_NONE);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();

                        // Something went wrong during the restart, let's use a bigger hammer
                        // and try a reset instead.
                        codecRecoveryType.set(CR_RECOVERY_TYPE_RESTART);
                    }
                }

                // We don't count flushes as codec recovery attempts
                if (codecRecoveryType.get() != CR_RECOVERY_TYPE_NONE) {
                    codecRecoveryAttempts++;
                    LimeLog.info("Codec recovery attempt: "+codecRecoveryAttempts);
                }

                // For "recoverable" exceptions, we can just stop, reconfigure, and restart.
                if (codecRecoveryType.get() == CR_RECOVERY_TYPE_RESTART) {
                    LimeLog.warning("Trying to restart decoder after CodecException");
                    try {
                        videoDecoder.stop();
                        configureAndStartDecoder(configuredFormat);
                        codecRecoveryType.set(CR_RECOVERY_TYPE_NONE);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();

                        // Our Surface is probably invalid, so just stop
                        stopping = true;
                        codecRecoveryType.set(CR_RECOVERY_TYPE_NONE);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();

                        // Something went wrong during the restart, let's use a bigger hammer
                        // and try a reset instead.
                        codecRecoveryType.set(CR_RECOVERY_TYPE_RESET);
                    }
                }

                // For "non-recoverable" exceptions on L+, we can call reset() to recover
                // without having to recreate the entire decoder again.
                if (codecRecoveryType.get() == CR_RECOVERY_TYPE_RESET && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    LimeLog.warning("Trying to reset decoder after CodecException");
                    try {
                        videoDecoder.reset();
                        configureAndStartDecoder(configuredFormat);
                        codecRecoveryType.set(CR_RECOVERY_TYPE_NONE);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();

                        // Our Surface is probably invalid, so just stop
                        stopping = true;
                        codecRecoveryType.set(CR_RECOVERY_TYPE_NONE);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();

                        // Something went wrong during the reset, we'll have to resort to
                        // releasing and recreating the decoder now.
                    }
                }

                // If we _still_ haven't managed to recover, go for the nuclear option and just
                // throw away the old decoder and reinitialize a new one from scratch.
                if (codecRecoveryType.get() == CR_RECOVERY_TYPE_RESET) {
                    LimeLog.warning("Trying to recreate decoder after CodecException");
                    videoDecoder.release();

                    try {
                        int err = initializeDecoder(true);
                        if (err != 0) {
                            throw new IllegalStateException("Decoder reset failed: " + err);
                        }
                        codecRecoveryType.set(CR_RECOVERY_TYPE_NONE);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();

                        // Our Surface is probably invalid, so just stop
                        stopping = true;
                        codecRecoveryType.set(CR_RECOVERY_TYPE_NONE);
                    } catch (IllegalStateException e) {
                        // If we failed to recover after all of these attempts, just crash
                        if (!reportedCrash) {
                            reportedCrash = true;
                            crashListener.notifyCrash(e);
                        }
                        throw new RendererException(this, e);
                    }
                }

                // Wake all quiesced threads and allow them to begin work again
                codecRecoveryThreadQuiescedFlags = 0;
                codecRecoveryMonitor.notifyAll();
            }
            else {
                // If we haven't quiesced all threads yet, wait to be signalled after recovery.
                // The final thread to be quiesced will handle the codec recovery.
                while (codecRecoveryType.get() != CR_RECOVERY_TYPE_NONE) {
                    try {
                        LimeLog.info("Waiting to quiesce decoder threads: "+codecRecoveryThreadQuiescedFlags);
                        codecRecoveryMonitor.wait(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();

                        // InterruptedException clears the thread's interrupt status. Since we can't
                        // handle that here, we will re-interrupt the thread to set the interrupt
                        // status back to true.
                        Thread.currentThread().interrupt();

                        break;
                    }
                }
            }
        }

        return true;
    }

    // Returns true if the exception is transient
    private boolean handleDecoderException(IllegalStateException e) {
        // Eat decoder exceptions if we're in the process of stopping
        if (stopping) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && e instanceof CodecException) {
            CodecException codecExc = (CodecException) e;

            if (codecExc.isTransient()) {
                // We'll let transient exceptions go
                LimeLog.warning(codecExc.getDiagnosticInfo());
                return true;
            }

            LimeLog.severe(codecExc.getDiagnosticInfo());

            // We can attempt a recovery or reset at this stage to try to start decoding again
            if (codecRecoveryAttempts < CR_MAX_TRIES) {
                // If the exception is non-recoverable or we already require a reset, perform a reset.
                // If we have no prior unrecoverable failure, we will try a restart instead.
                if (codecExc.isRecoverable()) {
                    if (codecRecoveryType.compareAndSet(CR_RECOVERY_TYPE_NONE, CR_RECOVERY_TYPE_RESTART)) {
                        LimeLog.info("Decoder requires restart for recoverable CodecException");
                        e.printStackTrace();
                    }
                    else if (codecRecoveryType.compareAndSet(CR_RECOVERY_TYPE_FLUSH, CR_RECOVERY_TYPE_RESTART)) {
                        LimeLog.info("Decoder flush promoted to restart for recoverable CodecException");
                        e.printStackTrace();
                    }
                    else if (codecRecoveryType.get() != CR_RECOVERY_TYPE_RESET && codecRecoveryType.get() != CR_RECOVERY_TYPE_RESTART) {
                        throw new IllegalStateException("Unexpected codec recovery type: " + codecRecoveryType.get());
                    }
                }
                else if (!codecExc.isRecoverable()) {
                    if (codecRecoveryType.compareAndSet(CR_RECOVERY_TYPE_NONE, CR_RECOVERY_TYPE_RESET)) {
                        LimeLog.info("Decoder requires reset for non-recoverable CodecException");
                        e.printStackTrace();
                    }
                    else if (codecRecoveryType.compareAndSet(CR_RECOVERY_TYPE_FLUSH, CR_RECOVERY_TYPE_RESET)) {
                        LimeLog.info("Decoder flush promoted to reset for non-recoverable CodecException");
                        e.printStackTrace();
                    }
                    else if (codecRecoveryType.compareAndSet(CR_RECOVERY_TYPE_RESTART, CR_RECOVERY_TYPE_RESET)) {
                        LimeLog.info("Decoder restart promoted to reset for non-recoverable CodecException");
                        e.printStackTrace();
                    }
                    else if (codecRecoveryType.get() != CR_RECOVERY_TYPE_RESET) {
                        throw new IllegalStateException("Unexpected codec recovery type: " + codecRecoveryType.get());
                    }
                }

                // The recovery will take place when all threads reach doCodecRecoveryIfRequired().
                return false;
            }
        }
        else {
            // IllegalStateException was primarily used prior to the introduction of CodecException.
            // Recovery from this requires a full decoder reset.
            //
            // NB: CodecException is an IllegalStateException, so we must check for it first.
            if (codecRecoveryAttempts < CR_MAX_TRIES) {
                if (codecRecoveryType.compareAndSet(CR_RECOVERY_TYPE_NONE, CR_RECOVERY_TYPE_RESET)) {
                    LimeLog.info("Decoder requires reset for IllegalStateException");
                    e.printStackTrace();
                }
                else if (codecRecoveryType.compareAndSet(CR_RECOVERY_TYPE_FLUSH, CR_RECOVERY_TYPE_RESET)) {
                    LimeLog.info("Decoder flush promoted to reset for IllegalStateException");
                    e.printStackTrace();
                }
                else if (codecRecoveryType.compareAndSet(CR_RECOVERY_TYPE_RESTART, CR_RECOVERY_TYPE_RESET)) {
                    LimeLog.info("Decoder restart promoted to reset for IllegalStateException");
                    e.printStackTrace();
                }
                else if (codecRecoveryType.get() != CR_RECOVERY_TYPE_RESET) {
                    throw new IllegalStateException("Unexpected codec recovery type: " + codecRecoveryType.get());
                }

                return false;
            }
        }

        // Only throw if we're not in the middle of codec recovery
        if (codecRecoveryType.get() == CR_RECOVERY_TYPE_NONE) {
            //
            // There seems to be a race condition with decoder/surface teardown causing some
            // decoders to to throw IllegalStateExceptions even before 'stopping' is set.
            // To workaround this while allowing real exceptions to propagate, we will eat the
            // first exception. If we are still receiving exceptions 3 seconds later, we will
            // throw the original exception again.
            //
            if (initialException != null) {
                // This isn't the first time we've had an exception processing video
                if (SystemClock.uptimeMillis() - initialExceptionTimestamp >= EXCEPTION_REPORT_DELAY_MS) {
                    // It's been over 3 seconds and we're still getting exceptions. Throw the original now.
                    if (!reportedCrash) {
                        reportedCrash = true;
                        crashListener.notifyCrash(initialException);
                    }
                    throw initialException;
                }
            }
            else {
                // This is the first exception we've hit
                initialException = new RendererException(this, e);
                initialExceptionTimestamp = SystemClock.uptimeMillis();
            }
        }

        // Not transient
        return false;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        // Do nothing if we're stopping
        if (stopping) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            frameTimeNanos -= activity.getWindowManager().getDefaultDisplay().getAppVsyncOffsetNanos();
        }

        // Don't render unless a new frame is due. This prevents microstutter when streaming
        // at a frame rate that doesn't match the display (such as 60 FPS on 120 Hz).
        long actualFrameTimeDeltaNs = frameTimeNanos - lastRenderedFrameTimeNanos;
        long expectedFrameTimeDeltaNs = 800000000 / refreshRate; // within 80% of the next frame
        if (actualFrameTimeDeltaNs >= expectedFrameTimeDeltaNs) {
            // Render up to one frame when in frame pacing mode.
            //
            // NB: Since the queue limit is 2, we won't starve the decoder of output buffers
            // by holding onto them for too long. This also ensures we will have that 1 extra
            // frame of buffer to smooth over network/rendering jitter.
            OutputFrame nextOutputFrame = outputBufferQueue.poll();
            if (nextOutputFrame != null) {
                try {
                    if (shouldDropOutputFrame(nextOutputFrame.presentationTimeUs)) {
                        videoDecoder.releaseOutputBuffer(nextOutputFrame.index, false);
                    }
                    else {
                        long renderTimeNanos = getSmoothedRenderTimeNanos(frameTimeNanos);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            videoDecoder.releaseOutputBuffer(nextOutputFrame.index, renderTimeNanos);
                        }
                        else {
                            videoDecoder.releaseOutputBuffer(nextOutputFrame.index, true);
                        }

                        lastRenderedFrameTimeNanos = renderTimeNanos;
                        activeWindowVideoStats.totalFramesRendered++;
                    }
                } catch (IllegalStateException ignored) {
                    try {
                        // Try to avoid leaking the output buffer by releasing it without rendering
                        videoDecoder.releaseOutputBuffer(nextOutputFrame.index, false);
                    } catch (IllegalStateException e) {
                        // This will leak nextOutputBuffer, but there's really nothing else we can do
                        e.printStackTrace();
                        handleDecoderException(e);
                    }
                }
            }
        }

        // Attempt codec recovery even if we have nothing to render right now. Recovery can still
        // be required even if the codec died before giving any output.
        doCodecRecoveryIfRequired(CR_FLAG_CHOREOGRAPHER);

        // Request another callback for next frame
        Choreographer.getInstance().postFrameCallback(this);
    }

    private void startChoreographerThread() {
        if (prefs.framePacing != PreferenceConfiguration.FRAME_PACING_BALANCED) {
            // Not using Choreographer in this pacing mode
            return;
        }

        // We use a separate thread to avoid any main thread delays from delaying rendering
        choreographerHandlerThread = new HandlerThread("Video - Choreographer", Process.THREAD_PRIORITY_DEFAULT + Process.THREAD_PRIORITY_MORE_FAVORABLE);
        choreographerHandlerThread.start();

        // Start the frame callbacks
        choreographerHandler = new Handler(choreographerHandlerThread.getLooper());
        choreographerHandler.post(new Runnable() {
            @Override
            public void run() {
                Choreographer.getInstance().postFrameCallback(MediaCodecDecoderRenderer.this);
            }
        });
    }

    private void startRendererThread()
    {
        rendererThread = new Thread() {
            @Override
            public void run() {
                BufferInfo info = new BufferInfo();
                while (!stopping) {
                    try {
                        // Try to output a frame
                        int outIndex = videoDecoder.dequeueOutputBuffer(info, 50000);
                        if (outIndex >= 0) {
                            long presentationTimeUs = info.presentationTimeUs;
                            int lastIndex = outIndex;

                            numFramesOut++;

                            // Render the latest frame now if frame pacing isn't in balanced mode
                            if (prefs.framePacing != PreferenceConfiguration.FRAME_PACING_BALANCED) {
                                // Get the last output buffer in the queue
                                while ((outIndex = videoDecoder.dequeueOutputBuffer(info, 0)) >= 0) {
                                    videoDecoder.releaseOutputBuffer(lastIndex, false);

                                    numFramesOut++;

                                    lastIndex = outIndex;
                                    presentationTimeUs = info.presentationTimeUs;
                                }

                                boolean dropOutputFrame = shouldDropOutputFrame(presentationTimeUs);
                                if (dropOutputFrame) {
                                    videoDecoder.releaseOutputBuffer(lastIndex, false);
                                }
                                else if (prefs.framePacing == PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS ||
                                        prefs.framePacing == PreferenceConfiguration.FRAME_PACING_CAP_FPS) {
                                    // In max smoothness or cap FPS mode, we want to never drop frames
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        // Use a PTS that will cause this frame to never be dropped
                                        videoDecoder.releaseOutputBuffer(lastIndex, 0);
                                    }
                                    else {
                                        videoDecoder.releaseOutputBuffer(lastIndex, true);
                                    }
                                }
                                else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        // Use a PTS that will cause this frame to be dropped if another comes in within
                                        // the same V-sync period
                                        videoDecoder.releaseOutputBuffer(lastIndex, getSmoothedRenderTimeNanos(System.nanoTime()));
                                    }
                                    else {
                                        videoDecoder.releaseOutputBuffer(lastIndex, true);
                                    }
                                }

                                if (!dropOutputFrame) {
                                    activeWindowVideoStats.totalFramesRendered++;
                                }
                            }
                            else {
                                // For balanced frame pacing case, the Choreographer callback will handle rendering.
                                // We just put all frames into the output buffer queue and let it handle things.

                                // Discard the oldest buffer if we've exceeded our limit.
                                //
                                // NB: We have to do this on the producer side because the consumer may not
                                // run for a while (if there is a huge mismatch between stream FPS and display
                                // refresh rate).
                                if (outputBufferQueue.size() == localSmoothingQueueLimit) {
                                    try {
                                        OutputFrame droppedFrame = outputBufferQueue.take();
                                        videoDecoder.releaseOutputBuffer(droppedFrame.index, false);
                                        if (prefs.preferAudioOverVideo) {
                                            activeWindowVideoStats.framesDroppedForAudioContinuity++;
                                        }
                                    } catch (InterruptedException e) {
                                        // We're shutting down, so we can just drop this buffer on the floor
                                        // and it will be reclaimed when the codec is released.
                                        return;
                                    }
                                }

                                // Add this buffer
                                outputBufferQueue.add(new OutputFrame(lastIndex, presentationTimeUs));
                            }

                            // Add delta time to the totals (excluding probable outliers)
                            long delta = SystemClock.uptimeMillis() - (presentationTimeUs / 1000);
                            if (delta >= 0 && delta < 1000) {
                                activeWindowVideoStats.decoderTimeMs += delta;
                                if (!USE_FRAME_RENDER_TIME) {
                                    activeWindowVideoStats.totalTimeMs += delta;
                                }
                            }
                        } else {
                            switch (outIndex) {
                                case MediaCodec.INFO_TRY_AGAIN_LATER:
                                    break;
                                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                    LimeLog.info("Output format changed");
                                    outputFormat = videoDecoder.getOutputFormat();
                                    LimeLog.info("New output format: " + outputFormat);
                                    break;
                                default:
                                    break;
                            }
                        }
                    } catch (IllegalStateException e) {
                        handleDecoderException(e);
                    } finally {
                        doCodecRecoveryIfRequired(CR_FLAG_RENDER_THREAD);
                    }
                }
            }
        };
        rendererThread.setName("Video - Renderer (MediaCodec)");
        rendererThread.setPriority(Thread.NORM_PRIORITY + 2);
        rendererThread.start();
    }

    private boolean fetchNextInputBuffer() {
        long startTime;
        boolean codecRecovered;

        if (nextInputBuffer != null) {
            // We already have an input buffer
            return true;
        }

        startTime = SystemClock.uptimeMillis();

        try {
            // If we don't have an input buffer index yet, fetch one now
            while (nextInputBufferIndex < 0 && !stopping) {
                nextInputBufferIndex = videoDecoder.dequeueInputBuffer(10000);
            }

            // Get the backing ByteBuffer for the input buffer index
            if (nextInputBufferIndex >= 0) {
                // Using the new getInputBuffer() API on Lollipop allows
                // the framework to do some performance optimizations for us
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    nextInputBuffer = videoDecoder.getInputBuffer(nextInputBufferIndex);
                    if (nextInputBuffer == null) {
                        // According to the Android docs, getInputBuffer() can return null "if the
                        // index is not a dequeued input buffer". I don't think this ever should
                        // happen but if it does, let's try to get a new input buffer next time.
                        nextInputBufferIndex = -1;
                    }
                }
                else {
                    nextInputBuffer = legacyInputBuffers[nextInputBufferIndex];

                    // Clear old input data pre-Lollipop
                    nextInputBuffer.clear();
                }
            }
        } catch (IllegalStateException e) {
            handleDecoderException(e);
            return false;
        } finally {
            codecRecovered = doCodecRecoveryIfRequired(CR_FLAG_INPUT_THREAD);
        }

        // If codec recovery is required, always return false to ensure the caller will request
        // an IDR frame to complete the codec recovery.
        if (codecRecovered) {
            return false;
        }

        int deltaMs = (int)(SystemClock.uptimeMillis() - startTime);

        if (deltaMs >= 20) {
            LimeLog.warning("Dequeue input buffer ran long: " + deltaMs + " ms");
        }

        if (nextInputBuffer == null) {
            // We've been hung for 5 seconds and no other exception was reported,
            // so generate a decoder hung exception
            if (deltaMs >= 5000 && initialException == null) {
                DecoderHungException decoderHungException = new DecoderHungException(deltaMs);
                if (!reportedCrash) {
                    reportedCrash = true;
                    crashListener.notifyCrash(decoderHungException);
                }
                throw new RendererException(this, decoderHungException);
            }

            return false;
        }

        return true;
    }

    @Override
    public void start() {
        startRendererThread();
        startChoreographerThread();
    }

    // !!! May be called even if setup()/start() fails !!!
    public void prepareForStop() {
        // Let the decoding code know to ignore codec exceptions now
        stopping = true;

        // Halt the rendering thread
        if (rendererThread != null) {
            rendererThread.interrupt();
        }

        // Stop any active codec recovery operations
        synchronized (codecRecoveryMonitor) {
            codecRecoveryType.set(CR_RECOVERY_TYPE_NONE);
            codecRecoveryMonitor.notifyAll();
        }

        // Post a quit message to the Choreographer looper (if we have one)
        if (choreographerHandler != null) {
            choreographerHandler.post(new Runnable() {
                @Override
                public void run() {
                    // Don't allow any further messages to be queued
                    choreographerHandlerThread.quit();

                    // Deregister the frame callback (if registered)
                    Choreographer.getInstance().removeFrameCallback(MediaCodecDecoderRenderer.this);
                }
            });
        }
    }

    @Override
    public void stop() {
        // May be called already, but we'll call it now to be safe
        prepareForStop();

        // Wait for the Choreographer looper to shut down (if we have one)
        if (choreographerHandlerThread != null) {
            try {
                choreographerHandlerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();

                // InterruptedException clears the thread's interrupt status. Since we can't
                // handle that here, we will re-interrupt the thread to set the interrupt
                // status back to true.
                Thread.currentThread().interrupt();
            }
        }

        // Wait for the renderer thread to shut down
        try {
            rendererThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();

            // InterruptedException clears the thread's interrupt status. Since we can't
            // handle that here, we will re-interrupt the thread to set the interrupt
            // status back to true.
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void cleanup() {
        videoDecoder.release();
    }

    @Override
    public void setHdrMode(boolean enabled, byte[] hdrMetadata) {
        // HDR metadata is only supported in Android 7.0 and later, so don't bother
        // restarting the codec on anything earlier than that.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (currentHdrMetadata != null && (!enabled || hdrMetadata == null)) {
                currentHdrMetadata = null;
            }
            else if (enabled && hdrMetadata != null && !Arrays.equals(currentHdrMetadata, hdrMetadata)) {
                currentHdrMetadata = hdrMetadata;
            }
            else {
                // Nothing to do
                return;
            }

            // If we reach this point, we need to restart the MediaCodec instance to
            // pick up the HDR metadata change. This will happen on the next input
            // or output buffer.

            // HACK: Reset codec recovery attempt counter, since this is an expected "recovery"
            codecRecoveryAttempts = 0;

            // Promote None/Flush to Restart and leave Reset alone
            if (!codecRecoveryType.compareAndSet(CR_RECOVERY_TYPE_NONE, CR_RECOVERY_TYPE_RESTART)) {
                codecRecoveryType.compareAndSet(CR_RECOVERY_TYPE_FLUSH, CR_RECOVERY_TYPE_RESTART);
            }
        }
    }

    private boolean queueNextInputBuffer(long timestampUs, int codecFlags) {
        boolean codecRecovered;

        try {
            videoDecoder.queueInputBuffer(nextInputBufferIndex,
                    0, nextInputBuffer.position(),
                    timestampUs, codecFlags);

            // We need a new buffer now
            nextInputBufferIndex = -1;
            nextInputBuffer = null;
        } catch (IllegalStateException e) {
            if (handleDecoderException(e)) {
                // We encountered a transient error. In this case, just hold onto the buffer
                // (to avoid leaking it), clear it, and keep it for the next frame. We'll return
                // false to trigger an IDR frame to recover.
                nextInputBuffer.clear();
            }
            else {
                // We encountered a non-transient error. In this case, we will simply leak the
                // buffer because we cannot be sure we will ever succeed in queuing it.
                nextInputBufferIndex = -1;
                nextInputBuffer = null;
            }
            return false;
        } finally {
            codecRecovered = doCodecRecoveryIfRequired(CR_FLAG_INPUT_THREAD);
        }

        // If codec recovery is required, always return false to ensure the caller will request
        // an IDR frame to complete the codec recovery.
        if (codecRecovered) {
            return false;
        }

        // Fetch a new input buffer now while we have some time between frames
        // to have it ready immediately when the next frame arrives.
        //
        // We must propagate the return value here in order to properly handle
        // codec recovery happening in fetchNextInputBuffer(). If we don't, we'll
        // never get an IDR frame to complete the recovery process.
        return fetchNextInputBuffer();
    }

    private void doProfileSpecificSpsPatching(SeqParameterSet sps) {
        // Some devices benefit from setting constraint flags 4 & 5 to make this Constrained
        // High Profile which allows the decoder to assume there will be no B-frames and
        // reduce delay and buffering accordingly. Some devices (Marvell, Exynos 4) don't
        // like it so we only set them on devices that are confirmed to benefit from it.
        if (sps.profileIdc == 100 && constrainedHighProfile) {
            LimeLog.info("Setting constraint set flags for constrained high profile");
            sps.constraintSet4Flag = true;
            sps.constraintSet5Flag = true;
        }
        else {
            // Force the constraints unset otherwise (some may be set by default)
            sps.constraintSet4Flag = false;
            sps.constraintSet5Flag = false;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public int submitDecodeUnit(byte[] decodeUnitData, int decodeUnitLength, int decodeUnitType,
                                int frameNumber, int frameType, char frameHostProcessingLatency,
                                long receiveTimeMs, long enqueueTimeMs) {
        if (stopping) {
            // Don't bother if we're stopping
            return MoonBridge.DR_OK;
        }
        
        // Jump-frame: agenda drop de frames P na SAÍDA do decoder via pendingDeduplicationDrops.
        // O frame continua sendo decodificado normalmente (a cadeia de referência H.264/HEVC
        // permanece intacta), mas shouldDropOutputFrame() suprime a apresentação na tela.
        // Isso economiza GPU/display sem corromper o decoder.
        if (jumpFrameMode != StreamConfiguration.JUMPFRAME_MODE_OFF) {
            jumpFrameCounter++;

            int framesToSkip = 0;
            switch (jumpFrameMode) {
                case StreamConfiguration.JUMPFRAME_MODE_LIGHT:
                    framesToSkip = 1;
                    break;
                case StreamConfiguration.JUMPFRAME_MODE_MEDIUM:
                    framesToSkip = 2;
                    break;
                case StreamConfiguration.JUMPFRAME_MODE_HEAVY:
                    framesToSkip = 3;
                    break;
            }

            // Drop na SAÍDA do decoder (shouldDropOutputFrame), não na entrada.
            // Frames P já decodificados podem ser descartados com segurança —
            // a cadeia de referência do decoder permanece intacta porque o frame
            // ainda foi decodificado internamente; só a apresentação é suprimida.
            if (frameType != MoonBridge.FRAME_TYPE_IDR &&
                jumpFrameCounter <= framesToSkip &&
                jumpFrameCounter < JUMPFRAME_COUNTER_INTERVAL) {
                pendingDeduplicationDrops.getAndUpdate(v -> Math.min(v + 1, 3));
                activeWindowVideoStats.framesDroppedByJumpFrame++;
            }

            if (jumpFrameCounter >= JUMPFRAME_COUNTER_INTERVAL) {
                jumpFrameCounter = 0;
            }
        }

        if (lastFrameNumber == 0) {
            activeWindowVideoStats.measurementStartTimestamp = SystemClock.uptimeMillis();
        } else if (frameNumber != lastFrameNumber && frameNumber != lastFrameNumber + 1) {
            // We can receive the same "frame" multiple times if it's an IDR frame.
            // In that case, each frame start NALU is submitted independently.
            activeWindowVideoStats.framesLost += frameNumber - lastFrameNumber - 1;
            activeWindowVideoStats.totalFrames += frameNumber - lastFrameNumber - 1;
            activeWindowVideoStats.frameLossEvents++;
        }

        // Reset CSD data for each IDR frame
        if (lastFrameNumber != frameNumber && frameType == MoonBridge.FRAME_TYPE_IDR) {
            vpsBuffers.clear();
            spsBuffers.clear();
            ppsBuffers.clear();
        }

        lastFrameNumber = frameNumber;

        // Flip stats windows roughly every second
        if (SystemClock.uptimeMillis() >= activeWindowVideoStats.measurementStartTimestamp + 1000) {
            if (prefs.enablePerfOverlay && !prefs.mobileNetworkOptimizations) {
                VideoStats lastTwo = new VideoStats();
                lastTwo.add(lastWindowVideoStats);
                lastTwo.add(activeWindowVideoStats);
                VideoStatsFps fps = lastTwo.getFps();
                String decoder;

                if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_H264) != 0) {
                    decoder = avcDecoder.getName();
                } else if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_H265) != 0) {
                    decoder = hevcDecoder.getName();
                } else if ((videoFormat & MoonBridge.VIDEO_FORMAT_MASK_AV1) != 0) {
                    decoder = av1Decoder.getName();
                } else {
                    decoder = "(unknown)";
                }

                float decodeTimeMs = (float)lastTwo.decoderTimeMs / lastTwo.totalFramesReceived;
                long rttInfo = MoonBridge.getEstimatedRttInfo();
                StringBuilder sb = new StringBuilder();
                sb.append(context.getString(R.string.perf_overlay_streamdetails, initialWidth + "x" + initialHeight, fps.totalFps)).append('\n');
                sb.append(context.getString(R.string.perf_overlay_decoder, decoder)).append('\n');
                sb.append(context.getString(R.string.perf_overlay_incomingfps, fps.receivedFps)).append('\n');
                sb.append(context.getString(R.string.perf_overlay_renderingfps, fps.renderedFps)).append('\n');
                sb.append(context.getString(R.string.perf_overlay_netdrops,
                        (float)lastTwo.framesLost / lastTwo.totalFrames * 100)).append('\n');
                sb.append(context.getString(R.string.perf_overlay_netlatency,
                        (int)(rttInfo >> 32), (int)rttInfo)).append('\n');
                if (lastTwo.framesWithHostProcessingLatency > 0) {
                    sb.append(context.getString(R.string.perf_overlay_hostprocessinglatency,
                            (float)lastTwo.minHostProcessingLatency / 10,
                            (float)lastTwo.maxHostProcessingLatency / 10,
                            (float)lastTwo.totalHostProcessingLatency / 10 / lastTwo.framesWithHostProcessingLatency)).append('\n');
                }
                sb.append(context.getString(R.string.perf_overlay_dectime, decodeTimeMs));

                long nowMs = SystemClock.uptimeMillis();
                if (nowMs - lastPerfOverlayUpdateMs >= PERF_OVERLAY_UPDATE_INTERVAL_MS) {
                    lastPerfOverlayUpdateMs = nowMs;
                    perfListener.onPerfUpdate(sb.toString());
                }
            }

            globalVideoStats.add(activeWindowVideoStats);
            lastWindowVideoStats.copy(activeWindowVideoStats);
            activeWindowVideoStats.clear();
            activeWindowVideoStats.measurementStartTimestamp = SystemClock.uptimeMillis();
        }

        boolean csdSubmittedForThisFrame = false;

        // IDR frames require special handling for CSD buffer submission
        if (frameType == MoonBridge.FRAME_TYPE_IDR) {
            // H264 SPS
            if (decodeUnitType == MoonBridge.BUFFER_TYPE_SPS && (videoFormat & MoonBridge.VIDEO_FORMAT_MASK_H264) != 0) {
                numSpsIn++;

                ByteBuffer spsBuf = ByteBuffer.wrap(decodeUnitData);
                int startSeqLen = decodeUnitData[2] == 0x01 ? 3 : 4;

                // Skip to the start of the NALU data
                spsBuf.position(startSeqLen + 1);

                // The H264Utils.readSPS function safely handles
                // Annex B NALUs (including NALUs with escape sequences)
                SeqParameterSet sps = H264Utils.readSPS(spsBuf);

                // Some decoders rely on H264 level to decide how many buffers are needed
                // Since we only need one frame buffered, we'll set the level as low as we can
                // for known resolution combinations. Reference frame invalidation may need
                // these, so leave them be for those decoders.
                if (!refFrameInvalidationActive) {
                    if (initialWidth <= 720 && initialHeight <= 480 && refreshRate <= 60) {
                        // Max 5 buffered frames at 720x480x60
                        LimeLog.info("Patching level_idc to 31");
                        sps.levelIdc = 31;
                    }
                    else if (initialWidth <= 1280 && initialHeight <= 720 && refreshRate <= 60) {
                        // Max 5 buffered frames at 1280x720x60
                        LimeLog.info("Patching level_idc to 32");
                        sps.levelIdc = 32;
                    }
                    else if (initialWidth <= 1920 && initialHeight <= 1080 && refreshRate <= 60) {
                        // Max 4 buffered frames at 1920x1080x64
                        LimeLog.info("Patching level_idc to 42");
                        sps.levelIdc = 42;
                    }
                    else {
                        // Leave the profile alone (currently 5.0)
                    }
                }

                // TI OMAP4 requires a reference frame count of 1 to decode successfully. Exynos 4
                // also requires this fixup.
                //
                // I'm doing this fixup for all devices because I haven't seen any devices that
                // this causes issues for. At worst, it seems to do nothing and at best it fixes
                // issues with video lag, hangs, and crashes.
                //
                // It does break reference frame invalidation, so we will not do that for decoders
                // where we've enabled reference frame invalidation.
                if (!refFrameInvalidationActive) {
                    LimeLog.info("Patching num_ref_frames in SPS");
                    sps.numRefFrames = 1;
                }

                // GFE 2.5.11 changed the SPS to add additional extensions. Some devices don't like these
                // so we remove them here on old devices unless these devices also support HEVC.
                // See getPreferredColorSpace() for further information.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O &&
                        sps.vuiParams != null &&
                        hevcDecoder == null &&
                        av1Decoder == null) {
                    sps.vuiParams.videoSignalTypePresentFlag = false;
                    sps.vuiParams.colourDescriptionPresentFlag = false;
                    sps.vuiParams.chromaLocInfoPresentFlag = false;
                }

                // Some older devices used to choke on a bitstream restrictions, so we won't provide them
                // unless explicitly whitelisted. For newer devices, leave the bitstream restrictions present.
                if (needsSpsBitstreamFixup || isExynos4 || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // The SPS that comes in the current H264 bytestream doesn't set bitstream_restriction_flag
                    // or max_dec_frame_buffering which increases decoding latency on Tegra.

                    // If the encoder didn't include VUI parameters in the SPS, add them now
                    if (sps.vuiParams == null) {
                        LimeLog.info("Adding VUI parameters");
                        sps.vuiParams = new VUIParameters();
                    }

                    // GFE 2.5.11 started sending bitstream restrictions
                    if (sps.vuiParams.bitstreamRestriction == null) {
                        LimeLog.info("Adding bitstream restrictions");
                        sps.vuiParams.bitstreamRestriction = new VUIParameters.BitstreamRestriction();
                        sps.vuiParams.bitstreamRestriction.motionVectorsOverPicBoundariesFlag = true;
                        sps.vuiParams.bitstreamRestriction.maxBytesPerPicDenom = 2;
                        sps.vuiParams.bitstreamRestriction.maxBitsPerMbDenom = 1;
                        sps.vuiParams.bitstreamRestriction.log2MaxMvLengthHorizontal = 16;
                        sps.vuiParams.bitstreamRestriction.log2MaxMvLengthVertical = 16;
                        sps.vuiParams.bitstreamRestriction.numReorderFrames = 0;
                    }
                    else {
                        LimeLog.info("Patching bitstream restrictions");
                    }

                    // Some devices throw errors if maxDecFrameBuffering < numRefFrames
                    sps.vuiParams.bitstreamRestriction.maxDecFrameBuffering = sps.numRefFrames;

                    // These values are the defaults for the fields, but they are more aggressive
                    // than what GFE sends in 2.5.11, but it doesn't seem to cause picture problems.
                    // We'll leave these alone for "modern" devices just in case they care.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        sps.vuiParams.bitstreamRestriction.maxBytesPerPicDenom = 2;
                        sps.vuiParams.bitstreamRestriction.maxBitsPerMbDenom = 1;
                    }

                    // log2_max_mv_length_horizontal and log2_max_mv_length_vertical are set to more
                    // conservative values by GFE 2.5.11. We'll let those values stand.
                }
                else if (sps.vuiParams != null) {
                    // Devices that didn't/couldn't get bitstream restrictions before GFE 2.5.11
                    // will continue to not receive them now
                    sps.vuiParams.bitstreamRestriction = null;
                }

                // If we need to hack this SPS to say we're baseline, do so now
                if (needsBaselineSpsHack) {
                    LimeLog.info("Hacking SPS to baseline");
                    sps.profileIdc = 66;
                    savedSps = sps;
                }

                // Patch the SPS constraint flags
                doProfileSpecificSpsPatching(sps);

                // The H264Utils.writeSPS function safely handles
                // Annex B NALUs (including NALUs with escape sequences)
                ByteBuffer escapedNalu = H264Utils.writeSPS(sps, decodeUnitLength);

                // Construct the patched SPS
                byte[] naluBuffer = new byte[startSeqLen + 1 + escapedNalu.limit()];
                System.arraycopy(decodeUnitData, 0, naluBuffer, 0, startSeqLen + 1);
                escapedNalu.get(naluBuffer, startSeqLen + 1, escapedNalu.limit());

                // Batch this to submit together with other CSD per AOSP docs
                spsBuffers.add(naluBuffer);
                return MoonBridge.DR_OK;
            }
            else if (decodeUnitType == MoonBridge.BUFFER_TYPE_VPS) {
                numVpsIn++;

                // Batch this to submit together with other CSD per AOSP docs
                byte[] naluBuffer = new byte[decodeUnitLength];
                System.arraycopy(decodeUnitData, 0, naluBuffer, 0, decodeUnitLength);
                vpsBuffers.add(naluBuffer);
                return MoonBridge.DR_OK;
            }
            // Only the HEVC SPS hits this path (H.264 is handled above)
            else if (decodeUnitType == MoonBridge.BUFFER_TYPE_SPS) {
                numSpsIn++;

                // Batch this to submit together with other CSD per AOSP docs
                byte[] naluBuffer = new byte[decodeUnitLength];
                System.arraycopy(decodeUnitData, 0, naluBuffer, 0, decodeUnitLength);
                spsBuffers.add(naluBuffer);
                return MoonBridge.DR_OK;
            }
            else if (decodeUnitType == MoonBridge.BUFFER_TYPE_PPS) {
                numPpsIn++;

                // Batch this to submit together with other CSD per AOSP docs
                byte[] naluBuffer = new byte[decodeUnitLength];
                System.arraycopy(decodeUnitData, 0, naluBuffer, 0, decodeUnitLength);
                ppsBuffers.add(naluBuffer);
                return MoonBridge.DR_OK;
            }
            else if ((videoFormat & (MoonBridge.VIDEO_FORMAT_MASK_H264 | MoonBridge.VIDEO_FORMAT_MASK_H265)) != 0) {
                // If this is the first CSD blob or we aren't supporting fused IDR frames, we will
                // submit the CSD blob in a separate input buffer for each IDR frame.
                if (!submittedCsd || !fusedIdrFrame) {
                    if (!fetchNextInputBuffer()) {
                        return MoonBridge.DR_NEED_IDR;
                    }

                    // Submit all CSD when we receive the first non-CSD blob in an IDR frame
                    for (byte[] vpsBuffer : vpsBuffers) {
                        nextInputBuffer.put(vpsBuffer);
                    }
                    for (byte[] spsBuffer : spsBuffers) {
                        nextInputBuffer.put(spsBuffer);
                    }
                    for (byte[] ppsBuffer : ppsBuffers) {
                        nextInputBuffer.put(ppsBuffer);
                    }

                    if (!queueNextInputBuffer(0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG)) {
                        return MoonBridge.DR_NEED_IDR;
                    }

                    // Remember that we already submitted CSD for this frame, so we don't do it
                    // again in the fused IDR case below.
                    csdSubmittedForThisFrame = true;

                    // Remember that we submitted CSD globally for this MediaCodec instance
                    submittedCsd = true;

                    if (needsBaselineSpsHack) {
                        needsBaselineSpsHack = false;

                        if (!replaySps()) {
                            return MoonBridge.DR_NEED_IDR;
                        }

                        LimeLog.info("SPS replay complete");
                    }
                }
            }
        }

        if (frameHostProcessingLatency != 0) {
            if (activeWindowVideoStats.minHostProcessingLatency != 0) {
                activeWindowVideoStats.minHostProcessingLatency = (char) Math.min(activeWindowVideoStats.minHostProcessingLatency, frameHostProcessingLatency);
            } else {
                activeWindowVideoStats.minHostProcessingLatency = frameHostProcessingLatency;
            }
            activeWindowVideoStats.framesWithHostProcessingLatency += 1;
        }
        activeWindowVideoStats.maxHostProcessingLatency = (char) Math.max(activeWindowVideoStats.maxHostProcessingLatency, frameHostProcessingLatency);
        activeWindowVideoStats.totalHostProcessingLatency += frameHostProcessingLatency;

        activeWindowVideoStats.totalFramesReceived++;
        activeWindowVideoStats.totalFrames++;

        if (!FRAME_RENDER_TIME_ONLY) {
            // Count time from first packet received to enqueue time as receive time
            // We will count DU queue time as part of decoding, because it is directly
            // caused by a slow decoder.
            activeWindowVideoStats.totalTimeMs += enqueueTimeMs - receiveTimeMs;
        }

        if (!fetchNextInputBuffer()) {
            return MoonBridge.DR_NEED_IDR;
        }

        int codecFlags = 0;

        if (frameType == MoonBridge.FRAME_TYPE_IDR) {
            codecFlags |= MediaCodec.BUFFER_FLAG_SYNC_FRAME;

            // If we are using fused IDR frames, submit the CSD with each IDR frame
            if (fusedIdrFrame && !csdSubmittedForThisFrame) {
                for (byte[] vpsBuffer : vpsBuffers) {
                    nextInputBuffer.put(vpsBuffer);
                }
                for (byte[] spsBuffer : spsBuffers) {
                    nextInputBuffer.put(spsBuffer);
                }
                for (byte[] ppsBuffer : ppsBuffers) {
                    nextInputBuffer.put(ppsBuffer);
                }
            }
        }

        long timestampUs = enqueueTimeMs * 1000;
        if (timestampUs <= lastTimestampUs) {
            // We can't submit multiple buffers with the same timestamp
            // so bump it up by one before queuing
            timestampUs = lastTimestampUs + 1;
        }
        lastTimestampUs = timestampUs;

        numFramesIn++;

        if (decodeUnitLength > nextInputBuffer.limit() - nextInputBuffer.position()) {
            IllegalArgumentException exception = new IllegalArgumentException(
                    "Decode unit length "+decodeUnitLength+" too large for input buffer "+nextInputBuffer.limit());
            if (!reportedCrash) {
                reportedCrash = true;
                crashListener.notifyCrash(exception);
            }
            throw new RendererException(this, exception);
        }

        analyzeFrameForLocalOptimizations(decodeUnitData, decodeUnitLength, frameType);

        // FIX-4: Area deduplication — descarta o frame ANTES de copiar para o decoder.
        // Correção: o nextInputBuffer já foi obtido via fetchNextInputBuffer() acima.
        // Não podemos simplesmente zerar o índice sem devolver o buffer ao codec —
        // isso vazava o slot de input e quebrava o fluxo de pré-busca para o próximo
        // frame. Devolvemos o buffer via queueInputBuffer com tamanho 0 (sem dados,
        // sem flags), que é ignorado pelo decoder mas libera o slot corretamente.
        if (prefs.areaDeduplicationEnabled
                && pendingAreaReplacementFrames > 0
                && frameType != MoonBridge.FRAME_TYPE_IDR) {
            pendingAreaReplacementFrames--;
            activeWindowVideoStats.framesReplacedByAreaDeduplication++;
            try {
                // queueInputBuffer com size=0 devolve o slot sem submeter nenhum dado
                videoDecoder.queueInputBuffer(nextInputBufferIndex, 0, 0, timestampUs, 0);
            } catch (IllegalStateException e) {
                LimeLog.warning("Area dedup: failed to return input buffer during discard");
            }
            nextInputBufferIndex = -1;
            nextInputBuffer = null;
            // Pré-busca o próximo buffer para manter o fluxo (melhor esforço)
            fetchNextInputBuffer();
            return MoonBridge.DR_OK;
        }

        // Copy data from our buffer list into the input buffer
        nextInputBuffer.put(decodeUnitData, 0, decodeUnitLength);

        if (!queueNextInputBuffer(timestampUs, codecFlags)) {
            return MoonBridge.DR_NEED_IDR;
        }

        return MoonBridge.DR_OK;
    }

    private boolean replaySps() {
        if (!fetchNextInputBuffer()) {
            return false;
        }

        // Write the Annex B header
        nextInputBuffer.put(new byte[]{0x00, 0x00, 0x00, 0x01, 0x67});

        // Switch the H264 profile back to high
        savedSps.profileIdc = 100;

        // Patch the SPS constraint flags
        doProfileSpecificSpsPatching(savedSps);

        // The H264Utils.writeSPS function safely handles
        // Annex B NALUs (including NALUs with escape sequences)
        ByteBuffer escapedNalu = H264Utils.writeSPS(savedSps, 128);
        nextInputBuffer.put(escapedNalu);

        // No need for the SPS anymore
        savedSps = null;

        // Queue the new SPS
        return queueNextInputBuffer(0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
    }

    @Override
    public int getCapabilities() {
        int capabilities = 0;

        // Request the optimal number of slices per frame for this decoder
        capabilities |= MoonBridge.CAPABILITY_SLICES_PER_FRAME(optimalSlicesPerFrame);

        // Enable reference frame invalidation on supported hardware
        if (refFrameInvalidationAvc) {
            capabilities |= MoonBridge.CAPABILITY_REFERENCE_FRAME_INVALIDATION_AVC;
        }
        if (refFrameInvalidationHevc) {
            capabilities |= MoonBridge.CAPABILITY_REFERENCE_FRAME_INVALIDATION_HEVC;
        }
        if (refFrameInvalidationAv1) {
            capabilities |= MoonBridge.CAPABILITY_REFERENCE_FRAME_INVALIDATION_AV1;
        }

        // Enable direct submit on supported hardware
        if (directSubmit) {
            capabilities |= MoonBridge.CAPABILITY_DIRECT_SUBMIT;
        }

        return capabilities;
    }

    public int getAverageEndToEndLatency() {
        if (globalVideoStats.totalFramesReceived == 0) {
            return 0;
        }
        return (int)(globalVideoStats.totalTimeMs / globalVideoStats.totalFramesReceived);
    }

    public int getAverageDecoderLatency() {
        if (globalVideoStats.totalFramesReceived == 0) {
            return 0;
        }
        return (int)(globalVideoStats.decoderTimeMs / globalVideoStats.totalFramesReceived);
    }

    static class DecoderHungException extends RuntimeException {
        private int hangTimeMs;

        DecoderHungException(int hangTimeMs) {
            this.hangTimeMs = hangTimeMs;
        }

        public String toString() {
            String str = "";

            str += "Hang time: "+hangTimeMs+" ms"+ RendererException.DELIMITER;
            str += super.toString();

            return str;
        }
    }

    static class RendererException extends RuntimeException {
        private static final long serialVersionUID = 8985937536997012406L;
        protected static final String DELIMITER = BuildConfig.DEBUG ? "\n" : " | ";

        private String text;

        RendererException(MediaCodecDecoderRenderer renderer, Exception e) {
            this.text = generateText(renderer, e);
        }

        public String toString() {
            return text;
        }

        private String generateText(MediaCodecDecoderRenderer renderer, Exception originalException) {
            String str;

            if (renderer.numVpsIn == 0 && renderer.numSpsIn == 0 && renderer.numPpsIn == 0) {
                str = "PreSPSError";
            }
            else if (renderer.numSpsIn > 0 && renderer.numPpsIn == 0) {
                str = "PrePPSError";
            }
            else if (renderer.numPpsIn > 0 && renderer.numFramesIn == 0) {
                str = "PreIFrameError";
            }
            else if (renderer.numFramesIn > 0 && renderer.outputFormat == null) {
                str = "PreOutputConfigError";
            }
            else if (renderer.outputFormat != null && renderer.numFramesOut == 0) {
                str = "PreOutputError";
            }
            else if (renderer.numFramesOut <= renderer.refreshRate * 30) {
                str = "EarlyOutputError";
            }
            else {
                str = "ErrorWhileStreaming";
            }

            str += "Format: "+String.format("%x", renderer.videoFormat)+DELIMITER;
            str += "AVC Decoder: "+((renderer.avcDecoder != null) ? renderer.avcDecoder.getName():"(none)")+DELIMITER;
            str += "HEVC Decoder: "+((renderer.hevcDecoder != null) ? renderer.hevcDecoder.getName():"(none)")+DELIMITER;
            str += "AV1 Decoder: "+((renderer.av1Decoder != null) ? renderer.av1Decoder.getName():"(none)")+DELIMITER;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && renderer.avcDecoder != null) {
                Range<Integer> avcWidthRange = renderer.avcDecoder.getCapabilitiesForType("video/avc").getVideoCapabilities().getSupportedWidths();
                str += "AVC supported width range: "+avcWidthRange+DELIMITER;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        Range<Double> avcFpsRange = renderer.avcDecoder.getCapabilitiesForType("video/avc").getVideoCapabilities().getAchievableFrameRatesFor(renderer.initialWidth, renderer.initialHeight);
                        str += "AVC achievable FPS range: "+avcFpsRange+DELIMITER;
                    } catch (IllegalArgumentException e) {
                        str += "AVC achievable FPS range: UNSUPPORTED!"+DELIMITER;
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && renderer.hevcDecoder != null) {
                Range<Integer> hevcWidthRange = renderer.hevcDecoder.getCapabilitiesForType("video/hevc").getVideoCapabilities().getSupportedWidths();
                str += "HEVC supported width range: "+hevcWidthRange+DELIMITER;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        Range<Double> hevcFpsRange = renderer.hevcDecoder.getCapabilitiesForType("video/hevc").getVideoCapabilities().getAchievableFrameRatesFor(renderer.initialWidth, renderer.initialHeight);
                        str += "HEVC achievable FPS range: " + hevcFpsRange + DELIMITER;
                    } catch (IllegalArgumentException e) {
                        str += "HEVC achievable FPS range: UNSUPPORTED!"+DELIMITER;
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && renderer.av1Decoder != null) {
                Range<Integer> av1WidthRange = renderer.av1Decoder.getCapabilitiesForType("video/av01").getVideoCapabilities().getSupportedWidths();
                str += "AV1 supported width range: "+av1WidthRange+DELIMITER;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        Range<Double> av1FpsRange = renderer.av1Decoder.getCapabilitiesForType("video/av01").getVideoCapabilities().getAchievableFrameRatesFor(renderer.initialWidth, renderer.initialHeight);
                        str += "AV1 achievable FPS range: " + av1FpsRange + DELIMITER;
                    } catch (IllegalArgumentException e) {
                        str += "AV1 achievable FPS range: UNSUPPORTED!"+DELIMITER;
                    }
                }
            }
            str += "Configured format: "+renderer.configuredFormat+DELIMITER;
            str += "Input format: "+renderer.inputFormat+DELIMITER;
            str += "Output format: "+renderer.outputFormat+DELIMITER;
            str += "Adaptive playback: "+renderer.adaptivePlayback+DELIMITER;
            str += "GL Renderer: "+renderer.glRenderer+DELIMITER;
            //str += "Build fingerprint: "+Build.FINGERPRINT+DELIMITER;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                str += "SOC: "+Build.SOC_MANUFACTURER+" - "+Build.SOC_MODEL+DELIMITER;
                str += "Performance class: "+Build.VERSION.MEDIA_PERFORMANCE_CLASS+DELIMITER;
                /*str += "Vendor params: ";
                List<String> params = renderer.videoDecoder.getSupportedVendorParameters();
                if (params.isEmpty()) {
                    str += "NONE";
                }
                else {
                    for (String param : params) {
                        str += param + " ";
                    }
                }
                str += DELIMITER;*/
            }
            str += "Consecutive crashes: "+renderer.consecutiveCrashCount+DELIMITER;
            str += "RFI active: "+renderer.refFrameInvalidationActive+DELIMITER;
            str += "Using modern SPS patching: "+(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)+DELIMITER;
            str += "Fused IDR frames: "+renderer.fusedIdrFrame+DELIMITER;
            str += "Video dimensions: "+renderer.initialWidth+"x"+renderer.initialHeight+DELIMITER;
            str += "FPS target: "+renderer.refreshRate+DELIMITER;
            str += "Bitrate: "+renderer.prefs.bitrate+" Kbps"+DELIMITER;
            str += "CSD stats: "+renderer.numVpsIn+", "+renderer.numSpsIn+", "+renderer.numPpsIn+DELIMITER;
            str += "Frames in-out: "+renderer.numFramesIn+", "+renderer.numFramesOut+DELIMITER;
            str += "Total frames received: "+renderer.globalVideoStats.totalFramesReceived+DELIMITER;
            str += "Total frames rendered: "+renderer.globalVideoStats.totalFramesRendered+DELIMITER;
            str += "Frame losses: "+renderer.globalVideoStats.framesLost+" in "+renderer.globalVideoStats.frameLossEvents+" loss events"+DELIMITER;
            str += "Average end-to-end client latency: "+renderer.getAverageEndToEndLatency()+"ms"+DELIMITER;
            str += "Average hardware decoder latency: "+renderer.getAverageDecoderLatency()+"ms"+DELIMITER;
            str += "Frame pacing mode: "+renderer.prefs.framePacing+DELIMITER;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (originalException instanceof CodecException) {
                    CodecException ce = (CodecException) originalException;

                    str += "Diagnostic Info: "+ce.getDiagnosticInfo()+DELIMITER;
                    str += "Recoverable: "+ce.isRecoverable()+DELIMITER;
                    str += "Transient: "+ce.isTransient()+DELIMITER;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        str += "Codec Error Code: "+ce.getErrorCode()+DELIMITER;
                    }
                }
            }

            str += originalException.toString();

            return str;
        }
    }
}