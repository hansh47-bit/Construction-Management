"""Assemble already-built portable artifacts. Never includes local DBs or secrets."""
from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
import shutil
import tarfile
from datetime import datetime, timezone


def package_release(repo: Path, output: Path, jar: Path | None = None) -> Path:
    jars = [jar] if jar else sorted((repo / "backend/build/libs").glob("*.jar"))
    jars = [p for p in jars if not p.name.endswith("-plain.jar")]
    if len(jars) != 1 or not jars[0].is_file():
        raise SystemExit("Build a Spring Boot JAR first. If multiple JARs exist, select the current build with --jar PATH.")
    frontend = repo / "frontend/dist"
    if not (frontend / "index.html").is_file():
        raise SystemExit("Run npm run build:linux in frontend first.")
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S-%f")
    bundle = output / "staging" / stamp / "construction-management-linux"
    bundle.mkdir(parents=True)
    template = repo / "deploy/linux"
    required_templates = {
        "start.sh", "stop.sh", "status.sh", "run.sh", "init-admin.sh", "backup.sh", "manage.sh",
        ".env.example", "config/application-linux.properties", "construction-management.service", "README.md",
    }
    for relative in sorted(required_templates):
        source = template / relative
        if not source.is_file():
            raise SystemExit(f"Missing deployment template: {source}")
        if source.is_symlink():
            raise SystemExit(f"Unexpected symlink in deployment templates: {source}")
        if source.is_file():
            destination = bundle / source.relative_to(template)
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, destination)
    shutil.copy2(jars[0], bundle / "app.jar")
    shutil.copytree(frontend, bundle / "frontend")
    (bundle / "BUILD.txt").write_text(
        f"Built (UTC): {stamp}\nRuntime: Java 17+, Linux Bash, curl, flock (util-linux)\n"
        "Frontend and API share the Spring Boot origin.\n"
        "No accounts, passwords, databases, uploads or private environment files included.\n",
        encoding="utf-8",
    )
    for script in bundle.glob("*.sh"):
        data = script.read_bytes()
        if b"\r" in data or data.startswith(b"\xef\xbb\xbf"):
            raise SystemExit(f"Shell scripts must be LF, UTF-8 without BOM: {script}")
        script.chmod(0o755)
    manifest = []
    for file in sorted(bundle.rglob("*")):
        if file.is_file():
            digest = hashlib.sha256(file.read_bytes()).hexdigest()
            manifest.append(f"{digest}  {file.relative_to(bundle).as_posix()}")
    (bundle / "SHA256SUMS").write_text("\n".join(manifest) + "\n", encoding="utf-8")
    archive = output / "construction-management-linux.tar.gz"

    def permissions(info: tarfile.TarInfo) -> tarfile.TarInfo:
        info.uid = info.gid = 0
        info.uname = info.gname = "root"
        info.mode = 0o755 if info.isdir() or info.name.endswith(".sh") else 0o644
        return info

    with tarfile.open(archive, "w:gz", format=tarfile.PAX_FORMAT) as tar:
        tar.add(bundle, arcname=bundle.name, filter=permissions)
    digest = hashlib.sha256(archive.read_bytes()).hexdigest()
    (output / (archive.name + ".sha256")).write_text(f"{digest}  {archive.name}\n", encoding="utf-8")
    print(f"Bundle: {bundle}")
    print(f"Archive: {archive}")
    print(f"SHA-256: {digest}")
    return archive


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path)
    parser.add_argument("--jar", type=Path, help="Explicit bootJar output when other JAR copies exist.")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    package_release(root, (args.output or root / "build/linux").resolve(), args.jar.resolve() if args.jar else None)
