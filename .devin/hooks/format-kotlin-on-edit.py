#!/usr/bin/env python3
import json
import re
import subprocess
import sys

data = json.load(sys.stdin)
path = data.get("tool_input", {}).get("file_path", "")
if not re.search(r"\.(kt|kts)$", path):
    sys.exit(0)

result = subprocess.run(
    ["./gradlew", "spotlessApply", "-q"],
    stderr=subprocess.DEVNULL,
)
sys.exit(result.returncode)
