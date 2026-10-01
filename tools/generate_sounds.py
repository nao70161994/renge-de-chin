"""Regenerate the original bell / oven waveforms; no runtime synthesis needed."""
import math
import struct
import wave
from pathlib import Path

RATE = 44100
assets = Path(__file__).resolve().parents[1] / 'app/src/main/assets'
for name, duration in [('bell', 1200), ('oven', 250)]:
    samples = []
    p1 = p2 = p3 = 0.0
    for i in range(RATE * duration // 1000):
        t = i / RATE
        if name == 'bell':
            signal = (math.sin(2 * math.pi * 880 * t)
                      + 0.3 * math.sin(2 * math.pi * 880 * 2.76 * t)
                      + 0.1 * math.sin(2 * math.pi * 880 * 5.4 * t))
            envelope = math.exp(-t * 4.0)
        else:
            dp = 2 * math.pi * (500 * math.exp(-t * 12) + 80) / RATE
            p1 += dp
            p2 += dp * 1.5
            p3 += dp * 2.0
            signal = math.sin(p1) + 0.5 * math.sin(p2) + 0.25 * math.sin(p3)
            envelope = min(t * 60, 1.0) * math.exp(-t * 8)
        samples.append(int(signal * envelope * 10000))
    with wave.open(str(assets / (name + '.wav')), 'wb') as audio:
        audio.setparams((1, 2, RATE, 0, 'NONE', 'not compressed'))
        audio.writeframes(struct.pack('<' + 'h' * len(samples), *samples))
