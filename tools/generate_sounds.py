"""Regenerate the original microwave bell.

The user-approved F2 oven effect (0.245 seconds) is a mastered WAV asset and
must not be overwritten with an earlier synthesized experiment. Its SHA-256 is
checked by the tests. To replace it, update oven.wav and its verified checksum.
"""
import math
import struct
import wave
from pathlib import Path

RATE = 44100
assets = Path(__file__).resolve().parents[1] / "app/src/main/assets"


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


def write_wave(path, samples):
    with wave.open(str(path), "wb") as audio:
        audio.setparams((1, 2, RATE, 0, "NONE", "not compressed"))
        audio.writeframes(struct.pack("<" + "h" * len(samples), *samples))


def main():
    # Intentionally leave oven.wav untouched: F2 is the selected, mastered sound.
    write_wave(assets / "bell.wav", bell_samples())


if __name__ == "__main__":
    main()
