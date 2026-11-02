package com.example.ui.camera.filters

/**
 * Supported real-time GPU Shader types for the Camera system.
 * Supports both Android 13+ AGSL (Android Graphics Shading Language) RuntimeShader
 * and hardware-accelerated Compose ColorMatrix / Canvas Shader fallbacks.
 */
enum class CameraShaderType(val displayName: String, val badge: String) {
    NONE("Normal", "⚡"),
    CHROMATIC_ABERRATION("RGB Split / Chromatic", "🌈"),
    NEON_CYBER("Cyber Neon Edge", "🔮"),
    VHS_RETRO("VHS 90s CRT Wave", "📼"),
    FISHEYE("Fisheye Action Lens", "🐟"),
    THERMAL_VISION("Thermal Heatmap FLIR", "🔥"),
    HALFTONE_COMIC("Halftone Pop Comic", "📰"),
    GLITCH_DISTORTION("Digital Cyber Glitch", "⚡"),
    NOIR_FILM("Noir 35mm Silver", "🎬"),
    TEAL_ORANGE("Teal & Orange Grade", "🌅"),
    EMERALD_MATRIX("Emerald Matrix Code", "🟩"),
    VINTAGE_KODACHROME("Vintage Kodachrome", "🎞️")
}
