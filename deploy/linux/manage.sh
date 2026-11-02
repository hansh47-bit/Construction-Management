#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

readonly SCRIPT_HOME="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
readonly SELF="$SCRIPT_HOME/manage.sh"
if [[ -f "$SCRIPT_HOME/.env" ]]; then
    set -a
    # This file is an administrator-owned Bash configuration, never a downloaded token.
    source "$SCRIPT_HOME/.env"
    set +a
fi
export APP_HOME="$SCRIPT_HOME"
export APP_PORT="${APP_PORT:-8080}"
export APP_BIND_ADDRESS="${APP_BIND_ADDRESS:-127.0.0.1}"
export SESSION_COOKIE_SECURE="${SESSION_COOKIE_SECURE:-true}"
export APP_DATA_DIR="${APP_DATA_DIR:-$APP_HOME/var}"
APP_START_TIMEOUT="${APP_START_TIMEOUT:-120}"
APP_STOP_TIMEOUT="${APP_STOP_TIMEOUT:-45}"

fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
[[ "$(uname -s)" == Linux ]] || fail 'These scripts require Linux. The server needs Bash, Java 17+, curl and flock (util-linux).'
command -v flock >/dev/null || fail 'Install flock (util-linux).'
[[ "$APP_PORT" =~ ^[0-9]+$ ]] && (( 10#$APP_PORT >= 1 && 10#$APP_PORT <= 65535 )) || fail 'APP_PORT must be 1-65535.'
[[ "$APP_START_TIMEOUT" =~ ^[1-9][0-9]*$ && "$APP_STOP_TIMEOUT" =~ ^[1-9][0-9]*$ ]] || fail 'Timeouts must be positive integer seconds.'
[[ "$SESSION_COOKIE_SECURE" == true || "$SESSION_COOKIE_SECURE" == false ]] || fail 'SESSION_COOKIE_SECURE must be true or false.'
[[ "$APP_DATA_DIR" = /* ]] || APP_DATA_DIR="$APP_HOME/$APP_DATA_DIR"
[[ "$APP_DATA_DIR" != *';'* && "$APP_DATA_DIR" != *$'\n'* ]] || fail 'APP_DATA_DIR must not contain semicolons or newlines.'
mkdir -p -- "$APP_DATA_DIR"/{data,uploads,logs,run,tmp,backups}
APP_DATA_DIR="$(cd -- "$APP_DATA_DIR" && pwd -P)"
export APP_DATA_DIR
readonly JAR_PATH="$APP_HOME/app.jar"
readonly PID_FILE="$APP_DATA_DIR/run/application.pid"
readonly CONSOLE_LOG="$APP_DATA_DIR/logs/console.log"
PID=''

read_pid() {
    PID=''
    [[ -f "$PID_FILE" ]] || return 1
    IFS= read -r PID < "$PID_FILE" || return 1
    [[ "$PID" =~ ^[0-9]+$ ]] && (( PID > 1 )) || return 1
}

is_our_process() {
    read_pid || return 1
    kill -0 "$PID" 2>/dev/null || return 1
    [[ -r "/proc/$PID/cmdline" ]] || return 1
    local arg found_jar=false found_home=false
    while IFS= read -r -d '' arg; do
        [[ "$arg" != "$JAR_PATH" ]] || found_jar=true
        [[ "$arg" != "-Dinc.application.home=$APP_HOME" ]] || found_home=true
    done < "/proc/$PID/cmdline"
    [[ "$found_jar" == true && "$found_home" == true ]]
}

prepare_java() {
    [[ -f "$JAR_PATH" && -f "$APP_HOME/config/application-linux.properties" ]] || fail 'app.jar or Linux configuration is missing; extract the complete release.'
    JAVA_BIN="${JAVA_BIN:-java}"
    command -v "$JAVA_BIN" >/dev/null || fail 'Java was not found. Install Java 17+ or set JAVA_BIN.'
    local version major
    version="$("$JAVA_BIN" -version 2>&1)"
    [[ "$version" =~ version[[:space:]]+\"([0-9]+) ]] || fail 'Could not determine the Java version.'
    major="${BASH_REMATCH[1]}"
    (( major >= 17 )) || fail 'Java 17 or newer is required.'
    JAVA_ARGS=()
    # Space-separated JVM options; do not eval configuration as a command.
    read -r -a JAVA_ARGS <<< "${JAVA_OPTS:--Xms128m -Xmx512m}"
    JAVA_ARGS+=("-Dfile.encoding=UTF-8" "-Dinc.application.home=$APP_HOME" -jar "$JAR_PATH")
    JAVA_ARGS+=(--spring.profiles.active=prod "--spring.config.additional-location=file:$APP_HOME/config/application-linux.properties")
}

control_lock() {
    exec 9> "$APP_DATA_DIR/run/control.lock"
    flock -w 10 9 || fail 'Another start/stop/initialization operation is in progress.'
}

run_application() {
    prepare_java
    [[ -f "$APP_HOME/frontend/index.html" ]] || fail 'The frontend build is missing.'
    exec 8> "$APP_DATA_DIR/run/instance.lock"
    flock -n 8 || fail 'This data directory is already in use by the application or administrator initialization.'
    is_our_process && fail "Already running (PID $PID)."
    cd -- "$APP_HOME"
    java_pid=''
    local result=0
    cleanup() {
        if [[ -n "$java_pid" && -f "$PID_FILE" ]] && [[ "$(cat "$PID_FILE")" == "$java_pid" ]]; then
            rm -f -- "$PID_FILE"
        fi
    }
    forward_signal() {
        [[ -z "$java_pid" ]] || kill -TERM "$java_pid" 2>/dev/null || true
    }
    trap cleanup EXIT
    trap forward_signal TERM INT
    "$JAVA_BIN" "${JAVA_ARGS[@]}" &
    java_pid=$!
    printf '%s\n' "$java_pid" > "$PID_FILE"
    wait "$java_pid" || result=$?
    # wait can be interrupted by a forwarded signal before Java has shut down.
    if kill -0 "$java_pid" 2>/dev/null; then
        wait "$java_pid" || result=$?
    fi
    return "$result"
}

stop_application() {
    if ! is_our_process; then
        printf 'Not running. No unrelated process was signalled.\n'
        return 0
    fi
    local target="$PID" count
    kill -TERM "$target"
    for (( count=0; count<APP_STOP_TIMEOUT; count++ )); do
        if ! is_our_process && flock -n "$APP_DATA_DIR/run/instance.lock" -c true; then
            printf 'Stopped (PID %s).\n' "$target"
            return 0
        fi
        sleep 1
    done
    fail "PID $target has not stopped. Inspect $APP_DATA_DIR/logs/application.log; no forced kill was issued."
}

start_application() {
    prepare_java
    command -v curl >/dev/null || fail 'Install curl for startup readiness checks.'
    if is_our_process; then
        printf 'Already running (PID %s).\n' "$PID"
        return 0
    fi
    # A foreground/systemd process may hold the instance lock before it has a PID file.
    flock -n "$APP_DATA_DIR/run/instance.lock" -c true || fail 'The application or initialization is already starting.'
    nohup bash "$SELF" _run </dev/null > "$CONSOLE_LOG" 2>&1 9>&- &
    local supervisor=$! count host="$APP_BIND_ADDRESS" ready=false
    case "$host" in
        0.0.0.0|'::'|'[::]') host=127.0.0.1 ;;
        *:*) host="[$host]" ;;
    esac
    for (( count=0; count<APP_START_TIMEOUT; count++ )); do
        if ! kill -0 "$supervisor" 2>/dev/null; then
            tail -n 40 "$CONSOLE_LOG" >&2
            fail 'Application startup failed.'
        fi
        if is_our_process && grep -q 'Started BackendApplication' "$CONSOLE_LOG" && curl --noproxy '*' --fail --silent --max-time 2 "http://$host:$APP_PORT/" -o /dev/null; then
            ready=true
            break
        fi
        sleep 1
    done
    if [[ "$ready" != true ]]; then
        stop_application
        fail "Startup timed out; inspect $CONSOLE_LOG."
    fi
    printf 'Started (PID %s). Frontend and API: http://%s:%s/\n' "$PID" "$host" "$APP_PORT"
    printf 'Logs: %s/logs/application.log\n' "$APP_DATA_DIR"
    if [[ "$SESSION_COOKIE_SECURE" == true ]]; then
        printf 'HTTPS cookie mode is enabled. Use your HTTPS Tunnel/proxy URL to sign in.\n'
    fi
}

initialize_admin() {
    prepare_java
    exec 8> "$APP_DATA_DIR/run/instance.lock"
    flock -n 8 || fail 'Stop the application before initializing the administrator.'
    is_our_process && fail 'Stop the application before initializing the administrator.'
    local login="${1:-}" display_name="${2:-}" password='' confirmation='' backup
    [[ -n "$login" ]] || read -r -p 'Administrator login ID: ' login
    [[ -n "$display_name" ]] || read -r -p 'Administrator display name: ' display_name
    read -r -s -p 'Temporary password (8-64 characters, UTF-8 <=72 bytes): ' password
    printf '\n'
    read -r -s -p 'Confirm temporary password: ' confirmation
    printf '\n'
    [[ -n "$password" && "$password" == "$confirmation" ]] || fail 'Passwords are empty or do not match.'
    if [[ -f "$APP_DATA_DIR/data/construction_management.mv.db" ]]; then
        backup="$APP_DATA_DIR/backups/construction_management-$(date +%Y%m%d-%H%M%S)-$$.mv.db"
        cp -p -- "$APP_DATA_DIR/data/construction_management.mv.db" "$backup"
        printf 'Stopped-database backup: %s\n' "$backup"
    fi
    cd -- "$APP_HOME"
    BOOTSTRAP_ADMIN_LOGIN="$login" BOOTSTRAP_ADMIN_NAME="$display_name" BOOTSTRAP_ADMIN_PASSWORD="$password" \
        "$JAVA_BIN" "${JAVA_ARGS[@]}" --app.bootstrap-admin=true --server.port=0 --server.address=127.0.0.1
    unset password confirmation
    printf 'Administrator created. The first login requires a password change.\n'
}

backup_exit() {
    local result=$?
    trap - EXIT INT TERM
    # Release the instance lock before restoring the previous running state.
    exec 8>&-
    if [[ -n "${BACKUP_WORK:-}" && "$BACKUP_WORK" == "$BACKUP_DIR"/.work.* && -d "$BACKUP_WORK" ]]; then
        if ! rm -rf -- "$BACKUP_WORK"; then
            printf 'ERROR: Could not remove temporary backup directory: %s\n' "$BACKUP_WORK" >&2
            result=1
        fi
    fi
    if [[ "${BACKUP_RESTART:-false}" == true ]]; then
        printf 'Restoring the application running state...\n'
        if [[ -n "$BACKUP_SERVICE" ]]; then
            if ! systemctl start "$BACKUP_SERVICE"; then
                printf 'ERROR: Could not restart %s. Start it manually.\n' "$BACKUP_SERVICE" >&2
                result=1
            fi
        elif ! (start_application); then
            printf 'ERROR: Could not restart the application. Run bash start.sh manually.\n' >&2
            result=1
        fi
    fi
    exit "$result"
}

backup_application() {
    [[ $# == 0 ]] || fail 'Usage: bash backup.sh (configure BACKUP_DIR/BACKUP_SYSTEMD_SERVICE in .env).'
    local tool service_state='' cgroup='' stamp archive checksum
    for tool in tar gzip sha256sum find sort xargs mktemp; do
        command -v "$tool" >/dev/null || fail "Install $tool before backing up."
    done
    [[ -f "$APP_DATA_DIR/data/construction_management.mv.db" ]] || fail 'No existing database was found; no backup created.'
    BACKUP_DIR="${BACKUP_DIR:-$APP_DATA_DIR/backups}"
    [[ "$BACKUP_DIR" = /* ]] || BACKUP_DIR="$APP_HOME/$BACKUP_DIR"
    mkdir -p -- "$BACKUP_DIR"
    BACKUP_DIR="$(cd -- "$BACKUP_DIR" && pwd -P)"
    case "$BACKUP_DIR/" in
        "$APP_DATA_DIR/data/"*|"$APP_DATA_DIR/uploads/"*) fail 'BACKUP_DIR must be outside data/ and uploads/.' ;;
    esac
    BACKUP_SERVICE="${BACKUP_SYSTEMD_SERVICE:-}"
    BACKUP_RESTART=false
    BACKUP_WORK=''
    if [[ -n "$BACKUP_SERVICE" ]]; then
        [[ "$BACKUP_SERVICE" =~ ^[a-zA-Z0-9_.@-]+\.service$ ]] || fail 'BACKUP_SYSTEMD_SERVICE must be a systemd .service name.'
        command -v systemctl >/dev/null || fail 'systemctl is required for BACKUP_SYSTEMD_SERVICE.'
        service_state="$(systemctl show --property=ActiveState --value "$BACKUP_SERVICE")" || fail 'Could not read the systemd service state.'
        case "$service_state" in
            active|inactive|failed) ;;
            *) fail "Service is $service_state. Wait for a stable state and retry." ;;
        esac
    elif is_our_process; then
        # systemd may restart Java if only its PID is terminated. Stop the unit instead.
        if [[ -r "/proc/$PID/cgroup" ]]; then
            cgroup="$(cat "/proc/$PID/cgroup")"
            [[ ! "$cgroup" =~ /[^/]+\.service(/|$) ]] || fail 'Application is managed by systemd. Set BACKUP_SYSTEMD_SERVICE in .env first.'
        fi
        # Check prerequisites for restarting before taking the application offline.
        prepare_java
        command -v curl >/dev/null || fail 'Install curl so the application can restart after backup.'
    fi
    trap backup_exit EXIT
    trap 'exit 130' INT
    trap 'exit 143' TERM
    if [[ -n "$BACKUP_SERVICE" && "$service_state" == active ]]; then
        BACKUP_RESTART=true
        systemctl stop "$BACKUP_SERVICE"
    elif [[ -z "$BACKUP_SERVICE" ]] && is_our_process; then
        BACKUP_RESTART=true
        stop_application
    fi
    exec 8> "$APP_DATA_DIR/run/instance.lock"
    flock -n 8 || fail 'The application/initialization is still using this data directory. Backup cancelled.'
    is_our_process && fail 'The application is still running. Backup cancelled.'
    [[ ! -e "$APP_DATA_DIR/data/construction_management.lock.db" ]] || fail 'An H2 AUTO_SERVER lock exists. Close other DB connections before backup.'
    # Reject external linked files; the archive must be self-contained.
    [[ ! -L "$APP_DATA_DIR/data/construction_management.mv.db" && ! -L "$APP_DATA_DIR/uploads" ]] || fail 'Linked database/upload paths are not supported.'
    [[ -z "$(find "$APP_DATA_DIR/uploads" -type l -print -quit)" ]] || fail 'Linked upload files are not supported.'
    BACKUP_WORK="$(mktemp -d "$BACKUP_DIR/.work.XXXXXXXX")"
    stamp="$(date -u +%Y%m%dT%H%M%SZ)-$$"
    archive="construction-management-backup-$stamp.tar.gz"
    checksum="$archive.sha256"
    printf 'Created UTC: %s\nAPP_DATA_DIR: %s\nContents: H2 database and all uploads\nRestore to the same absolute APP_DATA_DIR; attachment paths are preserved.\n' \
        "$(date -u +%FT%TZ)" "$APP_DATA_DIR" > "$BACKUP_WORK/BACKUP-INFO.txt"
    (
        cd -- "$APP_DATA_DIR"
        find data/construction_management.mv.db uploads -type f -print0 | sort -z | xargs -0 -r sha256sum
    ) > "$BACKUP_WORK/SHA256SUMS"
    tar -czf "$BACKUP_WORK/$archive" -C "$APP_DATA_DIR" data/construction_management.mv.db uploads \
        -C "$BACKUP_WORK" BACKUP-INFO.txt SHA256SUMS
    gzip -t "$BACKUP_WORK/$archive"
    tar -tzf "$BACKUP_WORK/$archive" >/dev/null
    (cd -- "$BACKUP_WORK"; sha256sum "$archive") > "$BACKUP_WORK/$checksum"
    # Publish only a complete, validated archive. No backup retention/deletion is automatic.
    mv -- "$BACKUP_WORK/$archive" "$BACKUP_DIR/$archive"
    mv -- "$BACKUP_WORK/$checksum" "$BACKUP_DIR/$checksum"
    printf 'Backup completed: %s/%s\nChecksum: %s/%s\n' "$BACKUP_DIR" "$archive" "$BACKUP_DIR" "$checksum"
}

command_name="${1:-status}"
shift || true
case "$command_name" in
    start) control_lock; start_application ;;
    stop) control_lock; stop_application ;;
    restart) control_lock; stop_application; start_application ;;
    status)
        if is_our_process; then printf 'Running (PID %s). Data: %s\n' "$PID" "$APP_DATA_DIR";
        else printf 'Not running. Data: %s\n' "$APP_DATA_DIR"; exit 3; fi ;;
    run|_run) run_application ;;
    init-admin) control_lock; initialize_admin "$@" ;;
    backup) control_lock; backup_application "$@" ;;
    *) fail 'Usage: bash manage.sh {start|stop|restart|status|run|backup|init-admin [LOGIN NAME]}' ;;
esac
