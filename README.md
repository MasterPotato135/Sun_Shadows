# ☀️ Sun Shadows

> A remote streaming client optimized for unstable mobile networks.

Sun Shadows is a fork of [Moonlight Android](https://github.com/moonlight-stream/moonlight-android) focused on improving game and application streaming over challenging connections such as 4G mobile networks.

While Moonlight is designed for excellent performance on local networks and stable connections, Sun Shadows focuses on adapting streaming for environments with limited bandwidth, higher latency, and packet instability through client-side optimizations.

---

# ✨ Features

## 📶 Mobile Network Optimization

Sun Shadows includes optimizations designed specifically for mobile networks:

- Dynamic bitrate control — when frame analysis detects a stable or redundant stream, calls `MoonBridge.requestBitrateChange()` via the Moonlight control channel. The host encoder (Sunshine / NVENC) applies the new target. This is one of the few optimizations that can reduce actual network bandwidth usage.
- Latency monitoring
- Connection instability handling
- 4G signal monitoring — reads RSRP and SINR from the LTE `SignalStrength` API and automatically scales Jump-Frame mode in real time based on signal degradation. Requires `READ_PHONE_STATE` permission; silently disabled if not granted.
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
- Light — 1 frame dropped per 5 (~20% fewer frames rendered)
- Medium — 2 frames dropped per 5 (~40% fewer frames rendered)
- Heavy — 3 frames dropped per 5 (~60% fewer frames rendered)

When 4G Signal Monitoring is active, Jump-Frame mode is scaled up automatically based on RSRP/SINR readings, floored at the user-configured base mode and capped at Heavy.

---

## Frame Deduplication

Samples 128 bytes distributed across each incoming decode unit and compares them against the previous frame's sample. When byte-level similarity exceeds the configured threshold, the frame is suppressed at the decoder **output** stage — after decoding has already occurred.

This reduces rendering and display work for visually static content such as paused menus or idle HUDs. Network bytes are not saved and decode CPU is not saved — the frame has already been decoded before the comparison occurs.

---

## Area Deduplication

At configurable intervals, divides a 128-byte frame sample into a grid of areas (configurable size, default 8×8) and compares each area against a lookback window of recent frames. When the proportion of stable areas meets or exceeds `areaDedupStableAreaRatioPercent` (fixed at 95% in this version) for a sustained number of consecutive analyses, the next N frames are dropped **before** entering the decoder via `queueInputBuffer` with zero size.

This saves decode CPU for those frames. Any area showing movement prevents the drop. This is a CPU optimization, not a network optimization.

Note: the 128-byte sample comes from the compressed bitstream, not decoded pixels. Area positions in the sample do not correspond to spatial positions in the rendered frame — this is a bitstream entropy heuristic, not spatial analysis.

---

## Bitrate Optimization

Analyzes frame-level similarity using a 128-byte sample. When the stream appears stable or redundant, posts a `MoonBridge.requestBitrateChange()` call to a dedicated background thread (to avoid blocking the JNI callback thread) and sends a bitrate adjustment request to the host via the Moonlight control channel. The host encoder (Sunshine / NVENC) then applies the new target bitrate.

This is one of the few optimizations that can affect actual network bandwidth usage, as it operates on the host encoder side.

---

## Block Compression Analysis

Samples 128 bytes from each frame and maps them to a 16×8 grayscale pseudo-image (16 columns × 8 rows = 128 values). This grid is divided into blocks; uniform and non-uniform blocks are counted to estimate bitstream entropy. The result feeds into the Adaptive Sharpness system.

⚠️ The byte positions in the sample do not correspond to spatial positions in the rendered frame. "Block (3, 2) is uniform" does not mean "region (3, 2) of the screen is uniform." This is a compressed bitstream heuristic, not a spatial analysis of the decoded image. The ProcessingMask and sharpness values derived here should be treated as entropy estimates, not as spatial detail maps.

---

## HUD Detection

Not currently implemented as a standalone component. Referenced in comments as a planned feature that would bias Area Deduplication and Adaptive Sharpness away from non-HUD regions.

---

# 🖼️ Adaptive Sharpness

Accumulates a per-frame sharpness estimate derived from Block Compression Analysis over a 30-frame window (~0.5 s at 60 fps). Every 30 frames, the average is mapped to a QP range and applied to the **decoder** via `MediaCodec.setParameters()` using the `video-qp-p-min` / `video-qp-p-max` keys (range: QP 10–40).

Higher measured entropy → lower QP target (preserve detail).  
Lower measured entropy → higher QP target (allow more compression).

Because the sharpness estimate is derived from compressed bitstream bytes rather than decoded pixels, the QP values reflect bitstream entropy, not visual sharpness of the rendered image. Support for `video-qp-p-min/max` varies by device and codec — on unsupported devices the call has no effect.

---

# 🖥️ Local Upscaling

Sun Shadows can stream at a lower resolution and scale up locally using Android's `MediaCodec` video scaling modes.

Available modes:

- Bilinear — `VIDEO_SCALING_MODE_SCALE_TO_FIT` (default Android scaler)
- High Quality — `VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING` (hardware high-quality path; quality depends on the device's MediaCodec implementation)

The "Lanczos" label in settings selects the `SCALE_TO_FIT_WITH_CROPPING` hardware path. Sun Shadows does not implement a software Lanczos filter — the name refers to the hardware scaler mode, whose actual algorithm depends on the device.

Example:

```
Stream: 720p → Local Upscaling → Display: 1080p
```

---

# 🎞️ Motion Smoothing

Adjusts the presentation timestamp passed to `releaseOutputBuffer(index, renderTimeNanos)` using a `FramePacingController` that measures inter-frame intervals, calculates average timing and jitter, and schedules each frame to reduce microstutter.

**This is frame pacing adjustment, not frame interpolation.** No new frames are synthesized. No optical flow is used. The effect is smoother perceived frame rhythm, not an increase in actual frame rate.

Three pacing modes are selected automatically from preferences:

- Low Latency — minimal adjustment, prioritizes responsiveness
- Balanced — moderate smoothing, driven by Choreographer vsync
- Smooth — stronger correction, higher latency tolerance

Available interpolation curve types (affect timestamp offset shape):

- None
- Linear
- Ease In-Out
- Cubic (Smoothstep)
- Exponential
- Smooth Step (Perlin)

---

# 🔄 Automatic Reconnection

Designed for unstable connections:

- On a recoverable network error, re-launches the Game activity with an incremented reconnect attempt counter instead of returning to the PC list
- Configurable maximum number of attempts (`autoReconnectAttempts`)
- Disabled automatically if the error is not classified as recoverable

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

- Adaptive Sharpness (QP control via `video-qp-p-min/max` — device support varies; derived from bitstream entropy, not pixel analysis)
- Area Deduplication (bitstream heuristic — not spatial analysis)
- Dynamic Bitrate (requires Sunshine host support)
- 4G Signal Monitoring (requires `READ_PHONE_STATE` permission)
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