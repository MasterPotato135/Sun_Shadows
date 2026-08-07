
# ☀️ Sun Shadows

> A remote streaming client optimized for unstable mobile networks.

Sun Shadows is a fork of [Moonlight Android](https://github.com/moonlight-stream/moonlight-android) focused on improving game and application streaming over challenging connections such as 4G mobile networks.

While Moonlight is designed for excellent performance on local networks and stable connections, Sun Shadows focuses on adapting streaming for environments with limited bandwidth, higher latency, and packet instability through client-side optimizations.

---

# ✨ Features

## 📶 Mobile Network Optimization

Sun Shadows includes optimizations designed specifically for mobile networks:

- Adaptive bitrate control
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

Allows the client to skip selected frames when necessary to reduce bandwidth usage.

Modes:

- Disabled
- Light (~20% bandwidth saving)
- Medium (~40% bandwidth saving)
- Heavy (~60% bandwidth saving)

Useful for unstable or limited connections.

---

## Area Deduplication

Analyzes repeated local patterns in frames to reduce unnecessary processing.

Useful for:

- Game HUDs
- Menus
- Static interfaces
- Repeated visual elements

Instead of processing identical areas repeatedly, Sun Shadows can reuse previous information when possible.

---

## Bitrate Optimization

Analyzes frames locally and adjusts streaming behavior based on image similarity.

Goals:

- Reduce unnecessary bandwidth usage
- Improve stability on mobile networks
- Maintain visual quality when possible

---

# 🖥️ Local Upscaling

Sun Shadows can reduce network usage by streaming at a lower resolution and increasing the resolution locally on the device.

Available filters:

- Bilinear
- Lanczos
- HUD Detection

Example:



Stream: 420p
↓
Local Upscaling
↓
Display: 720p / 1080p

This allows lower bandwidth usage while keeping a better visual experience.



# 🎞️ Motion Smoothing

Experimental features to improve perceived smoothness:

- Transition frames
- Mathematical interpolation
- Local motion adjustments

Available interpolation methods:

- Linear
- Ease In-Out
- Cubic
- Exponential
- Smooth Step


# 🔄 Automatic Reconnection

Designed for unstable connections:

- Automatically retries lost connections
- Avoids returning immediately to the PC list
- Improves mobile streaming reliability



# 🌐 Connection Support

Compatible with:

- Sunshine
- NVIDIA GameStream (where available)
- Local networks
- Internet streaming
- VPN/P2P solutions such as Tailscale


# 📱 Compatibility

Recommended requirements:

- Android device with MediaCodec support
- 4GB RAM or more recommended
- Hardware video decoding support


# 🏗️ Building

Clone the repository:


Build the APK:


./gradlew assembleDebug


The APK will be generated at:


app/build/outputs/apk/




# 🧪 Project Status

Sun Shadows is currently under active development.

Some features are experimental:

* Area deduplication
* Frame interpolation
* Aggressive mobile optimizations
* Adaptive streaming logic

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

See:




for more information.

