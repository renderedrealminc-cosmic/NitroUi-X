#ifndef COSMIC_CORE_HPP
#define COSMIC_CORE_HPP

// Cosmic.inc VisionOS Native C++ Architecture Header
namespace CosmicEngine {
    struct LiquidShaderConfig {
        float blurRadius = 38.0f;
        float specularIntensity = 0.92f;
        int targetFps = 60;
        const char* engineWatermark = "powered by cosmic.inc native pipeline";
    };

    inline int computeOptimalHeapSize(int rawMb) {
        return (rawMb * 85) / 100;
    }
}

#endif // COSMIC_CORE_HPP
