"""Check that the built-in sound assets match their reproducible generators."""
import importlib.util
import struct
import unittest
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location(
    "generate_sounds", ROOT / "tools" / "generate_sounds.py")
sounds = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sounds)


class SoundAssetTests(unittest.TestCase):
    def test_sounds_have_expected_duration_and_format(self):
        for name, factory, duration_ms in [
            ("bell", sounds.bell_samples, 1200),
            ("oven", sounds.oven_samples, 750),
        ]:
            with self.subTest(name=name), wave.open(
                str(ROOT / "app" / "src" / "main" / "assets" / (name + ".wav")), "rb"
            ) as audio:
                expected = factory()
                self.assertEqual(audio.getnchannels(), 1)
                self.assertEqual(audio.getsampwidth(), 2)
                self.assertEqual(audio.getframerate(), 44100)
                self.assertEqual(audio.getnframes(), sounds.RATE * duration_ms // 1000)
                actual = struct.unpack("<" + "h" * len(expected),
                                       audio.readframes(len(expected)))
                self.assertLessEqual(max(abs(a - b) for a, b in zip(actual, expected)), 1)
                self.assertGreater(max(map(abs, actual)), 10000)


if __name__ == "__main__":
    unittest.main()
