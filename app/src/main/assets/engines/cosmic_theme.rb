# Cosmic.inc VisionOS - Ruby Liquid Theme Controller
module CosmicTheme
  PALETTES = [
    { name: "Cosmic Nebula", primary: "#00E5FF", secondary: "#7C3AED" },
    { name: "Cyber Crimson", primary: "#FF2E93", secondary: "#FF8A00" },
    { name: "Aurora Emerald", primary: "#00FF9D", secondary: "#00B8FF" },
    { name: "Deep Frost", primary: "#38BDF8", secondary: "#6366F1" }
  ]

  def self.current_watermark
    "cosmic.inc • multi-engine os"
  end
end
