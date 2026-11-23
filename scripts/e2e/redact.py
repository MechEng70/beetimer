#!/usr/bin/env python3
"""Redacts token-like values from a log on stdin (playbook: logcat is saved only on failure, redacted).

Replaces: key=value or key: value pairs whose key looks secret (token, secret, password, passwd, api key,
auth, bearer, session, cookie), JWTs, and long hex or base64 runs (32 or more characters).
"""
import re
import sys

KEYED = re.compile(r"(?i)\b(\w*(?:token|secret|password|passwd|api[_-]?key|auth\w*|bearer|session\w*|cookie)\w*)(\s*[=:]\s*)(\S+)")
JWT = re.compile(r"\beyJ[\w-]+\.[\w-]+\.[\w-]+")
LONG = re.compile(r"\b(?:[A-Fa-f0-9]{32,}|[A-Za-z0-9+/_-]{40,}={0,2})\b")

for line in sys.stdin:
    line = KEYED.sub(lambda m: f"{m.group(1)}{m.group(2)}[REDACTED]", line)
    line = JWT.sub("[REDACTED-JWT]", line)
    line = LONG.sub("[REDACTED]", line)
    sys.stdout.write(line)
