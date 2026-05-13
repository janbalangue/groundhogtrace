from pathlib import Path

required = [
    "pom.xml",
    "docker-compose.yml",
    "services/groundhogtrace-api/pom.xml",
    "services/fake-crm-api/pom.xml",
    "services/groundhogtrace-api/src/main/java/dev/groundhogtrace/api/GroundhogTraceApplication.java",
    "services/fake-crm-api/src/main/java/dev/groundhogtrace/fakecrm/FakeCrmApplication.java",
    "examples/failing-capture.json",
]

missing = [p for p in required if not Path(p).exists()]
if missing:
    raise SystemExit(f"Missing required files: {missing}")

print("GroundhogTrace structure check passed")
