"""Exercise backup archives and recovery paths with mocked Linux process controls.

Runs with Git Bash on Windows or Bash on Linux. Uses real tar/gzip/sha256sum;
uname/flock/systemctl are test doubles, not a Linux lifecycle integration test.
"""
from pathlib import Path
import hashlib
import os
import shutil
import subprocess
import tarfile
import tempfile
import unittest


class BackupTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="inc-backup-test-")
        self.addCleanup(self.temp.cleanup)
        self.home = Path(self.temp.name)
        source = Path(__file__).resolve().parents[2] / "deploy/linux"
        for name in ("manage.sh", "backup.sh"):
            shutil.copy2(source / name, self.home / name)
        (self.home / "var/data").mkdir(parents=True)
        (self.home / "var/uploads/sub folder").mkdir(parents=True)
        self.database = self.home / "var/data/construction_management.mv.db"
        self.database.write_bytes(b"database-fixture\x00\xff")
        (self.home / "var/uploads/sub folder/attachment.txt").write_bytes(b"attachment-fixture")
        self.write(".env", 'export PATH="$SCRIPT_HOME/bin:$PATH"\nexport TEST_HOME="$SCRIPT_HOME"\n')
        self.write("bin/uname", '#!/usr/bin/env bash\nprintf "Linux\\n"\n')
        self.write("bin/flock", '''#!/usr/bin/env bash
if [[ "$*" == '-n 8' && -e "$TEST_HOME/lock-busy" ]]; then exit 1; fi
exit 0
''')
        self.write("bin/systemctl", '''#!/usr/bin/env bash
printf '%s\\n' "$1" >> "$TEST_HOME/service.trace"
case "$1" in
 show) cat "$TEST_HOME/service.state" ;;
 stop) printf 'inactive\\n' > "$TEST_HOME/service.state" ;;
 start)
   [[ ! -e "$TEST_HOME/start-fail" ]] || exit 1
   printf 'active\\n' > "$TEST_HOME/service.state" ;;
 *) exit 1 ;;
esac
''')
        self.bash = "C:/Program Files/Git/bin/bash.exe" if os.name == "nt" else shutil.which("bash")
        if not self.bash or not Path(self.bash).exists():
            self.skipTest("Bash not available")

    def write(self, name, text):
        file = self.home / name
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(text, encoding="utf-8", newline="\n")
        file.chmod(0o755)

    def systemd(self, state="active"):
        with (self.home / ".env").open("a", encoding="utf-8") as file:
            file.write("BACKUP_SYSTEMD_SERVICE=construction-management.service\n")
        self.write("service.state", state + "\n")

    def run_backup(self):
        return subprocess.run([self.bash, str(self.home / "backup.sh")], cwd=self.home,
                              capture_output=True, text=True, timeout=30)

    def archives(self):
        return list((self.home / "var/backups").glob("*.tar.gz"))

    def trace(self):
        return (self.home / "service.trace").read_text().splitlines()

    def fail_tar(self):
        self.write("bin/tar", "#!/usr/bin/env bash\nexit 28\n")

    def test_stopped_app_archive_and_checksums(self):
        result = self.run_backup()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        archive, = self.archives()
        with tarfile.open(archive) as tar:
            self.assertEqual(tar.extractfile("data/construction_management.mv.db").read(), self.database.read_bytes())
            for line in tar.extractfile("SHA256SUMS").read().decode().splitlines():
                # GNU checksum format has a space or '*' binary marker at offset 65.
                digest, name = line[:64], line[66:]
                self.assertEqual(hashlib.sha256(tar.extractfile(name).read()).hexdigest(), digest)
            self.assertFalse(any("backups/" in name or name.endswith(".env") for name in tar.getnames()))
        self.assertEqual(archive.with_suffix(".gz.sha256").read_text().split()[0], hashlib.sha256(archive.read_bytes()).hexdigest())
        self.assertFalse((self.home / "service.trace").exists())
        self.assertFalse(list((self.home / "var/backups").glob(".work.*")))

    def test_systemd_service_is_stopped_and_restarted(self):
        self.systemd()
        result = self.run_backup()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(self.trace(), ["show", "stop", "start"])
        self.assertEqual(len(self.archives()), 1)

    def test_compression_failure_restarts_service_and_removes_partial(self):
        self.systemd()
        self.fail_tar()
        result = self.run_backup()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.trace(), ["show", "stop", "start"])
        self.assertEqual(self.archives(), [])
        self.assertFalse(list((self.home / "var/backups").glob(".work.*")))
        self.assertEqual(self.database.read_bytes(), b"database-fixture\x00\xff")

    def test_inactive_service_stays_inactive(self):
        self.systemd("inactive")
        result = self.run_backup()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(self.trace(), ["show"])

    def test_instance_lock_cancels_backup(self):
        self.write("lock-busy", "busy")
        result = self.run_backup()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("still using this data directory", result.stderr)
        self.assertEqual(self.archives(), [])

    def test_restart_failure_is_reported_but_valid_backup_remains(self):
        self.systemd()
        self.write("start-fail", "fail")
        result = self.run_backup()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Could not restart", result.stderr)
        self.assertEqual(len(self.archives()), 1)

    def test_missing_db_does_not_stop_service(self):
        self.database.unlink()
        self.systemd()
        result = self.run_backup()
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.home / "service.trace").exists())

    def test_h2_auto_server_lock_cancels_backup(self):
        self.write("var/data/construction_management.lock.db", "lock")
        result = self.run_backup()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("H2 AUTO_SERVER lock", result.stderr)
        self.assertEqual(self.archives(), [])

    def test_output_inside_uploads_is_rejected(self):
        with (self.home / ".env").open("a", encoding="utf-8") as file:
            file.write('BACKUP_DIR="$SCRIPT_HOME/var/uploads/backups"\n')
        result = self.run_backup()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("must be outside", result.stderr)


if __name__ == "__main__":
    unittest.main()
