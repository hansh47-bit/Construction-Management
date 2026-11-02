"""Create a private Linux data archive from the current default Windows H2 database.

Requires Java 17+ (JDK), Python 3 and an existing built Spring Boot JAR.
Only BACKUP TO touches the source DB; path updates are restricted to the copy.
"""
from pathlib import Path, PurePosixPath
from datetime import datetime, timezone
import argparse
import hashlib
import os
import shutil
import subprocess
import tarfile
import tempfile
import zipfile


def hashes(folder: Path) -> dict[str, str]:
    result = {}
    for file in sorted(folder.rglob("*")):
        if file.is_symlink():
            raise SystemExit("Symlink uploads are not supported.")
        if file.is_file():
            result[file.relative_to(folder).as_posix()] = hashlib.sha256(file.read_bytes()).hexdigest()
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--linux-data-dir", default="/opt/construction-management/var")
    args = parser.parse_args()
    target = PurePosixPath(args.linux_data_dir)
    if not target.is_absolute() or ".." in target.parts or any(c in str(target) for c in ";\n\\"):
        raise SystemExit("Use a normalized absolute Linux data directory.")
    repo = Path(__file__).resolve().parents[1]
    backend = repo / "backend"
    if not (backend / "data/construction_management.mv.db").is_file():
        raise SystemExit("Existing source database not found; no new database will be created.")
    jars = [p for p in (backend / "build/libs").glob("*.jar") if not p.name.endswith("-plain.jar")]
    if len(jars) != 1:
        raise SystemExit("Build the Spring Boot JAR first.")
    stamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S-%f")
    output = repo / "build/linux"
    output.mkdir(parents=True, exist_ok=True)
    archive = output / f"construction-management-data-{stamp}.tar.gz"
    with tempfile.TemporaryDirectory(prefix="inc-linux-export-") as temp:
        work = Path(temp)
        data = work / "data"
        data.mkdir()
        uploads = work / "uploads"
        source_uploads = backend / "uploads"
        before = hashes(source_uploads)
        if source_uploads.exists():
            shutil.copytree(source_uploads, uploads)
        else:
            uploads.mkdir()
        with zipfile.ZipFile(jars[0]) as boot:
            drivers = [n for n in boot.namelist() if n.startswith("BOOT-INF/lib/h2-") and n.endswith(".jar")]
            if len(drivers) != 1:
                raise SystemExit("Expected one H2 JDBC driver in app.jar.")
            driver = work / "h2.jar"
            driver.write_bytes(boot.read(drivers[0]))
        env = os.environ.copy()
        env.update({
            "EXPORT_SOURCE_DB": (backend / "data/construction_management").as_posix(),
            "EXPORT_SOURCE_UPLOADS": source_uploads.as_posix(),
            "EXPORT_BACKUP_ZIP": (work / "backup.zip").as_posix(),
            "EXPORT_COPY_DB": (data / "construction_management").as_posix(),
            "EXPORT_COPIED_UPLOADS": uploads.as_posix(),
            "EXPORT_LINUX_DATA_DIR": str(target),
        })
        command = [env.get("JAVA_BIN", "java"), "--class-path", str(driver), str(repo / "scripts/LinuxDataExport.java")]
        subprocess.run(command + ["backup"], env=env, check=True)
        if before != hashes(source_uploads) or before != hashes(uploads):
            raise SystemExit("Uploads changed during backup. Retry with editing/uploads paused.")
        with zipfile.ZipFile(work / "backup.zip") as backup:
            members = backup.namelist()
            if members != ["construction_management.mv.db"]:
                raise SystemExit(f"Unexpected H2 backup entries: {members}")
            (data / members[0]).write_bytes(backup.read(members[0]))
        subprocess.run(command + ["relocate"], env=env, check=True)
        if before != hashes(source_uploads):
            raise SystemExit("Uploads changed during validation. Retry with editing/uploads paused.")
        # Never package transient H2 locks/traces or environment files.
        manifest = work / "DATA-SHA256SUMS"
        files = [(data / "construction_management.mv.db", "var/data/construction_management.mv.db")]
        files += [(uploads / relative, "var/uploads/" + relative) for relative in before]
        manifest.write_text("".join(f"{hashlib.sha256(p.read_bytes()).hexdigest()}  {name}\n" for p, name in files), encoding="utf-8")
        def permissions(info):
            info.uid = info.gid = 0
            info.uname = info.gname = "root"
            info.mode = 0o700 if info.isdir() else 0o600
            return info
        with tarfile.open(archive, "w:gz", format=tarfile.PAX_FORMAT) as tar:
            for file, name in files:
                tar.add(file, arcname=name, filter=permissions)
            tar.add(manifest, arcname="DATA-SHA256SUMS", filter=permissions)
        archive.chmod(0o600)
    digest = hashlib.sha256(archive.read_bytes()).hexdigest()
    archive.with_suffix(archive.suffix + ".sha256").write_text(f"{digest}  {archive.name}\n", encoding="utf-8")
    print(f"Private data archive: {archive}")
    print(f"Required Linux APP_DATA_DIR: {target}")
    print("Existing account/password hashes preserved. Do not run init-admin.sh.")


if __name__ == "__main__":
    main()
