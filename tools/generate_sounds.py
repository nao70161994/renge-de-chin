"""Regenerate the original microwave bell and a deep, slow sci-fi oven swing.

The oven sound is an original synthesis, not a sample from any film or game.
"""
import math
import struct
import wave
from pathlib import Path

RATE = 44100
OVEN_DURATION_MS = 1150
assets = Path(__file__).resolve().parents[1] / 'app/src/main/assets'


def bell_samples():
    """Keep the original microwave bell unchanged."""
    samples = []
    for i in range(RATE * 1200 // 1000):
        t = i / RATE
        signal = (math.sin(2 * math.pi * 880 * t)
                  + 0.3 * math.sin(2 * math.pi * 880 * 2.76 * t)
                  + 0.1 * math.sin(2 * math.pi * 880 * 5.4 * t))
        samples.append(int(signal * math.exp(-t * 4.0) * 10000))
    return samples


def oven_samples():
    """Low, slowly swelling 'vwooom': gentle Doppler, harmonic hum, warm whoosh."""
    samples = []
    phase1 = phase2 = phase3 = phase4 = 0.0
    wind_fast = wind_slow = 0.0
    seed = 0xC0FFEE
    for i in range(RATE * OVEN_DURATION_MS // 1000):
        t = i / RATE
        # Avoid the pronounced high-frequency upward sweep of an engine rev.
        frequency = 92 + 17 * math.exp(-((t - 0.39) / 0.33) ** 2) - 10 * t
        phase1 += 2 * math.pi * frequency / RATE
        phase2 += 2 * math.pi * (frequency * 2.009) / RATE
        phase3 += 2 * math.pi * (frequency * 2.988) / RATE
        phase4 += 2 * math.pi * (frequency * 4.013) / RATE
        hum = (0.53 * math.sin(phase1) + 0.35 * math.sin(phase2)
               + 0.18 * math.sin(phase3) + 0.07 * math.sin(phase4))
        flutter = 0.96 + 0.04 * math.sin(2 * math.pi * 19 * t)
        attack = 1 - math.exp(-t * 14)
        fade = math.exp(-max(t - 0.63, 0) * 5.8)
        body = (0.7 + 0.3 * math.exp(-((t - 0.45) / 0.34) ** 2))

        # Two very soft noise filters create air movement, rather than a hiss.
        seed = (1664525 * seed + 1013904223) & 0xFFFFFFFF
        noise = seed / 2147483648 - 1
        wind_fast = 0.91 * wind_fast + 0.09 * noise
        wind_slow = 0.996 * wind_slow + 0.004 * noise
        whoosh = math.exp(-((t - 0.49) / 0.26) ** 2)
        signal = (0.90 * hum * flutter * attack * fade * body
                  + 0.85 * (wind_fast - wind_slow) * whoosh * attack)
        # Make the tail end at zero to avoid a click when the clip finishes.
        signal *= min(1, (OVEN_DURATION_MS / 1000 - t) / 0.035)
        samples.append(max(-32768, min(32767, round(signal * 23500))))
    return samples

def write_wave(path, samples):
    with wave.open(str(path), 'wb') as audio:
        audio.setparams((1, 2, RATE, 0, 'NONE', 'not compressed'))
        audio.writeframes(struct.pack('<' + 'h' * len(samples), *samples))


def main():
    write_wave(assets / 'bell.wav', bell_samples())
    write_wave(assets / 'oven.wav', oven_samples())


if __name__ == '__main__':
    main()
