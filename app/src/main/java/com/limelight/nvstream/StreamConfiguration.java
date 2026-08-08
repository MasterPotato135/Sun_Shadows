//main/java/com/limelight/nvstream/StreamConfiguration.java
// OTIMIZAÇÕES PARA 4G + TAILSCALE P2P:
// - Resolução adaptativa para redes móveis ruins
// - Jump-frame configuration (pula frames para economizar banda)
// - Bitrate dinâmico baseado em detecção de latência
// - Compressão agressiva em 4G
package com.limelight.nvstream;

import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.jni.MoonBridge;

public class StreamConfiguration {
    public static final int INVALID_APP_ID = 0;

    public static final int STREAM_CFG_LOCAL = 0;
    public static final int STREAM_CFG_REMOTE = 1;
    public static final int STREAM_CFG_AUTO = 2;
    
    // TAILSCALE P2P + 4G OPTIMIZATIONS
    public static final int NETWORK_TYPE_LAN = 0;
    public static final int NETWORK_TYPE_TAILSCALE_P2P = 1;
    public static final int NETWORK_TYPE_4G = 2;
    public static final int NETWORK_TYPE_5G = 3;
    
    // Jump-frame modes para economizar banda
    public static final int JUMPFRAME_MODE_OFF = 0;
    public static final int JUMPFRAME_MODE_LIGHT = 1;  // Pula 1 frame a cada 5 (20% economia)
    public static final int JUMPFRAME_MODE_MEDIUM = 2; // Pula 2 frames a cada 5 (40% economia)
    public static final int JUMPFRAME_MODE_HEAVY = 3;  // Pula 3 frames a cada 5 (60% economia)
    
    private NvApp app;
    private int width, height;
    private int refreshRate;
    private int launchRefreshRate;
    private int clientRefreshRateX100;
    private int bitrate;
    private int dynamicBitrate;
    private boolean sops;
    private boolean enableAdaptiveResolution;
    private boolean playLocalAudio;
    private int maxPacketSize;
    private int remote;
    private MoonBridge.AudioConfiguration audioConfiguration;
    private int supportedVideoFormats;
    private int attachedGamepadMask;
    private int colorRange;
    private int colorSpace;
    private boolean persistGamepadsAfterDisconnect;
    
    // NEW: 4G + Tailscale optimizations
    private int networkType = NETWORK_TYPE_LAN;
    private boolean enable4GOptimizations = false;
    private boolean enableTailscaleP2P = true;
    private int jumpFrameMode = JUMPFRAME_MODE_OFF;
    private int targetLatencyMs = 50;
    private int maxLatencyMs = 150;
    private boolean enableFrameSkip = false;
    private boolean bitrateOptimization = false;
    private int bitrateAnalysisIntervalMs = 50;
    private int frameSimilarityThreshold = 85;
    private boolean localUpscaling = false;
    private int localUpscalingMode = 0;
    private boolean preferAudioOverVideo = false;
    private int transitionFrameMode = 0;
    private boolean localFrameDeduplication = false;
    private boolean localMotionSmoothing = false;
    private int minBitrateFor4G = 2000;  // 2Mbps mínimo
    private int maxBitrateFor4G = 8000;  // 8Mbps máximo
    
    // Block compression + Adaptive processing
    private boolean blockCompressionEnabled = true;
    private int blockSize = 16;  // 8 ou 16 pixels
    private boolean adaptiveSharpness = true;

    public static class Builder {
        private StreamConfiguration config = new StreamConfiguration();
        
        public StreamConfiguration.Builder setApp(NvApp app) {
            config.app = app;
            return this;
        }
        
        public StreamConfiguration.Builder setRemoteConfiguration(int remote) {
            config.remote = remote;
            return this;
        }
        
        public StreamConfiguration.Builder setResolution(int width, int height) {
            config.width = width;
            config.height = height;
            return this;
        }
        
        public StreamConfiguration.Builder setRefreshRate(int refreshRate) {
            config.refreshRate = refreshRate;
            return this;
        }

        public StreamConfiguration.Builder setLaunchRefreshRate(int refreshRate) {
            config.launchRefreshRate = refreshRate;
            return this;
        }
        
        public StreamConfiguration.Builder setBitrate(int bitrate) {
            config.bitrate = bitrate;
            config.dynamicBitrate = bitrate;
            return this;
        }
        
        // NEW: Dynamic bitrate para adaptar em tempo real
        public StreamConfiguration.Builder setDynamicBitrate(int bitrate) {
            config.dynamicBitrate = bitrate;
            return this;
        }
        
        public StreamConfiguration.Builder setEnableSops(boolean enable) {
            config.sops = enable;
            return this;
        }
        
        public StreamConfiguration.Builder enableAdaptiveResolution(boolean enable) {
            config.enableAdaptiveResolution = enable;
            return this;
        }

        public StreamConfiguration.Builder setBitrateOptimization(boolean enable) {
            config.bitrateOptimization = enable;
            return this;
        }

        public StreamConfiguration.Builder setBitrateAnalysisIntervalMs(int intervalMs) {
            config.bitrateAnalysisIntervalMs = intervalMs;
            return this;
        }

        public StreamConfiguration.Builder setFrameSimilarityThreshold(int threshold) {
            config.frameSimilarityThreshold = threshold;
            return this;
        }

        public StreamConfiguration.Builder setLocalUpscaling(boolean enable) {
            config.localUpscaling = enable;
            return this;
        }

        public StreamConfiguration.Builder setLocalUpscalingMode(int mode) {
            config.localUpscalingMode = mode;
            return this;
        }

        public StreamConfiguration.Builder setPreferAudioOverVideo(boolean enable) {
            config.preferAudioOverVideo = enable;
            return this;
        }

        public StreamConfiguration.Builder setTransitionFrameMode(int mode) {
            config.transitionFrameMode = mode;
            return this;
        }

        public StreamConfiguration.Builder setLocalFrameDeduplication(boolean enable) {
            config.localFrameDeduplication = enable;
            return this;
        }

        public StreamConfiguration.Builder setLocalMotionSmoothing(boolean enable) {
            config.localMotionSmoothing = enable;
            return this;
        }
        
        public StreamConfiguration.Builder setBlockCompressionEnabled(boolean enable) {
            config.blockCompressionEnabled = enable;
            return this;
        }
        
        public StreamConfiguration.Builder setBlockSize(int size) {
            config.blockSize = size;  // 8 ou 16
            return this;
        }
        
        public StreamConfiguration.Builder setAdaptiveSharpnessEnabled(boolean enable) {
            config.adaptiveSharpness = enable;
            return this;
        }
        
        public StreamConfiguration.Builder enableLocalAudioPlayback(boolean enable) {
            config.playLocalAudio = enable;
            return this;
        }
        
        public StreamConfiguration.Builder setMaxPacketSize(int maxPacketSize) {
            config.maxPacketSize = maxPacketSize;
            return this;
        }

        public StreamConfiguration.Builder setAttachedGamepadMask(int attachedGamepadMask) {
            config.attachedGamepadMask = attachedGamepadMask;
            return this;
        }

        public StreamConfiguration.Builder setAttachedGamepadMaskByCount(int gamepadCount) {
            config.attachedGamepadMask = 0;
            for (int i = 0; i < 4; i++) {
                if (gamepadCount > i) {
                    config.attachedGamepadMask |= 1 << i;
                }
            }
            return this;
        }

        public StreamConfiguration.Builder setPersistGamepadsAfterDisconnect(boolean value) {
            config.persistGamepadsAfterDisconnect = value;
            return this;
        }

        public StreamConfiguration.Builder setClientRefreshRateX100(int refreshRateX100) {
            config.clientRefreshRateX100 = refreshRateX100;
            return this;
        }

        public StreamConfiguration.Builder setAudioConfiguration(MoonBridge.AudioConfiguration audioConfig) {
            config.audioConfiguration = audioConfig;
            return this;
        }
        
        public StreamConfiguration.Builder setSupportedVideoFormats(int supportedVideoFormats) {
            config.supportedVideoFormats = supportedVideoFormats;
            return this;
        }

        public StreamConfiguration.Builder setColorRange(int colorRange) {
            config.colorRange = colorRange;
            return this;
        }

        public StreamConfiguration.Builder setColorSpace(int colorSpace) {
            config.colorSpace = colorSpace;
            return this;
        }
        
        // NEW: 4G + Tailscale P2P configuration methods
        public StreamConfiguration.Builder setNetworkType(int networkType) {
            config.networkType = networkType;
            return this;
        }
        
        public StreamConfiguration.Builder enable4GOptimizations(boolean enable) {
            config.enable4GOptimizations = enable;
            if (enable) {
                config.networkType = NETWORK_TYPE_4G;
                // Keep the slow-network branch conservative so that it is more stable on
                // weak mobile links instead of forcing the newest aggressive behavior.
                config.enableFrameSkip = false;
                config.jumpFrameMode = JUMPFRAME_MODE_LIGHT;
                config.enableAdaptiveResolution = true;
                config.targetLatencyMs = Math.max(config.targetLatencyMs, 80);
                if (config.width > 1920 || config.height > 1080) {
                    config.width = 1920;
                    config.height = 1080;
                }
                // Clamp to the same 4G bitrate ceiling used by setDynamicBitrateFor4G()
                // (maxBitrateFor4G, 8Mbps by default) instead of a lower hardcoded value.
                // The previous 6000 cap was inconsistent with the declared 4G bitrate range
                // and unnecessarily throttled quality/FPS headroom on good 4G/5G links.
                if (config.bitrate > config.maxBitrateFor4G) {
                    config.bitrate = config.maxBitrateFor4G;
                    config.dynamicBitrate = config.maxBitrateFor4G;
                }
            }
            return this;
        }
        
        public StreamConfiguration.Builder enableTailscaleP2P(boolean enable) {
            config.enableTailscaleP2P = enable;
            if (enable) {
                config.networkType = NETWORK_TYPE_TAILSCALE_P2P;
            }
            return this;
        }
        
        public StreamConfiguration.Builder setJumpFrameMode(int mode) {
            config.jumpFrameMode = mode;
            return this;
        }
        
        public StreamConfiguration.Builder setTargetLatencyMs(int latencyMs) {
            config.targetLatencyMs = latencyMs;
            return this;
        }
        
        public StreamConfiguration.Builder setMaxLatencyMs(int latencyMs) {
            config.maxLatencyMs = latencyMs;
            return this;
        }

        /** Sets the minimum bitrate (kbps) enforced when 4G optimizations are active. */
        public StreamConfiguration.Builder setMinBitrateFor4G(int bitrateKbps) {
            config.minBitrateFor4G = bitrateKbps;
            return this;
        }

        /** Sets the maximum bitrate (kbps) enforced when 4G optimizations are active. */
        public StreamConfiguration.Builder setMaxBitrateFor4G(int bitrateKbps) {
            config.maxBitrateFor4G = bitrateKbps;
            return this;
        }
        
        public StreamConfiguration.Builder enableFrameSkip(boolean enable) {
            config.enableFrameSkip = enable;
            return this;
        }

        public StreamConfiguration.Builder setEnableAdaptiveResolution(boolean enable) {
            config.enableAdaptiveResolution = enable;
            return this;
        }

        public StreamConfiguration build() {
            return config;
        }
    }
    
    private StreamConfiguration() {
        // Set default attributes
        this.app = new NvApp("Steam");
        this.width = 1280;
        this.height = 720;
        this.refreshRate = 60;
        this.launchRefreshRate = 60;
        this.bitrate = 10000;
        this.dynamicBitrate = 10000;
        this.maxPacketSize = 1024;
        this.remote = STREAM_CFG_AUTO;
        this.sops = true;
        this.enableAdaptiveResolution = false;
        this.audioConfiguration = MoonBridge.AUDIO_CONFIGURATION_STEREO;
        this.supportedVideoFormats = MoonBridge.VIDEO_FORMAT_H264;
        this.attachedGamepadMask = 0;
        this.networkType = NETWORK_TYPE_LAN;
        this.enable4GOptimizations = false;
        this.enableTailscaleP2P = true;
        this.jumpFrameMode = JUMPFRAME_MODE_OFF;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getRefreshRate() {
        return refreshRate;
    }

    public int getLaunchRefreshRate() {
        return launchRefreshRate;
    }
    
    public int getBitrate() {
        return bitrate;
    }
    
    // NEW: Dynamic bitrate getter
    public int getDynamicBitrate() {
        return dynamicBitrate;
    }
    
    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    public NvApp getApp() {
        return app;
    }
    
    public boolean getSops() {
        return sops;
    }
    
    public boolean getAdaptiveResolutionEnabled() {
        return enableAdaptiveResolution;
    }
    
    public boolean getPlayLocalAudio() {
        return playLocalAudio;
    }
    
    public int getRemote() {
        return remote;
    }

    public MoonBridge.AudioConfiguration getAudioConfiguration() {
        return audioConfiguration;
    }
    
    public int getSupportedVideoFormats() {
        return supportedVideoFormats;
    }

    public int getAttachedGamepadMask() {
        return attachedGamepadMask;
    }

    public boolean getPersistGamepadsAfterDisconnect() {
        return persistGamepadsAfterDisconnect;
    }

    public int getClientRefreshRateX100() {
        return clientRefreshRateX100;
    }

    public int getColorRange() {
        return colorRange;
    }

    public int getColorSpace() {
        return colorSpace;
    }
    
    // NEW: 4G + Tailscale P2P getters
    public int getNetworkType() {
        return networkType;
    }
    
    public boolean is4GOptimizationsEnabled() {
        return enable4GOptimizations;
    }
    
    public boolean isTailscaleP2PEnabled() {
        return enableTailscaleP2P;
    }
    
    public int getJumpFrameMode() {
        return jumpFrameMode;
    }
    
    public int getTargetLatencyMs() {
        return targetLatencyMs;
    }
    
    public int getMaxLatencyMs() {
        return maxLatencyMs;
    }
    
    public boolean isFrameSkipEnabled() {
        return enableFrameSkip;
    }

    public boolean isBitrateOptimizationEnabled() {
        return bitrateOptimization;
    }

    public int getBitrateAnalysisIntervalMs() {
        return bitrateAnalysisIntervalMs;
    }

    public int getFrameSimilarityThreshold() {
        return frameSimilarityThreshold;
    }

    public boolean isLocalUpscalingEnabled() {
        return localUpscaling;
    }

    public int getLocalUpscalingMode() {
        return localUpscalingMode;
    }

    public boolean isPreferAudioOverVideoEnabled() {
        return preferAudioOverVideo;
    }

    public int getTransitionFrameMode() {
        return transitionFrameMode;
    }

    public boolean isLocalFrameDeduplicationEnabled() {
        return localFrameDeduplication;
    }

    public boolean isLocalMotionSmoothingEnabled() {
        return localMotionSmoothing;
    }
    
    public int getMinBitrateFor4G() {
        return minBitrateFor4G;
    }
    
    public int getMaxBitrateFor4G() {
        return maxBitrateFor4G;
    }
    
    public boolean isBlockCompressionEnabled() {
        return blockCompressionEnabled;
    }
    
    public int getBlockSize() {
        return blockSize;
    }
    
    public boolean isAdaptiveSharpnessEnabled() {
        return adaptiveSharpness;
    }
    
    // Dynamic bitrate adjustment method
    public void setDynamicBitrate(int bitrate) {
        // Clamp bitrate based on network type
        if (enable4GOptimizations) {
            if (bitrate < minBitrateFor4G) {
                this.dynamicBitrate = minBitrateFor4G;
            } else if (bitrate > maxBitrateFor4G) {
                this.dynamicBitrate = maxBitrateFor4G;
            } else {
                this.dynamicBitrate = bitrate;
            }
        } else {
            this.dynamicBitrate = bitrate;
        }
    }
}