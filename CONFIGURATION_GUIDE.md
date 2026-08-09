# Sun Shadows — Configuration Guide

This guide explains every setting available in the app, what it actually does in the current code, and how to configure it for common scenarios.

Settings marked **🚧 Future** are visible in the UI but not yet read or used by the app — they have no effect in this version.

---

## 📋 Table of Contents

1. [Basic Settings](#1-basic-settings)
2. [Mobile Network Settings](#2-mobile-network-settings)
3. [Area Deduplication](#3-area-deduplication)
4. [Audio Settings](#4-audio-settings)
5. [Gamepad Settings](#5-gamepad-settings)
6. [Input Settings](#6-input-settings)
7. [On-Screen Controls](#7-on-screen-controls)
8. [Host Settings](#8-host-settings)
9. [UI Settings](#9-ui-settings)
10. [Advanced Settings](#10-advanced-settings)
11. [Scenario Presets](#11-scenario-presets)

---

## 1. Basic Settings

### Resolution
**Default:** 1280×720

Sets the resolution requested from the host encoder. Lower resolution reduces bandwidth and decode load on the device. Combined with Local Upscaling, you can stream at 720p and display at 1080p.

Options: 640×360, 854×480, 1280×720, 1920×1080, 2560×1440, 3840×2160, Native.

> Lower resolution is the single most effective way to reduce bandwidth on 4G. Start here before touching any other setting.

---

### FPS
**Default:** 60

Target frame rate requested from the host. The host encoder targets this value; actual delivered FPS depends on network and host GPU.

> On unstable 4G, 30 FPS at higher resolution is often better than 60 FPS with constant stuttering.

---

### Bitrate
**Default:** auto (calculated from resolution + FPS)  
**Range:** 0.5 Mbps – 150 Mbps, step 0.5 Mbps

Sets the initial bitrate sent to the host encoder at stream start. If Bitrate Optimization is enabled, this value is adjusted dynamically during the session via the control channel.

> For 4G: start at 5–10 Mbps for 720p60. Increase only if the connection is stable.

---

### Frame Pacing
**Default:** Balanced

Controls how decoded frames are scheduled for display.

| Option | Behavior |
|--------|----------|
| Minimum Latency | Frames displayed as soon as decoded. Lowest latency, most stutter. |
| Balanced | Uses Choreographer vsync to align frame delivery. Best general-purpose choice. |
| Cap FPS | Limits render rate to match stream FPS. Reduces GPU load. |
| Maximum Smoothness | Prioritizes smooth frame rhythm over latency. |

> For 4G gaming: **Balanced**. For remote desktop / low-motion use: **Minimum Latency**.

---

### Stretch to Fill
**Default:** Off

Stretches the video to fill the screen, ignoring aspect ratio. Off by default to preserve correct proportions.

---

## 2. Mobile Network Settings

### Mobile Network Optimizations
**Default:** On

Master switch that enables the mobile network optimization stack. When off, the app behaves closer to stock Moonlight.

> Leave this on for any 4G or unstable connection.

---

### Jump-Frame Mode
**Default:** Off  
**Options:** Off, Light, Medium, Heavy

Drops decoded frames at the render output stage to reduce GPU/display load. The host still encodes and sends all frames — this does not save bandwidth.

| Mode | Frames dropped | Rendered |
|------|---------------|----------|
| Off | 0 of 5 | 100% |
| Light | 1 of 5 | ~80% |
| Medium | 2 of 5 | ~60% |
| Heavy | 3 of 5 | ~40% |

When **4G Signal Monitoring** is also enabled, this setting acts as a **floor** — the app can scale up automatically based on RSRP/SINR but will never go below the mode you set here.

> Start with **Light** if the device feels warm or laggy. Use **Medium/Heavy** only on very weak connections.

---

### Target Latency
**Default:** 50 ms  
**Range:** 20–200 ms, step 5 ms

Target end-to-end latency hint used by the stream pacing system. Lower values prioritize responsiveness; higher values give the system more headroom to absorb jitter.

> For 4G with variable latency: 80–120 ms. For local Wi-Fi: 20–50 ms.

---

### Adaptive Resolution
**Default:** Off

When enabled, allows the app to request a resolution reduction from the host during periods of network stress. Requires Sunshine host support.

> Experimental. Keep off unless you are specifically testing adaptive resolution behavior.

---

### Bitrate Optimization
**Default:** On

Analyzes frame-level similarity using a 128-byte sample every N milliseconds (see Analysis Interval below). When frames appear redundant or the stream stable, sends a `requestBitrateChange()` to the host via the Moonlight control channel. The host encoder (Sunshine / NVENC) applies the new target.

This is one of the few settings that actually reduces network bandwidth — the change happens at the host encoder.

> Keep on for 4G. Turn off only if you notice the bitrate fluctuating in ways that cause quality drops on fast-moving scenes.

---

### Analysis Interval
**Default:** 50 ms  
**Range:** 10–200 ms, step 5 ms  
**Requires:** Bitrate Optimization = On

How often the frame similarity analysis runs, in milliseconds. This interval also currently controls how often Frame Deduplication analysis runs.

| Value | Effect |
|-------|--------|
| 10–30 ms | Very frequent analysis. Higher CPU use. More responsive to scene changes. |
| 50 ms (default) | ~20 checks/second. Good balance. |
| 100–200 ms | Less frequent. Lower CPU use. Slower to react to scene changes. |

> Lower values react faster but cost more CPU. On mid-range devices, 50 ms is the right default. On weak devices, try 80–100 ms.

---

### Frame Similarity Threshold
**Default:** 85%  
**Range:** 50–100%, step 5%  
**Requires:** Bitrate Optimization = On

Minimum byte-level similarity between two consecutive 128-byte frame samples for a frame to be considered redundant. When similarity exceeds this threshold, the frame is suppressed at the decoder output and a bitrate reduction may be triggered.

| Value | Effect |
|-------|--------|
| 50–70% | Very aggressive. Drops frames even with moderate motion. May cause visual artifacts. |
| 85% (default) | Balanced. Drops only near-static frames. |
| 95–100% | Conservative. Only drops truly identical frames. |

> Do not go below 75% unless you are specifically targeting idle/menu-heavy scenarios. The similarity is measured on compressed bytes, not pixels — the relationship to visual similarity is approximate.

---

### Local Upscaling
**Default:** Off

Streams at the configured resolution and scales up to the display resolution locally using the Android MediaCodec hardware scaler.

**Example:** Stream at 720p, display at 1080p — decode cost of 720p, display quality of 1080p (hardware-dependent).

---

### Upscaling Mode
**Requires:** Local Upscaling = On  
**Default:** Bilinear

| Mode | API used | Notes |
|------|----------|-------|
| Bilinear | `VIDEO_SCALING_MODE_SCALE_TO_FIT` | Standard Android scaler. Consistent across devices. |
| Lanczos (High Quality) | `VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING` | Hardware high-quality path. Actual algorithm depends on the device's MediaCodec implementation. May crop edges slightly. |

> "Lanczos" is the label for the hardware high-quality mode — Sun Shadows does not implement a software Lanczos filter. Quality varies by device.

---

### Prefer Audio Over Video
**Default:** Off

When the device is under load, prioritizes audio continuity over video frame delivery. Useful for music streaming or voice-heavy content where audio dropouts are more disruptive than video stutters.

---

### Motion Smoothing Mode
**Default:** Off

Adjusts the presentation timestamp of each frame via `releaseOutputBuffer(index, renderTimeNanos)` to smooth perceived frame pacing. **No new frames are created.** This is frame timing adjustment, not interpolation.

| Option | Effect |
|--------|--------|
| Off | Frames displayed at decode time. No adjustment. |
| Low Latency | Minimal pacing adjustment, lowest added latency. |
| Balanced | Moderate smoothing using Choreographer vsync. |
| Smooth | Stronger rhythm correction, higher latency tolerance. |

---

### Interpolation Curve
**Default:** Linear

Shape of the timestamp offset applied per frame when Motion Smoothing is active.

| Curve | Character |
|-------|-----------|
| None | No curve applied. |
| Linear | Constant adjustment rate. |
| Ease In-Out | Slow start and end, faster middle. |
| Cubic (Smoothstep) | Smooth S-curve. |
| Exponential | Rapid initial adjustment, slow tail. |
| Smooth Step (Perlin) | Perlin-style smooth interpolation. |

> For most users: **Linear** or **Ease In-Out**. The differences are subtle and device-dependent.

---

### Transition Strength
**Default:** 50%  
**Range:** 0–100%, step 5%

Intensity of the pacing adjustment. 0% = no effect (same as Off). 100% = maximum timestamp shift per frame. Values above 50% automatically select **Smooth** pacing mode; values 1–50% select **Balanced**.

---

### Transition Frequency
**Default:** 16 ms  
**Range:** 1–100 ms, step 1 ms

Minimum interval between pacing adjustments. 16 ms ≈ 60 Hz. Lower values allow more frequent corrections but increase CPU overhead.

> Keep at 16 ms for 60 FPS streams. Set to 33 ms for 30 FPS streams.

---

### Frame Deduplication
**Default:** Off

Compares a 128-byte sample of each incoming frame against the previous frame. When byte-level similarity exceeds the Frame Similarity Threshold, the decoded frame is suppressed at the **output** stage — after decoding has already occurred.

Saves GPU/display work on static content (menus, paused screens). Does not save decode CPU or network bandwidth.

> Pair with Bitrate Optimization for the best result: Bitrate Optimization reduces what arrives; Frame Deduplication reduces what gets rendered.

---

### Local Motion Smoothing
**Default:** Off

Legacy switch that enables the **Low Latency** pacing mode. Equivalent to setting Motion Smoothing Mode to Low Latency with default strength. Kept for compatibility.

---

### Auto Reconnect
**Default:** On

On a recoverable network error, re-launches the stream session instead of returning to the PC list. The reconnect attempt counter is passed between sessions; the app gives up after the configured number of attempts.

---

### Reconnect Attempts
**Default:** 3  
**Range:** 1–5, step 1  
**Requires:** Auto Reconnect = On

Maximum number of reconnection attempts before the app returns to the PC list.

> For 4G: set to 5. Mobile connections can recover within a few seconds after a brief drop.

---

### 4G Signal Monitoring
**Default:** Off

Reads RSRP (signal power) and SINR (signal quality) from the Android LTE `SignalStrength` API and automatically scales Jump-Frame Mode up when the signal degrades. Requires `READ_PHONE_STATE` permission; silently disabled if not granted.

Scaling table:

| RSRP | Action |
|------|--------|
| ≥ −95 dBm (good) | Restores your configured Jump-Frame base mode |
| ≥ −105 dBm (medium) | Minimum Light |
| ≥ −115 dBm (poor) | Minimum Medium |
| < −115 dBm (critical) | Forces Heavy |

If SINR is also available and below 3 dB (very noisy), the mode is pushed one level higher.

If RSRP is unavailable, falls back to Android's general signal level (0–4).

> Enable this if you move around while streaming (vehicle, commute). It reacts to signal changes in real time without requiring manual adjustment.

---

### 🚧 4G Packet Loss Recovery
**Future — no effect in current version.**

---

### 🚧 4G Buffer Size
**Future — no effect in current version.**

---

### 🚧 4G Dynamic Bitrate
**Future — no effect in current version.**  
*(Note: dynamic bitrate via frame analysis already exists under Bitrate Optimization above — this setting is a separate planned mechanism.)*

---

### 🚧 Jitter Correction
**Future — no effect in current version.**

---

### 🚧 Error Correction (FEC)
**Future — no effect in current version.**

---

### 🚧 Maximum 4G Latency
**Future — no effect in current version.**

---

### 🚧 Bitrate Smoothing
**Future — no effect in current version.**

---

### 🚧 Network Priority
**Future — no effect in current version.**

---

### 🚧 Frame Drop Threshold
**Future — no effect in current version.**

---

### 🚧 Quality Adaptation
**Future — no effect in current version.**

---

### 🚧 Codec Preference (4G)
**Future — no effect in current version.**  
*(Codec selection already exists under Video Format in Advanced Settings.)*

---

### 🚧 Connection Recovery (4G)
**Future — no effect in current version.**  
*(Basic reconnection already exists under Auto Reconnect above.)*

---

### 🚧 Recovery Timeout
**Future — no effect in current version.**

---

### 🚧 Packet Prioritization
**Future — no effect in current version.**

---

### 🚧 Congestion Control
**Future — no effect in current version.**

---

### 🚧 Advanced Error Correction
**Future — no effect in current version.**

---

### 🚧 Minimum Bandwidth
**Future — no effect in current version.**

---

### 🚧 Smooth Handover (4G/Wi-Fi)
**Future — no effect in current version.**

---

### 🚧 Stream Profile
**Future — no effect in current version.**

---

## 3. Area Deduplication

Analyzes a 128-byte frame sample divided into a grid of areas and compares each area against a lookback window of recent frames. When enough areas are stable for enough consecutive checks, the next N frames are dropped **before** entering the decoder via `queueInputBuffer` with zero size — saving actual decode CPU.

⚠️ The sample comes from the compressed bitstream, not decoded pixels. Area positions in the sample do not map to spatial positions on screen. This is a bitstream entropy heuristic.

---

### Enable Area Deduplication
**Default:** Off

Master switch. All settings below require this to be on.

---

### Check Interval
**Default:** 10 frames  
**Range:** 2–60 frames, step 1

How many frames to wait between each analysis pass. At 60 FPS, the default of 10 means analysis runs ~6 times per second.

| Value | Effect |
|-------|--------|
| 2–5 | Very frequent. Reacts quickly to motion. Higher CPU cost. |
| 10 (default) | Balanced. |
| 30–60 | Infrequent. Lower CPU cost. Slower to detect motion resuming. |

---

### Lookback Frames
**Default:** 5  
**Range:** 2–30, step 1

How many past frames each area is compared against to confirm it is stable. Higher values require the area to be stable for longer before a drop is authorized.

| Value | Effect |
|-------|--------|
| 2–3 | Fast to authorize a drop. Risk of dropping during brief pauses in motion. |
| 5 (default) | Reasonable confidence before dropping. |
| 15–30 | Very conservative. Only drops after long static periods. |

---

### Replace Frames
**Default:** 3  
**Range:** 1–20, step 1

How many consecutive frames are dropped once a stable pattern is confirmed. The last decoded frame stays on screen during these drops.

| Value | Effect |
|-------|--------|
| 1–2 | Conservative drops. Almost no visual impact. |
| 3 (default) | Balanced. Saves ~3 decode cycles per trigger. |
| 10–20 | Aggressive. Only safe for fully static content (paused game, idle HUD). |

> If you notice the screen freezing briefly on content that isn't actually static, reduce this value first.

---

### Similarity Threshold
**Default:** 90%  
**Range:** 50–100%, step 5%

Minimum similarity between an area's current sample and its lookback samples for that area to be counted as stable. Only areas meeting this threshold count toward the stable area ratio (fixed at 95% of all areas).

| Value | Effect |
|-------|--------|
| 50–70% | Very permissive. Counts areas as stable even with significant change. Risk of false drops. |
| 90% (default) | Requires high similarity. Safe for most content. |
| 100% | Byte-perfect match required. Almost never triggers except on truly static frames. |

---

### Grid Size
**Default:** 8 areas  
**Range:** 2–32, step 1

Number of areas the 128-byte sample is divided into. Each area gets an equal slice of the 128 bytes.

| Value | Effect |
|-------|--------|
| 2–4 | Coarse grid. Less sensitive to localized motion. |
| 8 (default) | 16 bytes per area. Good granularity for a 128-byte sample. |
| 16–32 | Fine grid. Very sensitive. At 32 areas, each area is only 4 bytes — high noise risk. |

> Do not go above 16. At 32 areas with 128 bytes total, each area is 4 bytes and the comparison becomes noise-dominated.

---

## 4. Audio Settings

### Audio Configuration
**Default:** Stereo

| Option | Channels |
|--------|----------|
| Stereo | 2.0 |
| 5.1 Surround | 5.1 |
| 7.1 Surround | 7.1 |

> Use Stereo on 4G. Surround audio increases bandwidth and is rarely useful on mobile.

---

### Enable Audio Effects
**Default:** Off

Applies Android AudioFX processing to the audio output. Effect depends on the device's AudioFX implementation.

---

## 5. Gamepad Settings

### Stick Deadzone
**Default:** 7%  
**Range:** 0–20%, step 1%

Minimum analog stick displacement before input is registered. Increase if your controller drifts at rest.

---

### Multi-Controller Support
**Default:** On

Allows multiple controllers to be connected simultaneously and mapped to separate players.

---

### Xbox USB Driver
**Default:** On

Uses a built-in USB HID driver for Xbox controllers. Enables features (like rumble) that the generic Android driver may not support.

---

### Bind All USB Devices
**Default:** Off  
**Requires:** Xbox USB Driver = On

Binds the USB driver to all connected USB devices, not just recognized controllers. Enable if your controller is not detected.

---

### Mouse Emulation
**Default:** On

Allows a connected gamepad to emulate mouse input on the host.

---

### Analog Stick Scrolling
**Default:** Right stick  
**Requires:** Mouse Emulation = On

Which analog stick is used for scrolling when in mouse emulation mode.

| Option | Stick |
|--------|-------|
| None | Disabled |
| Right | Right stick scrolls |
| Left | Left stick scrolls |

---

### Vibrate Fallback to Device
**Default:** Off

When the host sends rumble commands and the controller does not support rumble, vibrates the Android device itself instead.

---

### Fallback Vibration Strength
**Default:** 100%  
**Range:** 0–200%, step 1%  
**Requires:** Vibrate Fallback = On

Strength of the device vibration used as rumble fallback.

---

### Flip Face Buttons
**Default:** Off

Swaps A/B and X/Y button positions. Useful when switching between Xbox and Nintendo controller layouts.

---

### Gamepad Touchpad as Mouse
**Default:** Off

Maps the physical touchpad on DualShock/DualSense controllers to mouse movement on the host.

---

### Gamepad Motion Sensors
**Default:** On

Forwards motion sensor data (gyroscope, accelerometer) from the controller to the host. Required for gyro aiming in supported games.

---

### Motion Sensor Fallback to Device
**Default:** Off

If the controller has no motion sensors, uses the Android device's sensors instead.

---

## 6. Input Settings

### Touchscreen Trackpad Mode
**Default:** On

Treats the touchscreen as a relative trackpad (like a laptop touchpad) rather than an absolute touch surface. Recommended for game streaming.

---

### Mouse Navigation Buttons
**Default:** Off

Sends the Android back/forward navigation buttons as mouse buttons 4 and 5 to the host.

---

### Absolute Mouse Mode
**Default:** Off

Maps touch input to absolute screen coordinates on the host instead of relative movement. Useful for desktop/remote-desktop use cases where you want to tap exactly where you touch.

---

## 7. On-Screen Controls

### Show On-Screen Controls
**Default:** Off

Displays a virtual gamepad overlay on the screen.

---

### Vibrate On-Screen Controls
**Default:** On  
**Requires:** On-Screen Controls = On

Vibrates the device when on-screen buttons are pressed.

---

### Only Show L3/R3
**Default:** Off  
**Requires:** On-Screen Controls = On

Hides all on-screen buttons except L3 and R3 (stick clicks). Useful if you have a physical controller but need stick click access.

---

### Show Guide Button
**Default:** On  
**Requires:** On-Screen Controls = On

Shows the Xbox Guide / Home button in the on-screen overlay.

---

### OSC Opacity
**Default:** 90%  
**Range:** 0–100%, step 1%  
**Requires:** On-Screen Controls = On

Transparency of the on-screen control overlay.

---

### Reset On-Screen Controls
Resets the position and layout of all on-screen controls to defaults.

---

## 8. Host Settings

### Optimize Game Settings (SOPS)
**Default:** On

Allows the Sunshine/NVIDIA host to automatically optimize in-game settings for streaming (resolution, frame rate, etc.).

---

### Play Audio on Host
**Default:** Off

Keeps audio playing on the host PC while streaming. When off, audio is redirected exclusively to the Android device.

---

## 9. UI Settings

### Picture-in-Picture
**Default:** Off

Enables Android PiP mode, allowing the stream to continue in a small floating window when you switch apps.

---

### Language
**Default:** System default

Overrides the app language independently of the Android system language.

---

### Small Icon Mode
**Default:** Auto (on for small screens)

Uses smaller icons in the PC/app list. Enabled automatically on phones; disabled on tablets and TVs.

---

## 10. Advanced Settings

### Unlock FPS
**Default:** Off

Removes the FPS cap and allows the stream to render above the display's refresh rate. Useful on high-refresh-rate displays (90/120/144 Hz).

---

### Reduce Refresh Rate to Match Stream
**Default:** Off

Reduces the display refresh rate to match the stream's FPS. Can reduce tearing and power usage on OLED displays.

---

### Disable Performance Warnings
**Default:** Off

Suppresses in-app warning toasts about performance or connection issues.

---

### Video Format
**Default:** Auto

Forces the video codec used for decoding.

| Option | Notes |
|--------|-------|
| Auto | Host and client negotiate the best available codec. Recommended. |
| Force AV1 | Best compression. Requires AV1 hardware decoder on the device. |
| Force H.265/HEVC | Better compression than H.264. Widely supported. |
| Force H.264 | Maximum compatibility. Higher bandwidth than HEVC/AV1. |

> On 4G: prefer **Auto** or **Force H.265** if your device supports it. AV1 saves the most bandwidth but requires a capable host and device.

---

### Enable HDR
**Default:** Off

Requests an HDR stream from the host. Requires HDR-capable host GPU, Sunshine support, and an HDR display on the device. Increases bandwidth significantly.

---

### Full Color Range
**Default:** Off

Requests full (0–255) color range instead of limited (16–235). Only enable if your display and media pipeline support full range end-to-end.

---

### Performance Overlay
**Default:** Off

Shows a real-time overlay with frame rate, decode time, network stats, and other performance metrics during streaming.

---

### Post-Stream Latency Toast
**Default:** Off

Displays a summary toast after the stream ends showing average latency statistics.

---

## 11. Scenario Presets

These are recommended starting points, not guaranteed optimal values. Adjust from here based on your device and connection.

---

### Stable 4G / Good Signal (RSRP ≥ −95 dBm)

| Setting | Value |
|---------|-------|
| Resolution | 1280×720 |
| FPS | 60 |
| Bitrate | 8–12 Mbps |
| Frame Pacing | Balanced |
| Mobile Network Optimizations | On |
| Jump-Frame Mode | Off |
| Bitrate Optimization | On |
| Analysis Interval | 50 ms |
| Frame Similarity Threshold | 85% |
| Auto Reconnect | On |
| Reconnect Attempts | 3 |
| 4G Signal Monitoring | On |
| Video Format | Auto or H.265 |

---

### Weak 4G / Variable Signal

| Setting | Value |
|---------|-------|
| Resolution | 1280×720 |
| FPS | 30 |
| Bitrate | 4–6 Mbps |
| Frame Pacing | Balanced |
| Mobile Network Optimizations | On |
| Jump-Frame Mode | Light |
| Target Latency | 100 ms |
| Bitrate Optimization | On |
| Analysis Interval | 50 ms |
| Frame Similarity Threshold | 80% |
| Frame Deduplication | On |
| Auto Reconnect | On |
| Reconnect Attempts | 5 |
| 4G Signal Monitoring | On |
| Video Format | H.265 |

---

### Critical Signal / Moving Vehicle

| Setting | Value |
|---------|-------|
| Resolution | 854×480 |
| FPS | 30 |
| Bitrate | 2–4 Mbps |
| Frame Pacing | Minimum Latency |
| Mobile Network Optimizations | On |
| Jump-Frame Mode | Medium |
| Target Latency | 150 ms |
| Bitrate Optimization | On |
| Analysis Interval | 80 ms |
| Frame Similarity Threshold | 75% |
| Frame Deduplication | On |
| Area Deduplication | On |
| Area Dedup Replace Frames | 5 |
| Auto Reconnect | On |
| Reconnect Attempts | 5 |
| 4G Signal Monitoring | On |
| Video Format | H.265 |

---

### Static / Low-Motion Content (Remote Desktop, Menus)

| Setting | Value |
|---------|-------|
| Resolution | 1920×1080 |
| FPS | 30 |
| Bitrate | 5–8 Mbps |
| Frame Pacing | Balanced |
| Bitrate Optimization | On |
| Frame Similarity Threshold | 90% |
| Frame Deduplication | On |
| Area Deduplication | On |
| Area Dedup Check Interval | 5 frames |
| Area Dedup Lookback Frames | 5 |
| Area Dedup Replace Frames | 8 |
| Area Dedup Similarity Threshold | 92% |
| Video Format | Auto |

---

### Local Wi-Fi / Low Latency Gaming

| Setting | Value |
|---------|-------|
| Resolution | 1920×1080 or Native |
| FPS | 60 (or unlock FPS) |
| Bitrate | 20–50 Mbps |
| Frame Pacing | Minimum Latency |
| Mobile Network Optimizations | Off |
| Jump-Frame Mode | Off |
| Bitrate Optimization | Off |
| Frame Deduplication | Off |
| Area Deduplication | Off |
| Video Format | Auto |
| HDR | On (if supported) |