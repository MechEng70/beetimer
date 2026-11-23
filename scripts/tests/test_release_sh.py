"""Checks on scripts/release.sh. Run: python3 -I -m unittest discover -s scripts -t scripts"""

import os
import re
import unittest

RELEASE_SH = os.path.join(os.path.dirname(__file__), "..", "release.sh")


class ReleaseShTest(unittest.TestCase):
    def test_no_early_exit_reader_after_a_pipe(self):
        # Regression (R11 T-016): release.sh runs with `set -euo pipefail`. `aapt2 ... | head -1` and
        # `apksigner ... | grep -m1` stop reading early, the writer dies of SIGPIPE (exit 141), and the script
        # quit right after the gate with no message. Filters after a pipe must read all their input.
        with open(RELEASE_SH, encoding="utf-8") as f:
            text = f.read()
        self.assertIn("set -euo pipefail", text)
        early = re.compile(r"\|\s*(head\b|grep\b[^|]*\s-[A-Za-z]*[mq]|sed\b[^|]*\bq\b)")
        bad = [f"{n}: {line.strip()}" for n, line in enumerate(text.splitlines(), 1) if early.search(line)]
        self.assertEqual(bad, [], "early-exit readers after a pipe")


if __name__ == "__main__":
    unittest.main()
