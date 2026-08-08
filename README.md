# ☀️ Sun Shadows

> A remote streaming client optimized for unstable mobile networks.

Sun Shadows is a fork of [Moonlight Android](https://github.com/moonlight-stream/moonlight-android) focused on improving game and application streaming over challenging connections such as 4G mobile networks.

While Moonlight is designed for excellent performance on local networks and stable connections, Sun Shadows focuses on adapting streaming for environments with limited bandwidth, higher latency, and packet instability through client-side optimizations.

---

# ✨ Features

## 📶 Mobile Network Optimization

Sun Shadows includes optimizations designed specifically for mobile networks:

- Dynamic bitrate control — adjusts the host encoder bitrate in real time via the control stream when frame analysis detects changing conditions
- Latency monitoring
- Connection instability handling
- Mobile network profiles
- Bandwidth optimization

Available profiles:

- 🐢 Slow Network
- ⚖️ Balanced
- 🎮 Quality
- 📉 Safe Low Bandwidth
- ✨ Local Smoothing

---

# 🚀 Video Optimizations

## Jump-Frame Mode

Suppresses presentation of selected decoded frames to reduce rendering load and perceived stutter on constrained devices.

Frames are fully decoded before being dropped at the output stage, so the H.264/H.265/AV1 bitstream remains valid and no decoder errors are introduced. The host continues encoding and transmitting all frames — this is a client-side rendering optimization, not a network bandwidth reduction.

Modes (frames dropped per 5-frame window):

- Disabled — no frames dropped
- Light — 1 frame dropped per 5 (20% fewer frames rendered)
- Medium — 2 frames dropped per 5 (40% fewer frames rendered)
- Heavy — 3 frames dropped per 5 (60% fewer frames rendered)

Useful for devices with limited GPU/display throughput on unstable connections.

---

## Frame Deduplication

Samples 48 bytes distributed across each incoming decode unit and compares them against the previous frame's sample. When byte-level similarity exceeds the configured threshold, the frame is suppressed at the decoder output stage.

This reduces rendering and display work for visually static content such as paused menus or idle HUDs. Network bytes are not saved — the frame has already arrived before the comparison occurs.

---

## Area Deduplication

At configurable intervals, analyzes a lookback window of recent frames to detect locally repeating patterns (static HUD regions, static backgrounds). When a repeating area is found, the next N frames are dropped before entering the decoder via `queueInputBuffer` with zero size, saving decode CPU for those frames.

This is a CPU optimization, not a network optimization.

---

## Bitrate Optimization

Analyzes frame-level similarity and, when the stream appears stable or redundant, calls `MoonBridge.requestBitrateChange()` to send a bitrate adjustment request to the host via the Moonlight control channel. The host encoder (Sunshine / NVENC) then applies the new target.

This is one of the few optimizations that can affect actual network bandwidth usage, as it operates on the host encoder side.

---

## Block Compression Analysis

Samples 48 bytes from each frame, maps them to a grayscale pseudo-image, and divides that image into a grid. Uniform and non-uniform blocks are counted to estimate visual complexity. The result feeds into the Adaptive Sharpness system.

Note: this analysis works on compressed bitstream bytes, not decoded pixel data. It is a heuristic, not a spatial analysis of the rendered frame.

---

## HUD Detection

Divides the frame conceptually into 32-pixel regions and tracks which regions remain similar across frames. Regions consistently outside the center that show low change are flagged as probable HUD. This information is used to bias Area Deduplication and Adaptive Sharpness away from non-HUD regions.

---

# 🖼️ Adaptive Sharpness

Accumulates a per-frame sharpness estimate over a 30-frame window (~0.5 s at 60 fps). Every 30 frames, the average is mapped to a QP range and applied to the decoder via `MediaCodec.setParameters()` using the `video-qp-p-min` / `video-qp-p-max` keys.

Higher measured sharpness → lower QP target (preserve detail).  
Lower measured sharpness → higher QP target (allow more compression).

Support for these QP parameters varies by device and codec. On unsupported devices the call has no effect.

---

# 🖥️ Local Upscaling

Sun Shadows can stream at a lower resolution and scale up locally on the device using Android's `MediaCodec` scaling modes.

Available modes:

- Bilinear — `VIDEO_SCALING_MODE_SCALE_TO_FIT` (default Android scaler)
- High Quality — `VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING` (hardware high-quality path; quality depends on the device's MediaCodec implementation)

The "Lanczos" label in settings selects the high-quality hardware scaler path. Sun Shadows does not implement a software Lanczos filter.

Example:

```
Stream: 720p
↓
Local Upscaling
↓
Display: 1080p
```

---

# 🎞️ Motion Smoothing

Adjusts the presentation timestamp passed to `releaseOutputBuffer()` using mathematical curves. This changes *when* each frame is displayed relative to its nominal time, smoothing perceived frame pacing.

**This is frame pacing adjustment, not frame interpolation.** No new frames are synthesized. No optical flow is used.

Available curves:

- Linear
- Ease In-Out
- Cubic (Smoothstep)
- Exponential
- Smooth Step (Perlin)

---

# 🔄 Automatic Reconnection

Designed for unstable connections:

- Automatically retries lost connections
- Avoids returning immediately to the PC list
- Improves mobile streaming reliability

---

# 🌐 Connection Support

Compatible with:

- Sunshine
- NVIDIA GameStream (where available)
- Local networks
- Internet streaming
- VPN/P2P solutions such as Tailscale

---

# 📱 Compatibility

Recommended requirements:

- Android device with MediaCodec support
- 4 GB RAM or more recommended
- Hardware video decoding support

---

# 🏗️ Building

Clone the repository, then build the APK:

```
./gradlew assembleDebug
```

The APK will be generated at:

```
app/build/outputs/apk/
```

---

# 🧪 Project Status

Sun Shadows is currently under active development.

Features that are experimental or hardware-dependent:

- Adaptive Sharpness (QP control — device support varies)
- Area Deduplication
- Dynamic Bitrate (requires Sunshine host support)
- Aggressive mobile optimizations

Feedback, testing, and contributions are welcome.

---

# 🙏 Credits

Sun Shadows is based on:

Moonlight Android  
[https://github.com/moonlight-stream/moonlight-android](https://github.com/moonlight-stream/moonlight-android)

Special thanks to the Moonlight developers for creating the foundation of this project.

---

# 📜 License

Sun Shadows follows the same license as the original Moonlight project.
