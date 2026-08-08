//main/java/com/limelight/preferences/PreferenceConfiguration.java
package com.limelight.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.Display;

import com.limelight.nvstream.jni.MoonBridge;

public class PreferenceConfiguration {
    public enum FormatOption {
        AUTO,
        FORCE_AV1,
        FORCE_HEVC,
        FORCE_H264,
    };

    public enum AnalogStickForScrolling {
        NONE,
        RIGHT,
        LEFT
    }

    private static final String LEGACY_RES_FPS_PREF_STRING = "list_resolution_fps";
    private static final String LEGACY_ENABLE_51_SURROUND_PREF_STRING = "checkbox_51_surround";

    static final String RESOLUTION_PREF_STRING = "list_resolution";
    static final String FPS_PREF_STRING = "list_fps";
    static final String BITRATE_PREF_STRING = "seekbar_bitrate_kbps";
    private static final String BITRATE_PREF_OLD_STRING = "seekbar_bitrate";
    private static final String STRETCH_PREF_STRING = "checkbox_stretch_video";
    private static final String SOPS_PREF_STRING = "checkbox_enable_sops";
    private static final String DISABLE_TOASTS_PREF_STRING = "checkbox_disable_warnings";
    private static final String HOST_AUDIO_PREF_STRING = "checkbox_host_audio";
    private static final String DEADZONE_PREF_STRING = "seekbar_deadzone";
    private static final String OSC_OPACITY_PREF_STRING = "seekbar_osc_opacity";
    private static final String LANGUAGE_PREF_STRING = "list_languages";
    private static final String SMALL_ICONS_PREF_STRING = "checkbox_small_icon_mode";
    private static final String MULTI_CONTROLLER_PREF_STRING = "checkbox_multi_controller";
    static final String AUDIO_CONFIG_PREF_STRING = "list_audio_config";
    private static final String USB_DRIVER_PREF_SRING = "checkbox_usb_driver";
    private static final String VIDEO_FORMAT_PREF_STRING = "video_format";
    private static final String ONSCREEN_CONTROLLER_PREF_STRING = "checkbox_show_onscreen_controls";
    private static final String ONLY_L3_R3_PREF_STRING = "checkbox_only_show_L3R3";
    private static final String SHOW_GUIDE_BUTTON_PREF_STRING = "checkbox_show_guide_button";
    private static final String LEGACY_DISABLE_FRAME_DROP_PREF_STRING = "checkbox_disable_frame_drop";
    private static final String ENABLE_HDR_PREF_STRING = "checkbox_enable_hdr";
    private static final String ENABLE_PIP_PREF_STRING = "checkbox_enable_pip";
    private static final String ENABLE_PERF_OVERLAY_STRING = "checkbox_enable_perf_overlay";
    private static final String BIND_ALL_USB_STRING = "checkbox_usb_bind_all";
    private static final String MOUSE_EMULATION_STRING = "checkbox_mouse_emulation";
    private static final String ANALOG_SCROLLING_PREF_STRING = "analog_scrolling";
    private static final String MOUSE_NAV_BUTTONS_STRING = "checkbox_mouse_nav_buttons";
    static final String UNLOCK_FPS_STRING = "checkbox_unlock_fps";
    private static final String VIBRATE_OSC_PREF_STRING = "checkbox_vibrate_osc";
    private static final String VIBRATE_FALLBACK_PREF_STRING = "checkbox_vibrate_fallback";
    private static final String VIBRATE_FALLBACK_STRENGTH_PREF_STRING = "seekbar_vibrate_fallback_strength";
    private static final String FLIP_FACE_BUTTONS_PREF_STRING = "checkbox_flip_face_buttons";
    private static final String TOUCHSCREEN_TRACKPAD_PREF_STRING = "checkbox_touchscreen_trackpad";
    private static final String LATENCY_TOAST_PREF_STRING = "checkbox_enable_post_stream_toast";
    private static final String FRAME_PACING_PREF_STRING = "frame_pacing";
    private static final String ABSOLUTE_MOUSE_MODE_PREF_STRING = "checkbox_absolute_mouse_mode";
    private static final String ENABLE_AUDIO_FX_PREF_STRING = "checkbox_enable_audiofx";
    private static final String REDUCE_REFRESH_RATE_PREF_STRING = "checkbox_reduce_refresh_rate";
    private static final String FULL_RANGE_PREF_STRING = "checkbox_full_range";
    private static final String GAMEPAD_TOUCHPAD_AS_MOUSE_PREF_STRING = "checkbox_gamepad_touchpad_as_mouse";
    private static final String GAMEPAD_MOTION_SENSORS_PREF_STRING = "checkbox_gamepad_motion_sensors";
    private static final String GAMEPAD_MOTION_FALLBACK_PREF_STRING = "checkbox_gamepad_motion_fallback";
    private static final String MOBILE_NETWORK_OPT_PREF_STRING = "checkbox_mobile_network_optimizations";
    private static final String JUMP_FRAME_MODE_PREF_STRING = "list_jump_frame_mode";
    private static final String TARGET_LATENCY_PREF_STRING = "seekbar_target_latency";
    private static final String ADAPTIVE_RESOLUTION_PREF_STRING = "checkbox_adaptive_resolution";
    private static final String BITRATE_OPTIMIZATION_PREF_STRING = "checkbox_bitrate_optimization";
    private static final String BITRATE_ANALYSIS_INTERVAL_PREF_STRING = "seekbar_bitrate_analysis_interval";
    private static final String FRAME_SIMILARITY_THRESHOLD_PREF_STRING = "seekbar_frame_similarity_threshold";
    private static final String LOCAL_UPSCALING_PREF_STRING = "checkbox_local_upscaling";
    private static final String LOCAL_UPSCALING_MODE_PREF_STRING = "list_local_upscaling_mode";
    private static final String PREFER_AUDIO_OVER_VIDEO_PREF_STRING = "checkbox_prefer_audio_over_video";
    private static final String TRANSITION_FRAME_MODE_PREF_STRING = "list_transition_frame_mode";
    // Tipo de interpolação entre frames (0=nenhuma, 1=linear, 2=ease-in-out, 3=cúbica, 4=exponencial, 5=smooth-step)
    private static final String TRANSITION_INTERPOLATION_TYPE_PREF_STRING = "list_transition_interpolation_type";
    // Força/intensidade da transição: 0–100 (%)
    private static final String TRANSITION_STRENGTH_PREF_STRING = "seekbar_transition_strength";
    // Frequência da transição em ms (intervalo mínimo entre frames interpolados)
    private static final String TRANSITION_FREQUENCY_MS_PREF_STRING = "seekbar_transition_frequency_ms";
    private static final String LOCAL_FRAME_DEDUPLICATION_PREF_STRING = "checkbox_local_frame_deduplication";
    private static final String LOCAL_MOTION_SMOOTHING_PREF_STRING = "checkbox_local_motion_smoothing";
    private static final String AUTO_RECONNECT_PREF_STRING = "checkbox_auto_reconnect";
    private static final String AUTO_RECONNECT_ATTEMPTS_PREF_STRING = "seekbar_auto_reconnect_attempts";
    private static final String BLOCK_COMPRESSION_PREF_STRING = "checkbox_block_compression";
    private static final String BLOCK_SIZE_PREF_STRING = "list_block_size";
    private static final String ADAPTIVE_SHARPNESS_PREF_STRING = "checkbox_adaptive_sharpness";
    private static final String HUD_DETECTION_PREF_STRING = "checkbox_hud_detection";
    private static final String HUD_RESOLUTION_REDUCTION_PREF_STRING = "seekbar_hud_resolution_reduction";

    // Deduplicação de áreas (config própria, separada do menu de filtros)
    private static final String AREA_DEDUPLICATION_PREF_STRING = "checkbox_area_deduplication";
    private static final String AREA_DEDUP_CHECK_INTERVAL_PREF_STRING = "seekbar_area_dedup_check_interval";
    private static final String AREA_DEDUP_LOOKBACK_FRAMES_PREF_STRING = "seekbar_area_dedup_lookback_frames";
    private static final String AREA_DEDUP_REPLACE_FRAMES_PREF_STRING = "seekbar_area_dedup_replace_frames";
    private static final String AREA_DEDUP_SIMILARITY_THRESHOLD_PREF_STRING = "seekbar_area_dedup_similarity_threshold";
    private static final String AREA_DEDUP_GRID_SIZE_PREF_STRING = "seekbar_area_dedup_grid_size";

    static final String DEFAULT_RESOLUTION = "1280x720";
    static final String DEFAULT_FPS = "60";
    private static final boolean DEFAULT_STRETCH = false;
    private static final boolean DEFAULT_SOPS = true;
    private static final boolean DEFAULT_DISABLE_TOASTS = false;
    private static final boolean DEFAULT_HOST_AUDIO = false;
    private static final int DEFAULT_DEADZONE = 7;
    private static final int DEFAULT_OPACITY = 90;
    public static final String DEFAULT_LANGUAGE = "default";
    private static final boolean DEFAULT_MULTI_CONTROLLER = true;
    private static final boolean DEFAULT_USB_DRIVER = true;
    private static final String DEFAULT_VIDEO_FORMAT = "auto";

    private static final boolean ONSCREEN_CONTROLLER_DEFAULT = false;
    private static final boolean ONLY_L3_R3_DEFAULT = false;
    private static final boolean SHOW_GUIDE_BUTTON_DEFAULT = true;
    private static final boolean DEFAULT_ENABLE_HDR = false;
    private static final boolean DEFAULT_ENABLE_PIP = false;
    private static final boolean DEFAULT_ENABLE_PERF_OVERLAY = false;
    private static final boolean DEFAULT_BIND_ALL_USB = false;
    private static final boolean DEFAULT_MOUSE_EMULATION = true;
    private static final String DEFAULT_ANALOG_STICK_FOR_SCROLLING = "right";
    private static final boolean DEFAULT_MOUSE_NAV_BUTTONS = false;
    private static final boolean DEFAULT_UNLOCK_FPS = false;
    private static final boolean DEFAULT_VIBRATE_OSC = true;
    private static final boolean DEFAULT_VIBRATE_FALLBACK = false;
    private static final int DEFAULT_VIBRATE_FALLBACK_STRENGTH = 100;
    private static final boolean DEFAULT_FLIP_FACE_BUTTONS = false;
    private static final boolean DEFAULT_TOUCHSCREEN_TRACKPAD = true;
    private static final String DEFAULT_AUDIO_CONFIG = "2"; // Stereo
    private static final boolean DEFAULT_LATENCY_TOAST = false;
    private static final String DEFAULT_FRAME_PACING = "balanced";
    private static final boolean DEFAULT_ABSOLUTE_MOUSE_MODE = false;
    private static final boolean DEFAULT_ENABLE_AUDIO_FX = false;
    private static final boolean DEFAULT_REDUCE_REFRESH_RATE = false;
    private static final boolean DEFAULT_FULL_RANGE = false;
    private static final boolean DEFAULT_GAMEPAD_TOUCHPAD_AS_MOUSE = false;
    private static final boolean DEFAULT_GAMEPAD_MOTION_SENSORS = true;
    private static final boolean DEFAULT_GAMEPAD_MOTION_FALLBACK = false;
    // O fork é focado em uso 4G: liga as otimizações de rede móvel por padrão para que
    // novos usuários se beneficiem imediatamente. Quem quiser o comportamento original do
    // Moonlight pode desligar manualmente em Configurações → Rede móvel.
    private static final boolean DEFAULT_MOBILE_NETWORK_OPT = true;
    private static final int DEFAULT_JUMP_FRAME_MODE = 0; // Off
    // Alinhado com o targetLatencyMs padrão do StreamConfiguration (50 ms).
    // O valor anterior (80 ms) era inconsistente: o StreamConfiguration assumia 50 ms
    // mas as prefs entregavam 80 ms na primeira inicialização do app.
    private static final int DEFAULT_TARGET_LATENCY_MS = 50;
    private static final boolean DEFAULT_ADAPTIVE_RESOLUTION = false;
    private static final boolean DEFAULT_BITRATE_OPTIMIZATION = true;
    private static final int DEFAULT_BITRATE_ANALYSIS_INTERVAL_MS = 50;
    private static final int DEFAULT_FRAME_SIMILARITY_THRESHOLD = 85;
    private static final boolean DEFAULT_LOCAL_UPSCALING = false;
    private static final int DEFAULT_LOCAL_UPSCALING_MODE = 1;
    private static final boolean DEFAULT_PREFER_AUDIO_OVER_VIDEO = false;
    private static final int DEFAULT_TRANSITION_FRAME_MODE = 0;
    // 1 = Linear (padrão conservador, compatível com todos os dispositivos)
    public static final int TRANSITION_INTERP_NONE = 0;
    public static final int TRANSITION_INTERP_LINEAR = 1;
    public static final int TRANSITION_INTERP_EASE_IN_OUT = 2;
    public static final int TRANSITION_INTERP_CUBIC = 3;
    public static final int TRANSITION_INTERP_EXPONENTIAL = 4;
    public static final int TRANSITION_INTERP_SMOOTH_STEP = 5;
    private static final int DEFAULT_TRANSITION_INTERPOLATION_TYPE = TRANSITION_INTERP_LINEAR;
    // 50% de força por padrão (blend moderado, não distorce o frame original)
    private static final int DEFAULT_TRANSITION_STRENGTH = 50;
    // 16 ms ≈ 60 FPS; valor seguro que não sobrecarrega CPU em dispositivos fracos
    private static final int DEFAULT_TRANSITION_FREQUENCY_MS = 16;
    private static final boolean DEFAULT_LOCAL_FRAME_DEDUPLICATION = false;
    private static final boolean DEFAULT_LOCAL_MOTION_SMOOTHING = false;
    private static final boolean DEFAULT_AUTO_RECONNECT = true;
    private static final int DEFAULT_AUTO_RECONNECT_ATTEMPTS = 3;
    // Desligados por padrão: essas análises consomem CPU em cada frame (~60x/s) e
    // a relação custo/benefício depende do dispositivo. O usuário pode ativá-las
    // manualmente em Configurações → Filtros de Vídeo após avaliar o impacto.
    // Ligá-las por padrão contradizia o objetivo do fork (melhorar desempenho em 4G).
    private static final boolean DEFAULT_BLOCK_COMPRESSION = false;
    private static final int DEFAULT_BLOCK_SIZE = 16;
    private static final boolean DEFAULT_ADAPTIVE_SHARPNESS = false;
    private static final boolean DEFAULT_HUD_DETECTION = false;
    private static final int DEFAULT_HUD_RESOLUTION_REDUCTION = 50;

    // Deduplicação de áreas: desligada por padrão (feature opcional/experimental)
    private static final boolean DEFAULT_AREA_DEDUPLICATION = false;
    // (x) a cada quantos frames a análise de padrão local é executada
    private static final int DEFAULT_AREA_DEDUP_CHECK_INTERVAL = 10;
    // (y) quantos frames anteriores são olhados para tentar achar um padrão local
    private static final int DEFAULT_AREA_DEDUP_LOOKBACK_FRAMES = 5;
    // (z) quantos dos próximos frames são descartados antes do decoder (último frame fica na tela)
    private static final int DEFAULT_AREA_DEDUP_REPLACE_FRAMES = 3;
    private static final int DEFAULT_AREA_DEDUP_SIMILARITY_THRESHOLD = 90;
    private static final int DEFAULT_AREA_DEDUP_GRID_SIZE = 8;

    public static final int FRAME_PACING_MIN_LATENCY = 0;
    public static final int FRAME_PACING_BALANCED = 1;
    public static final int FRAME_PACING_CAP_FPS = 2;
    public static final int FRAME_PACING_MAX_SMOOTHNESS = 3;

    public static final String RES_360P = "640x360";
    public static final String RES_480P = "854x480";
    public static final String RES_720P = "1280x720";
    public static final String RES_1080P = "1920x1080";
    public static final String RES_1440P = "2560x1440";
    public static final String RES_4K = "3840x2160";
    public static final String RES_NATIVE = "Native";

    public int width, height, fps;
    public int bitrate;
    public FormatOption videoFormat;
    public int deadzonePercentage;
    public int oscOpacity;
    public boolean stretchVideo, enableSops, playHostAudio, disableWarnings;
    public String language;
    public boolean smallIconMode, multiController, usbDriver, flipFaceButtons;
    public boolean onscreenController;
    public boolean onlyL3R3;
    public boolean showGuideButton;
    public boolean enableHdr;
    public boolean enablePip;
    public boolean enablePerfOverlay;
    public boolean enableLatencyToast;
    public boolean bindAllUsb;
    public boolean mouseEmulation;
    public AnalogStickForScrolling analogStickForScrolling;
    public boolean mouseNavButtons;
    public boolean unlockFps;
    public boolean vibrateOsc;
    public boolean vibrateFallbackToDevice;
    public int vibrateFallbackToDeviceStrength;
    public boolean touchscreenTrackpad;
    public MoonBridge.AudioConfiguration audioConfiguration;
    public int framePacing;
    public boolean absoluteMouseMode;
    public boolean enableAudioFx;
    public boolean reduceRefreshRate;
    public boolean fullRange;
    public boolean gamepadMotionSensors;
    public boolean gamepadTouchpadAsMouse;
    public boolean gamepadMotionSensorsFallbackToDevice;
    public boolean mobileNetworkOptimizations;
    public int jumpFrameMode;
    public int targetLatencyMs;
    public boolean adaptiveResolution;
    public boolean bitrateOptimization;
    public int bitrateAnalysisIntervalMs;
    public int frameSimilarityThreshold;
    public boolean localUpscaling;
    public int localUpscalingMode;
    public boolean preferAudioOverVideo;
    public int transitionFrameMode;
    // Tipo de interpolação entre frames (ver constantes TRANSITION_INTERP_*)
    public int transitionInterpolationType;
    // Força da transição em % (0 = sem efeito, 100 = máximo blend)
    public int transitionStrength;
    // Intervalo mínimo entre frames interpolados em ms (1–100)
    public int transitionFrequencyMs;
    public boolean localFrameDeduplication;
    public boolean localMotionSmoothing;
    public boolean autoReconnect;
    public int autoReconnectAttempts;
    public boolean blockCompressionEnabled;
    public int blockSize;
    public boolean adaptiveSharpness;
    public boolean hudDetectionEnabled;
    public int hudResolutionReduction;

    // Deduplicação de áreas (config própria, separada do menu de filtros).
    // Cada var abaixo só faz sentido/é habilitada na UI quando areaDeduplicationEnabled = true.
    public boolean areaDeduplicationEnabled;
    public int areaDedupCheckInterval;      // (x)
    public int areaDedupLookbackFrames;     // (y)
    public int areaDedupReplaceFrames;      // (z)
    public int areaDedupSimilarityThreshold;
    public int areaDedupGridSize;

    public static boolean isNativeResolution(int width, int height) {
        // It's not a native resolution if it matches an existing resolution option
        if (width == 640 && height == 360) {
            return false;
        }
        else if (width == 854 && height == 480) {
            return false;
        }
        else if (width == 1280 && height == 720) {
            return false;
        }
        else if (width == 1920 && height == 1080) {
            return false;
        }
        else if (width == 2560 && height == 1440) {
            return false;
        }
        else if (width == 3840 && height == 2160) {
            return false;
        }

        return true;
    }

    // If we have a screen that has semi-square dimensions, we may want to change our behavior
    // to allow any orientation and vertical+horizontal resolutions.
    public static boolean isSquarishScreen(int width, int height) {
        float longDim = Math.max(width, height);
        float shortDim = Math.min(width, height);

        // We just put the arbitrary cutoff for a square-ish screen at 1.3
        return longDim / shortDim < 1.3f;
    }

    public static boolean isSquarishScreen(Display display) {
        int width, height;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            width = display.getMode().getPhysicalWidth();
            height = display.getMode().getPhysicalHeight();
        }
        else {
            width = display.getWidth();
            height = display.getHeight();
        }

        return isSquarishScreen(width, height);
    }

    private static String convertFromLegacyResolutionString(String resString) {
        if (resString.equalsIgnoreCase("360p")) {
            return RES_360P;
        }
        else if (resString.equalsIgnoreCase("480p")) {
            return RES_480P;
        }
        else if (resString.equalsIgnoreCase("720p")) {
            return RES_720P;
        }
        else if (resString.equalsIgnoreCase("1080p")) {
            return RES_1080P;
        }
        else if (resString.equalsIgnoreCase("1440p")) {
            return RES_1440P;
        }
        else if (resString.equalsIgnoreCase("4K")) {
            return RES_4K;
        }
        else {
            // Should be unreachable
            return RES_720P;
        }
    }

    private static int getWidthFromResolutionString(String resString) {
        return Integer.parseInt(resString.split("x")[0]);
    }

    private static int getHeightFromResolutionString(String resString) {
        return Integer.parseInt(resString.split("x")[1]);
    }

    private static String getResolutionString(int width, int height) {
        switch (height) {
            case 360:
                return RES_360P;
            case 480:
                return RES_480P;
            default:
            case 720:
                return RES_720P;
            case 1080:
                return RES_1080P;
            case 1440:
                return RES_1440P;
            case 2160:
                return RES_4K;
        }
    }

    public static int getDefaultBitrate(String resString, String fpsString) {
        int width = getWidthFromResolutionString(resString);
        int height = getHeightFromResolutionString(resString);
        int fps = Integer.parseInt(fpsString);

        // This logic is shamelessly stolen from Moonlight Qt:
        // https://github.com/moonlight-stream/moonlight-qt/blob/master/app/settings/streamingpreferences.cpp

        // Don't scale bitrate linearly beyond 60 FPS. It's definitely not a linear
        // bitrate increase for frame rate once we get to values that high.
        double frameRateFactor = (fps <= 60 ? fps : (Math.sqrt(fps / 60.f) * 60.f)) / 30.f;

        // TODO: Collect some empirical data to see if these defaults make sense.
        // We're just using the values that the Shield used, as we have for years.
        int[] pixelVals = {
            640 * 360,
            854 * 480,
            1280 * 720,
            1920 * 1080,
            2560 * 1440,
            3840 * 2160,
            -1,
        };
        int[] factorVals = {
            1,
            2,
            5,
            10,
            20,
            40,
            -1
        };

        // Calculate the resolution factor by linear interpolation of the resolution table
        float resolutionFactor;
        int pixels = width * height;
        for (int i = 0; ; i++) {
            if (pixels == pixelVals[i]) {
                // We can bail immediately for exact matches
                resolutionFactor = factorVals[i];
                break;
            }
            else if (pixels < pixelVals[i]) {
                if (i == 0) {
                    // Never go below the lowest resolution entry
                    resolutionFactor = factorVals[i];
                }
                else {
                    // Interpolate between the entry greater than the chosen resolution (i) and the entry less than the chosen resolution (i-1)
                    resolutionFactor = ((float)(pixels - pixelVals[i-1]) / (pixelVals[i] - pixelVals[i-1])) * (factorVals[i] - factorVals[i-1]) + factorVals[i-1];
                }
                break;
            }
            else if (pixelVals[i] == -1) {
                // Never go above the highest resolution entry
                resolutionFactor = factorVals[i-1];
                break;
            }
        }

        return (int)Math.round(resolutionFactor * frameRateFactor) * 1000;
    }

    public static boolean getDefaultSmallMode(Context context) {
        PackageManager manager = context.getPackageManager();
        if (manager != null) {
            // TVs shouldn't use small mode by default
            if (manager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)) {
                return false;
            }

            // API 21 uses LEANBACK instead of TELEVISION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                if (manager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
                    return false;
                }
            }
        }

        // Use small mode on anything smaller than a 7" tablet
        return context.getResources().getConfiguration().smallestScreenWidthDp < 500;
    }

    public static int getDefaultBitrate(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return getDefaultBitrate(
                prefs.getString(RESOLUTION_PREF_STRING, DEFAULT_RESOLUTION),
                prefs.getString(FPS_PREF_STRING, DEFAULT_FPS));
    }

    private static FormatOption getVideoFormatValue(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String str = prefs.getString(VIDEO_FORMAT_PREF_STRING, DEFAULT_VIDEO_FORMAT);
        if (str.equals("auto")) {
            return FormatOption.AUTO;
        }
        else if (str.equals("forceav1")) {
            return FormatOption.FORCE_AV1;
        }
        else if (str.equals("forceh265")) {
            return FormatOption.FORCE_HEVC;
        }
        else if (str.equals("neverh265")) {
            return FormatOption.FORCE_H264;
        }
        else {
            // Should never get here
            return FormatOption.AUTO;
        }
    }

    private static int getFramePacingValue(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Migrate legacy never drop frames option to the new location
        if (prefs.contains(LEGACY_DISABLE_FRAME_DROP_PREF_STRING)) {
            boolean legacyNeverDropFrames = prefs.getBoolean(LEGACY_DISABLE_FRAME_DROP_PREF_STRING, false);
            prefs.edit()
                    .remove(LEGACY_DISABLE_FRAME_DROP_PREF_STRING)
                    .putString(FRAME_PACING_PREF_STRING, legacyNeverDropFrames ? "balanced" : "latency")
                    .apply();
        }

        String str = prefs.getString(FRAME_PACING_PREF_STRING, DEFAULT_FRAME_PACING);
        if (str.equals("latency")) {
            return FRAME_PACING_MIN_LATENCY;
        }
        else if (str.equals("balanced")) {
            return FRAME_PACING_BALANCED;
        }
        else if (str.equals("cap-fps")) {
            return FRAME_PACING_CAP_FPS;
        }
        else if (str.equals("smoothness")) {
            return FRAME_PACING_MAX_SMOOTHNESS;
        }
        else {
            // Should never get here
            return FRAME_PACING_MIN_LATENCY;
        }
    }

    private static AnalogStickForScrolling getAnalogStickForScrollingValue(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String str = prefs.getString(ANALOG_SCROLLING_PREF_STRING, DEFAULT_ANALOG_STICK_FOR_SCROLLING);
        if (str.equals("right")) {
            return AnalogStickForScrolling.RIGHT;
        }
        else if (str.equals("left")) {
            return AnalogStickForScrolling.LEFT;
        }
        else {
            return AnalogStickForScrolling.NONE;
        }
    }

    public static void resetStreamingSettings(Context context) {
        // We consider resolution, FPS, bitrate, HDR, and video format as "streaming settings" here
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .remove(BITRATE_PREF_STRING)
                .remove(BITRATE_PREF_OLD_STRING)
                .remove(LEGACY_RES_FPS_PREF_STRING)
                .remove(RESOLUTION_PREF_STRING)
                .remove(FPS_PREF_STRING)
                .remove(VIDEO_FORMAT_PREF_STRING)
                .remove(ENABLE_HDR_PREF_STRING)
                .remove(UNLOCK_FPS_STRING)
                .remove(FULL_RANGE_PREF_STRING)
                .apply();
    }

    public static void completeLanguagePreferenceMigration(Context context) {
        // Put our language option back to default which tells us that we've already migrated it
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(LANGUAGE_PREF_STRING, DEFAULT_LANGUAGE).apply();
    }

    public static boolean isShieldAtvFirmwareWithBrokenHdr() {
        // This particular Shield TV firmware crashes when using HDR
        // https://www.nvidia.com/en-us/geforce/forums/notifications/comment/155192/
        return Build.MANUFACTURER.equalsIgnoreCase("NVIDIA") &&
                Build.FINGERPRINT.contains("PPR1.180610.011/4079208_2235.1395");
    }

    public static PreferenceConfiguration readPreferences(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        PreferenceConfiguration config = new PreferenceConfiguration();

        // Migrate legacy preferences to the new locations
        if (prefs.contains(LEGACY_ENABLE_51_SURROUND_PREF_STRING)) {
            if (prefs.getBoolean(LEGACY_ENABLE_51_SURROUND_PREF_STRING, false)) {
                prefs.edit()
                        .remove(LEGACY_ENABLE_51_SURROUND_PREF_STRING)
                        .putString(AUDIO_CONFIG_PREF_STRING, "51")
                        .apply();
            }
        }

        String str = prefs.getString(LEGACY_RES_FPS_PREF_STRING, null);
        if (str != null) {
            if (str.equals("360p30")) {
                config.width = 640;
                config.height = 360;
                config.fps = 30;
            }
            else if (str.equals("360p60")) {
                config.width = 640;
                config.height = 360;
                config.fps = 60;
            }
            else if (str.equals("720p30")) {
                config.width = 1280;
                config.height = 720;
                config.fps = 30;
            }
            else if (str.equals("720p60")) {
                config.width = 1280;
                config.height = 720;
                config.fps = 60;
            }
            else if (str.equals("1080p30")) {
                config.width = 1920;
                config.height = 1080;
                config.fps = 30;
            }
            else if (str.equals("1080p60")) {
                config.width = 1920;
                config.height = 1080;
                config.fps = 60;
            }
            else if (str.equals("4K30")) {
                config.width = 3840;
                config.height = 2160;
                config.fps = 30;
            }
            else if (str.equals("4K60")) {
                config.width = 3840;
                config.height = 2160;
                config.fps = 60;
            }
            else {
                // Should never get here
                config.width = 1280;
                config.height = 720;
                config.fps = 60;
            }

            prefs.edit()
                    .remove(LEGACY_RES_FPS_PREF_STRING)
                    .putString(RESOLUTION_PREF_STRING, getResolutionString(config.width, config.height))
                    .putString(FPS_PREF_STRING, ""+config.fps)
                    .apply();
        }
        else {
            // Use the new preference location
            String resStr = prefs.getString(RESOLUTION_PREF_STRING, PreferenceConfiguration.DEFAULT_RESOLUTION);

            // Convert legacy resolution strings to the new style
            if (!resStr.contains("x")) {
                resStr = PreferenceConfiguration.convertFromLegacyResolutionString(resStr);
                prefs.edit().putString(RESOLUTION_PREF_STRING, resStr).apply();
            }

            config.width = PreferenceConfiguration.getWidthFromResolutionString(resStr);
            config.height = PreferenceConfiguration.getHeightFromResolutionString(resStr);
            config.fps = Integer.parseInt(prefs.getString(FPS_PREF_STRING, PreferenceConfiguration.DEFAULT_FPS));
        }

        if (!prefs.contains(SMALL_ICONS_PREF_STRING)) {
            // We need to write small icon mode's default to disk for the settings page to display
            // the current state of the option properly
            prefs.edit().putBoolean(SMALL_ICONS_PREF_STRING, getDefaultSmallMode(context)).apply();
        }

        if (!prefs.contains(GAMEPAD_MOTION_SENSORS_PREF_STRING) && Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {
            // Android 12 has a nasty bug that causes crashes when the app touches the InputDevice's
            // associated InputDeviceSensorManager (just calling getSensorManager() is enough).
            // As a workaround, we will override the default value for the gamepad motion sensor
            // option to disabled on Android 12 to reduce the impact of this bug.
            // https://cs.android.com/android/_/android/platform/frameworks/base/+/8970010a5e9f3dc5c069f56b4147552accfcbbeb
            prefs.edit().putBoolean(GAMEPAD_MOTION_SENSORS_PREF_STRING, false).apply();
        }

        // This must happen after the preferences migration to ensure the preferences are populated
        config.bitrate = prefs.getInt(BITRATE_PREF_STRING, prefs.getInt(BITRATE_PREF_OLD_STRING, 0) * 1000);
        if (config.bitrate == 0) {
            config.bitrate = getDefaultBitrate(context);
        }

        String audioConfig = prefs.getString(AUDIO_CONFIG_PREF_STRING, DEFAULT_AUDIO_CONFIG);
        if (audioConfig.equals("71")) {
            config.audioConfiguration = MoonBridge.AUDIO_CONFIGURATION_71_SURROUND;
        }
        else if (audioConfig.equals("51")) {
            config.audioConfiguration = MoonBridge.AUDIO_CONFIGURATION_51_SURROUND;
        }
        else /* if (audioConfig.equals("2")) */ {
            config.audioConfiguration = MoonBridge.AUDIO_CONFIGURATION_STEREO;
        }

        config.videoFormat = getVideoFormatValue(context);
        config.framePacing = getFramePacingValue(context);

        config.analogStickForScrolling = getAnalogStickForScrollingValue(context);

        config.deadzonePercentage = prefs.getInt(DEADZONE_PREF_STRING, DEFAULT_DEADZONE);

        config.oscOpacity = prefs.getInt(OSC_OPACITY_PREF_STRING, DEFAULT_OPACITY);

        config.language = prefs.getString(LANGUAGE_PREF_STRING, DEFAULT_LANGUAGE);

        // Checkbox preferences
        config.disableWarnings = prefs.getBoolean(DISABLE_TOASTS_PREF_STRING, DEFAULT_DISABLE_TOASTS);
        config.enableSops = prefs.getBoolean(SOPS_PREF_STRING, DEFAULT_SOPS);
        config.stretchVideo = prefs.getBoolean(STRETCH_PREF_STRING, DEFAULT_STRETCH);
        config.playHostAudio = prefs.getBoolean(HOST_AUDIO_PREF_STRING, DEFAULT_HOST_AUDIO);
        config.smallIconMode = prefs.getBoolean(SMALL_ICONS_PREF_STRING, getDefaultSmallMode(context));
        config.multiController = prefs.getBoolean(MULTI_CONTROLLER_PREF_STRING, DEFAULT_MULTI_CONTROLLER);
        config.usbDriver = prefs.getBoolean(USB_DRIVER_PREF_SRING, DEFAULT_USB_DRIVER);
        config.onscreenController = prefs.getBoolean(ONSCREEN_CONTROLLER_PREF_STRING, ONSCREEN_CONTROLLER_DEFAULT);
        config.onlyL3R3 = prefs.getBoolean(ONLY_L3_R3_PREF_STRING, ONLY_L3_R3_DEFAULT);
        config.showGuideButton = prefs.getBoolean(SHOW_GUIDE_BUTTON_PREF_STRING, SHOW_GUIDE_BUTTON_DEFAULT);
        config.enableHdr = prefs.getBoolean(ENABLE_HDR_PREF_STRING, DEFAULT_ENABLE_HDR) && !isShieldAtvFirmwareWithBrokenHdr();
        config.enablePip = prefs.getBoolean(ENABLE_PIP_PREF_STRING, DEFAULT_ENABLE_PIP);
        config.enablePerfOverlay = prefs.getBoolean(ENABLE_PERF_OVERLAY_STRING, DEFAULT_ENABLE_PERF_OVERLAY);
        config.bindAllUsb = prefs.getBoolean(BIND_ALL_USB_STRING, DEFAULT_BIND_ALL_USB);
        config.mouseEmulation = prefs.getBoolean(MOUSE_EMULATION_STRING, DEFAULT_MOUSE_EMULATION);
        config.mouseNavButtons = prefs.getBoolean(MOUSE_NAV_BUTTONS_STRING, DEFAULT_MOUSE_NAV_BUTTONS);
        config.unlockFps = prefs.getBoolean(UNLOCK_FPS_STRING, DEFAULT_UNLOCK_FPS);
        config.vibrateOsc = prefs.getBoolean(VIBRATE_OSC_PREF_STRING, DEFAULT_VIBRATE_OSC);
        config.vibrateFallbackToDevice = prefs.getBoolean(VIBRATE_FALLBACK_PREF_STRING, DEFAULT_VIBRATE_FALLBACK);
        config.vibrateFallbackToDeviceStrength = prefs.getInt(VIBRATE_FALLBACK_STRENGTH_PREF_STRING, DEFAULT_VIBRATE_FALLBACK_STRENGTH);
        config.flipFaceButtons = prefs.getBoolean(FLIP_FACE_BUTTONS_PREF_STRING, DEFAULT_FLIP_FACE_BUTTONS);
        config.touchscreenTrackpad = prefs.getBoolean(TOUCHSCREEN_TRACKPAD_PREF_STRING, DEFAULT_TOUCHSCREEN_TRACKPAD);
        config.enableLatencyToast = prefs.getBoolean(LATENCY_TOAST_PREF_STRING, DEFAULT_LATENCY_TOAST);
        config.absoluteMouseMode = prefs.getBoolean(ABSOLUTE_MOUSE_MODE_PREF_STRING, DEFAULT_ABSOLUTE_MOUSE_MODE);
        config.enableAudioFx = prefs.getBoolean(ENABLE_AUDIO_FX_PREF_STRING, DEFAULT_ENABLE_AUDIO_FX);
        config.reduceRefreshRate = prefs.getBoolean(REDUCE_REFRESH_RATE_PREF_STRING, DEFAULT_REDUCE_REFRESH_RATE);
        config.fullRange = prefs.getBoolean(FULL_RANGE_PREF_STRING, DEFAULT_FULL_RANGE);
        config.gamepadTouchpadAsMouse = prefs.getBoolean(GAMEPAD_TOUCHPAD_AS_MOUSE_PREF_STRING, DEFAULT_GAMEPAD_TOUCHPAD_AS_MOUSE);
        config.gamepadMotionSensors = prefs.getBoolean(GAMEPAD_MOTION_SENSORS_PREF_STRING, DEFAULT_GAMEPAD_MOTION_SENSORS);
        config.gamepadMotionSensorsFallbackToDevice = prefs.getBoolean(GAMEPAD_MOTION_FALLBACK_PREF_STRING, DEFAULT_GAMEPAD_MOTION_FALLBACK);

        // Otimizações para 4G / redes móveis (já vêm predefinidas com os melhores valores)
        config.mobileNetworkOptimizations = prefs.getBoolean(MOBILE_NETWORK_OPT_PREF_STRING, DEFAULT_MOBILE_NETWORK_OPT);
        config.jumpFrameMode = Integer.parseInt(prefs.getString(JUMP_FRAME_MODE_PREF_STRING, String.valueOf(DEFAULT_JUMP_FRAME_MODE)));
        config.targetLatencyMs = prefs.getInt(TARGET_LATENCY_PREF_STRING, DEFAULT_TARGET_LATENCY_MS);
        config.adaptiveResolution = prefs.getBoolean(ADAPTIVE_RESOLUTION_PREF_STRING, DEFAULT_ADAPTIVE_RESOLUTION);
        config.bitrateOptimization = prefs.getBoolean(BITRATE_OPTIMIZATION_PREF_STRING, DEFAULT_BITRATE_OPTIMIZATION);
        config.bitrateAnalysisIntervalMs = prefs.getInt(BITRATE_ANALYSIS_INTERVAL_PREF_STRING, DEFAULT_BITRATE_ANALYSIS_INTERVAL_MS);
        config.frameSimilarityThreshold = prefs.getInt(FRAME_SIMILARITY_THRESHOLD_PREF_STRING, DEFAULT_FRAME_SIMILARITY_THRESHOLD);
        config.localUpscaling = prefs.getBoolean(LOCAL_UPSCALING_PREF_STRING, DEFAULT_LOCAL_UPSCALING);
        config.localUpscalingMode = Integer.parseInt(prefs.getString(LOCAL_UPSCALING_MODE_PREF_STRING, String.valueOf(DEFAULT_LOCAL_UPSCALING_MODE)));
        config.preferAudioOverVideo = prefs.getBoolean(PREFER_AUDIO_OVER_VIDEO_PREF_STRING, DEFAULT_PREFER_AUDIO_OVER_VIDEO);
        config.transitionFrameMode = Integer.parseInt(prefs.getString(TRANSITION_FRAME_MODE_PREF_STRING, String.valueOf(DEFAULT_TRANSITION_FRAME_MODE)));
        config.transitionInterpolationType = Integer.parseInt(prefs.getString(TRANSITION_INTERPOLATION_TYPE_PREF_STRING, String.valueOf(DEFAULT_TRANSITION_INTERPOLATION_TYPE)));
        config.transitionStrength = prefs.getInt(TRANSITION_STRENGTH_PREF_STRING, DEFAULT_TRANSITION_STRENGTH);
        config.transitionFrequencyMs = prefs.getInt(TRANSITION_FREQUENCY_MS_PREF_STRING, DEFAULT_TRANSITION_FREQUENCY_MS);
        config.localFrameDeduplication = prefs.getBoolean(LOCAL_FRAME_DEDUPLICATION_PREF_STRING, DEFAULT_LOCAL_FRAME_DEDUPLICATION);
        config.localMotionSmoothing = prefs.getBoolean(LOCAL_MOTION_SMOOTHING_PREF_STRING, DEFAULT_LOCAL_MOTION_SMOOTHING);
        config.autoReconnect = prefs.getBoolean(AUTO_RECONNECT_PREF_STRING, DEFAULT_AUTO_RECONNECT);
        config.autoReconnectAttempts = prefs.getInt(AUTO_RECONNECT_ATTEMPTS_PREF_STRING, DEFAULT_AUTO_RECONNECT_ATTEMPTS);
        
        // Block compression, adaptive sharpness e HUD detection
        config.blockCompressionEnabled = prefs.getBoolean(BLOCK_COMPRESSION_PREF_STRING, DEFAULT_BLOCK_COMPRESSION);
        config.blockSize = Integer.parseInt(prefs.getString(BLOCK_SIZE_PREF_STRING, String.valueOf(DEFAULT_BLOCK_SIZE)));
        config.adaptiveSharpness = prefs.getBoolean(ADAPTIVE_SHARPNESS_PREF_STRING, DEFAULT_ADAPTIVE_SHARPNESS);
        config.hudDetectionEnabled = prefs.getBoolean(HUD_DETECTION_PREF_STRING, DEFAULT_HUD_DETECTION);
        config.hudResolutionReduction = prefs.getInt(HUD_RESOLUTION_REDUCTION_PREF_STRING, DEFAULT_HUD_RESOLUTION_REDUCTION);

        // Deduplicação de áreas
        config.areaDeduplicationEnabled = prefs.getBoolean(AREA_DEDUPLICATION_PREF_STRING, DEFAULT_AREA_DEDUPLICATION);
        config.areaDedupCheckInterval = prefs.getInt(AREA_DEDUP_CHECK_INTERVAL_PREF_STRING, DEFAULT_AREA_DEDUP_CHECK_INTERVAL);
        config.areaDedupLookbackFrames = prefs.getInt(AREA_DEDUP_LOOKBACK_FRAMES_PREF_STRING, DEFAULT_AREA_DEDUP_LOOKBACK_FRAMES);
        config.areaDedupReplaceFrames = prefs.getInt(AREA_DEDUP_REPLACE_FRAMES_PREF_STRING, DEFAULT_AREA_DEDUP_REPLACE_FRAMES);
        config.areaDedupSimilarityThreshold = prefs.getInt(AREA_DEDUP_SIMILARITY_THRESHOLD_PREF_STRING, DEFAULT_AREA_DEDUP_SIMILARITY_THRESHOLD);
        config.areaDedupGridSize = prefs.getInt(AREA_DEDUP_GRID_SIZE_PREF_STRING, DEFAULT_AREA_DEDUP_GRID_SIZE);

        return config;
    }
}