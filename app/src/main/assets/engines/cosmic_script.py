# Cosmic.inc VisionOS - Python Automation & Cloud Controller
FIREBASE_ENDPOINT = "https://visionos-91949-default-rtdb.asia-southeast1.firebasedatabase.app/visionos.json"
LOUNGE_TITLE = "Cosmic Lounge (Win12 Dock)"
WATERMARK_TEXT = "cosmic.inc"

def filter_bloatware(packages):
    blocked = ["tungsten", "leanback.setup", "partnercustomizer"]
    return [p for p in packages if not any(b in p for b in blocked)]
  
