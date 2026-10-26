"""Regenerate the original microwave bell and an original sci-fi oven swoosh.

The new oven effect is a synthesized pitch sweep, not an extracted soundtrack
or a sample of any commercial sound effect. No runtime synthesis is needed.
"""
import math
import struct
import wave
from pathlib import Path

RATE = 44100
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
    """Short, laser-sword-like 'vwoom': falling harmonics and airy sweep."""
    samples = []
    phase = phase2 = phase3 = 0.0
    wind = 0.0
    seed = 0xC0FFEE
    for i in range(RATE * 750 // 1000):
        t = i / RATE
        frequency = (120 + 350 * math.exp(-((t - 0.125) / 0.115)**2)
                     + 80 * math.exp(-t * 8))
        phase += 2 * math.pi * frequency / RATE
        phase2 += 2 * math.pi * (frequency * 1.995) / RATE
        phase3 += 2 * math.pi * (frequency * 3.01) / RATE
        hum = (0.66 * math.sin(phase) + 0.28 * math.sin(phase2)
               + 0.16 * math.sin(phase3))
        flutter = 0.88 + 0.12 * math.sin(2 * math.pi * 28 * t)
        attack = min(t / 0.018, 1.0)
        fade = math.exp(-max(t - 0.18, 0) * 5.4)
        # Deterministic filtered noise adds a smooth swishing movement.
        seed = (1664525 * seed + 1013904223) & 0xFFFFFFFF
        noise = seed / 2147483648 - 1
        wind = wind * 0.90 + noise * 0.10
        sweep = math.exp(-((t - 0.18) / 0.125)**2)
        signal = 0.68 * hum * flutter * attack * fade + 1.1 * wind * sweep
        samples.append(max(-32768, min(32767, round(signal * 24500))))
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
