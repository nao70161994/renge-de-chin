"""Regression checks for the microwave bell and user-selected F2 oven WAV."""
import hashlib
import importlib.util
import struct
import unittest
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
F2_SHA256 = "12364fbecf5777794fe15eb9336bf210cbd753142745a33abb9d93828ff361e2"

spec = importlib.util.spec_from_file_location(
    "generate_sounds", ROOT / "tools" / "generate_sounds.py")
sounds = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sounds)


class SoundAssetTests(unittest.TestCase):
    def test_microwave_bell_matches_generator(self):
        expected = sounds.bell_samples()
        with wave.open(str(ASSETS / "bell.wav"), "rb") as audio:
            self.assertEqual(audio.getnchannels(), 1)
            self.assertEqual(audio.getsampwidth(), 2)
            self.assertEqual(audio.getframerate(), sounds.RATE)
            self.assertEqual(audio.getnframes(), sounds.RATE * 1200 // 1000)
            actual = struct.unpack("<" + "h" * len(expected),
                                   audio.readframes(len(expected)))
        self.assertEqual(list(actual), expected)

    def test_oven_is_approved_f2_not_previous_experiment(self):
        path = ASSETS / "oven.wav"
        self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(), F2_SHA256)
        with wave.open(str(path), "rb") as audio:
            self.assertEqual(audio.getnchannels(), 1)
            self.assertEqual(audio.getsampwidth(), 2)
            self.assertEqual(audio.getframerate(), 44100)
            self.assertEqual(audio.getnframes(), 10804)  # ~0.245 seconds
            samples = struct.unpack("<" + "h" * 10804, audio.readframes(10804))
            self.assertGreater(max(map(abs, samples)), 10000)


if __name__ == "__main__":
    unittest.main()
