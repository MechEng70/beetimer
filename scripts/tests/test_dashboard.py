"""Tests for scripts/dashboard.py. Run: python3 -I -m unittest discover -s scripts/tests"""

import os
import sys
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import dashboard  # noqa: E402

TASKS = """## Round R11

### Phase 4: device test hygiene

#### T-013 [~] Full E2E at 10x (L)
- **Goal:** the whole suite.
- **Size:** L. **Model:** main.

#### T-013a [ ] Alarm screen readability (M)
- **Goal:** bigger alarm text.
- **Size:** M. **Model:** main.

### Phase 5: release

#### T-014 [ ] Versioning (S)
- **Goal:** versionCode.
"""


class ParseTasksTest(unittest.TestCase):
    def test_letter_suffixed_task_is_its_own_task(self):
        # Regression (R11): T-013a was missing from the dashboard, and its fields overwrote T-013's.
        phases = dashboard.parse_tasks(TASKS)
        ids = [t["id"] for p in phases for t in p["tasks"]]
        self.assertEqual(ids, ["T-013", "T-013a", "T-014"])
        t013, t013a = phases[0]["tasks"]
        self.assertEqual(t013["fields"]["Goal"], "the whole suite.")
        self.assertEqual(t013a["fields"]["Goal"], "bigger alarm text.")
        self.assertEqual((t013a["mark"], t013a["title"], t013a["size"]), (" ", "Alarm screen readability", "M"))


if __name__ == "__main__":
    unittest.main()
