# Linux 서버 실행 안내

이 패키지는 프론트 빌드 결과와 Spring Boot JAR를 포함합니다. Java 17 이상, Bash, curl, flock(util-linux)이 있는 Linux 서버에서 실행합니다. 실행 서버에 Node.js, Gradle, Nginx는 필요하지 않습니다. `app.jar`가 화면과 `/api`를 같은 포트에서 제공합니다.

## 1. 압축 해제와 설정

```bash
tar -xzf construction-management-linux.tar.gz
cd construction-management-linux
sha256sum -c SHA256SUMS
cp .env.example .env
chmod 600 .env
```

tar.gz에는 실행 권한이 포함되어 있습니다. 다른 방법으로 옮겼다면 `chmod +x ./*.sh`를 실행하거나 `bash start.sh`처럼 실행합니다. 셸 파일은 LF/UTF-8입니다.

기본 설정은 이전에 논의한 Cloudflare Tunnel/HTTPS 프록시 연결용입니다.

```dotenv
APP_BIND_ADDRESS=127.0.0.1
APP_PORT=8080
SESSION_COOKIE_SECURE=true
```

서버에서 `http://127.0.0.1:8080`으로 Tunnel을 연결한 뒤 외부 HTTPS 주소로 로그인합니다. 중간 웹서버를 거친다면 원래 HTTPS 요청 정보(X-Forwarded-Proto)를 전달해야 합니다. 별도 프론트 포트나 CORS 설정은 필요하지 않습니다.

서버 IP로 HTTP 직접 접속을 시험하려면 `.env`의 두 값을 명시적으로 변경합니다.

```dotenv
APP_BIND_ADDRESS=0.0.0.0
SESSION_COOKIE_SECURE=false
```

이 경우 `http://서버IP:8080`으로 접속합니다. 서버 방화벽에서도 해당 포트를 허용해야 합니다. HTTPS 운영으로 전환하면 Secure 쿠키 설정을 다시 true로 변경합니다.

## 2. 기존 데이터 이전 또는 새 어드민 생성

이 배포 파일에는 개발 PC의 DB, 계정, 비밀번호, 첨부파일이 포함되지 않습니다.

**기존 Windows 계정과 프로젝트를 그대로 옮기는 경우:** Windows 백엔드를 중지한 상태에서 다음 파일/폴더를 복사합니다. 사용자 ID·권한·해시와 공사비 이력은 같은 H2 DB에 들어 있으므로 DB 전체를 이전합니다.

| Windows 원본 | Linux 기본 위치 |
| --- | --- |
| `backend/data/construction_management.mv.db` | `var/data/construction_management.mv.db` |
| `backend/uploads/` 전체 | `var/uploads/` 전체 |

```bash
mkdir -p var/data var/uploads
# 중지된 원본 DB와 첨부파일을 위 경로로 복사한 후 시작합니다.
```

기존 계정이 있으면 초기 어드민 발급을 다시 실행하지 않습니다. 이전 단계에서 발급했던 계정은 해당 DB를 옮겨야 사용할 수 있습니다. 원본 DB에 다른 프로그램/H2 콘솔이 연결된 상태로 파일을 복사하지 않습니다.

**Windows에서 Linux로 첨부까지 옮기는 권장 방법:** 첨부의 DB 경로는 Windows 절대경로이므로 단순 복사만으로는 첨부 다운로드가 되지 않습니다. 저장소 루트에서 다음 명령으로 DB 백업과 첨부를 함께 내보냅니다. 원본 DB는 H2의 `BACKUP TO`로 백업하고, Linux 경로 변경은 복사본에만 적용합니다. 백업 중에는 자료 입력·첨부 수정 작업을 멈추고, 최종 이전 시점 이후에는 원본에 추가 입력하지 않습니다.

```powershell
python scripts/export-linux-data.py --linux-data-dir /opt/construction-management/var
```

생성된 `build/linux/construction-management-data-날짜.tar.gz`를 서버에 올립니다. 이 파일에는 실제 업무자료와 계정 해시가 있으므로 내부 이전용으로 보관합니다. Java 17 이상 JDK와 Python 3, 빌드된 백엔드 JAR가 필요합니다. 기본 Windows DB/첨부 경로를 사용하며, 별도 DB 인증정보는 `EXPORT_DB_USER`, `EXPORT_DB_PASSWORD` 환경변수로 전달합니다.

Linux에서 앱 패키지를 `/opt/construction-management`에 설치하고 **서버의 기존 DB가 없는 첫 이전**에 다음을 실행합니다. 이미 서버 데이터를 만든 경우 서비스를 중지하고 DB·첨부를 별도 백업한 후 이전 여부를 판단합니다. 아래 명령은 기존 서버 DB가 있으면 중단합니다.

```bash
cd /opt/construction-management
# 앱이 실행 중이면 먼저 bash stop.sh 또는 systemctl stop construction-management 실행
test ! -e var/data/construction_management.mv.db || { echo 'Existing DB: stop and back up first'; exit 1; }
tar -xzf /업로드경로/construction-management-data-날짜.tar.gz
sha256sum -c DATA-SHA256SUMS
```

`.env`에 `APP_DATA_DIR=/opt/construction-management/var`를 지정하고 `bash start.sh`로 시작합니다. 초기 어드민 생성 명령은 실행하지 않고 기존 계정으로 로그인합니다. systemd에서 `inc` 사용자를 사용하는 경우 이전 파일과 데이터 디렉터리도 `inc`가 읽고 쓸 수 있도록 소유권을 설정합니다. 내보낼 때 지정한 절대 Linux 데이터 경로와 `.env`의 경로가 일치해야 첨부가 열립니다.

**새 DB로 시작하는 경우:** 애플리케이션을 중지한 상태에서 실행합니다.

```bash
bash init-admin.sh inc-admin '시스템관리자'
```

임시 비밀번호와 확인값을 화면에 표시하지 않고 입력받습니다. 비밀번호를 명령행 인수나 `.env`에 저장하지 않습니다. 기존 DB 파일이 있으면 백업한 뒤 초기화를 시도하며, 계정이 하나라도 있으면 기존 계정을 바꾸지 않고 실패합니다. 최초 로그인에서 비밀번호 변경이 필요합니다.

## 3. 실행·중지·로그

```bash
bash start.sh
bash status.sh
tail -f var/logs/application.log
bash stop.sh
# 재시작
bash manage.sh restart
```

어느 작업 디렉터리에서 호출해도 스크립트 위치를 기준으로 설정과 프론트를 찾습니다. `start.sh`는 화면의 HTTP 응답을 확인한 뒤 성공을 보고하며 백그라운드로 실행합니다. 같은 데이터 디렉터리 중복 실행은 잠금으로 차단합니다. `stop.sh`는 이 패키지의 Java 프로세스인지 확인하고 정상 종료 신호만 보냅니다.

로그인 실패에 따른 계정 잠금 정책도 배포 폴더의 `.env`에서 설정합니다. 아래 값은 기본값이며, 각각 허용 실패 횟수와 잠금 시간(분)입니다.

```dotenv
LOGIN_MAX_FAILED_ATTEMPTS=5
LOGIN_LOCK_MINUTES=15
```

두 값은 따옴표 없이 1 이상의 정수로 입력합니다. 값이 잘못되면 앱이 시작되지 않으며 로그에 설정 오류가 표시됩니다. `.env` 수정 후에는 재빌드 없이 앱 재시작으로 반영합니다. systemd는 `systemctl restart construction-management`, 수동 실행은 `bash manage.sh restart`를 사용합니다. 이미 잠긴 계정의 잠금 해제 시각은 유지되며 새 로그인 실패부터 변경한 기준이 적용됩니다. 로그인 성공이나 관리자 잠금 해제 시 실패 횟수가 0으로 초기화됩니다. 계정 잠금과 별도로 기존 IP별 로그인 요청 제한(15분당 50회)은 유지됩니다.

다른 계정으로 전환할 때는 앱에서 로그아웃한 뒤 로그인합니다. 로그아웃은 서버 세션과 브라우저의 세션 쿠키를 정리하고, 다음 로그인은 새 세션을 사용합니다. 로그아웃 직후 익명 세션 쿠키를 다시 발급하지 않습니다. CSRF 토큰 발급·전송·검증은 사용하지 않습니다. 로그인과 업무 권한 검사는 서버 세션을 기준으로 적용합니다. 같은 브라우저의 탭들은 세션 쿠키를 공유하므로, 여러 계정을 동시에 사용하려면 브라우저 프로필이나 시크릿 창을 분리합니다.

기본 데이터 위치는 패키지 폴더의 `var/`입니다. `.env`에서 다음과 같이 영구 경로를 지정할 수 있습니다. 기존 데이터를 사용하는 경우 그 경로 아래에 `data/`, `uploads/`를 옮겨둡니다.

```dotenv
APP_DATA_DIR=/var/lib/inc-construction
JAVA_OPTS='-Xms128m -Xmx512m'
```

| 경로 | 내용 |
| --- | --- |
| `var/data/` | H2 DB와 계정·권한·업무 데이터 |
| `var/uploads/` | 계약/결재/현장 사진 등 모든 첨부 |
| `var/logs/application.log` | 회전 보관되는 애플리케이션 로그 |
| `var/logs/console.log` | 최근 백그라운드 기동의 콘솔 로그 |
| `var/run/` | PID와 프로세스 잠금 |
| `var/backups/` | 초기 계정 발급 시 중지된 DB의 백업 |

## 4. systemd 자동 시작 (선택)

패키지를 `/opt/construction-management`에 설치하고 `inc` 시스템 사용자가 패키지와 데이터 경로를 읽고 쓸 수 있게 구성한 후, 다음을 실행합니다. 다른 경로/사용자를 사용하면 제공된 service 파일을 먼저 수정합니다.

```bash
sudo useradd --system --user-group --home-dir /opt/construction-management --shell /usr/sbin/nologin inc
sudo chown -R inc:inc /opt/construction-management
sudo cp construction-management.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now construction-management
sudo systemctl status construction-management
```

systemd를 사용하면 시작/중지도 `systemctl start|stop|restart construction-management`로 관리합니다. `start.sh`와 systemd를 동시에 실행하지 않습니다. 서비스는 `run.sh`를 전경으로 실행하고 프로세스 장애 시 재시작합니다. 사용자 생성과 소유권 변경은 신규 설치 디렉터리에 맞게 적용합니다.

## 5. 업데이트와 백업

실행 중인 JAR를 덮어쓰지 않습니다. 서비스를 중지하고 DB·첨부를 백업한 뒤, 새 `app.jar`, `frontend/`, 스크립트와 설정 템플릿을 교체하고 다시 시작합니다. 기존 `.env`와 `var/`는 보존합니다. 새 폴더로 릴리스를 배포하면 동일한 절대 `APP_DATA_DIR`를 지정해 기존 데이터를 사용합니다. DB 파일 백업은 중지된 상태 또는 H2가 지원하는 일관된 백업 절차로 수행합니다.

### DB와 첨부파일 백업

배포 폴더에서 다음을 실행합니다. 기존 설치에 백업 기능만 추가한다면 `backup.sh`와 `manage.sh`를 함께 교체합니다.

```bash
bash backup.sh
```

앱이 실행 중이면 정상 종료하고, DB와 모든 첨부를 `.tar.gz`로 압축한 뒤 앱을 다시 시작합니다. 원래 중지 상태였다면 중지 상태를 유지합니다. 압축 오류 등으로 실패한 경우에도 원래 실행 중이던 앱은 재시작을 시도하고, 재시작 실패는 오류로 알립니다. 백업 중에는 앱 접속이 잠시 중단됩니다. 이 기능은 제공된 실행 스크립트 또는 설정한 systemd 서비스로 관리하는 앱을 대상으로 합니다. 그 밖의 Java 프로세스나 H2 도구에서 같은 DB를 직접 열고 있다면 먼저 연결을 종료합니다.

기본 저장 위치는 `$APP_DATA_DIR/backups/`이며 다음 두 파일이 생성됩니다.

```text
construction-management-backup-날짜-PID.tar.gz
construction-management-backup-날짜-PID.tar.gz.sha256
```

압축파일에는 `data/construction_management.mv.db`, `uploads/`, 파일별 `SHA256SUMS`, 원래 데이터 경로를 기록한 `BACKUP-INFO.txt`가 포함됩니다. `.env`, 로그, 이전 백업 파일은 포함하지 않습니다. 백업 파일은 실행 사용자만 읽고 쓸 수 있도록 생성됩니다. 자동 삭제와 정기 실행은 설정하지 않으며 실행할 때마다 백업합니다.

저장 위치를 바꾸려면 `.env`에 지정합니다. 다른 디스크나 마운트 경로도 사용할 수 있습니다.

```dotenv
BACKUP_DIR=/mnt/backup/inc-construction
```

systemd로 운영한다면 `.env`에 서비스 이름을 지정하고, 서비스 제어 권한이 있는 사용자로 실행합니다. 제공된 기본 서비스 기준의 예시입니다.

```dotenv
BACKUP_SYSTEMD_SERVICE=construction-management.service
```

```bash
sudo bash /opt/construction-management/backup.sh
```

앱 실행 사용자가 데이터와 `run/` 폴더에 쓰기 권한을 갖는 기존 설치에서 실행합니다. 서비스는 백업 전에 `systemctl stop`, 완료/실패 후에는 `systemctl start`로 관리됩니다. 원래 비활성 서비스는 시작하지 않습니다.

복구 전에는 앱을 중지하고 현재 DB·첨부도 별도 보관합니다. 백업을 임시 폴더에 해제한 뒤 그 폴더에서 `sha256sum -c SHA256SUMS`로 검증합니다. `BACKUP-INFO.txt`에 기록된 **동일한 절대 `APP_DATA_DIR` 경로**로 DB와 첨부를 함께 복구하고, 실행 사용자의 파일 소유권을 확인한 뒤 시작합니다. `.env`는 별도로 보관하며, 다른 경로로 복구하려면 DB에 저장된 첨부 절대경로도 이전해야 합니다.

## 6. 소스에서 패키지 재생성

Windows PowerShell(저장소 루트):

```powershell
.\scripts\build-linux.ps1
```

Linux(저장소 루트, 빌드 서버에는 Node/npm, JDK 17, Python 3 필요):

```bash
bash scripts/build-linux.sh
```

결과는 `build/linux/construction-management-linux.tar.gz`와 외부 SHA-256 파일입니다. `npm run build:linux`는 개발 PC의 API URL 설정과 관계없이 같은 Origin의 `/api`를 사용하도록 빌드합니다. 패키징은 배포 템플릿의 허용된 파일만 복사하며 로컬 데이터·비밀 환경파일은 수집하지 않습니다.

## 7. H2 웹 콘솔 접속

기본 상태에서는 H2 콘솔이 비활성화되어 있습니다. 콘솔을 지원하는 최신 `app.jar`를 설치하고 `.env`에 다음을 지정한 뒤 앱을 재시작합니다.

```dotenv
H2_CONSOLE_ENABLED=true
```

앱 로그인 없이 `http://localhost:8080/h2-console/`에 직접 접속하면 H2 자체 로그인 화면이 표시됩니다. 아래 DB 접속 정보로 로그인합니다. 앱의 계정·임시 비밀번호 변경 상태는 콘솔 접속에 영향을 주지 않습니다. 콘솔은 자체 DB 인증과 로컬 연결 제한을 사용하며, 앱 API는 별도의 로그인·업무 권한 검사를 적용합니다.

| H2 로그인 항목 | 기본값 |
| --- | --- |
| Driver Class | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:file:/opt/construction-management/var/data/construction_management;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;IFEXISTS=TRUE` |
| User Name | `sa` |
| Password | 빈칸 |

JDBC URL은 실제 `APP_DATA_DIR`에 맞춥니다. `.mv.db` 확장자는 URL에 붙이지 않습니다. 앱에 내장된 콘솔은 앱과 같은 프로세스로 접속하므로 앱을 켜둔 상태에서 사용합니다. 비활성 상태에서 터미널/DBeaver 등 다른 프로세스로 DB 파일을 여는 방식과는 구분합니다.

외부 콘솔 접속은 `spring.h2.console.settings.web-allow-others=false`와 콘솔 전용 로컬 연결 필터로 제한됩니다. Linux 서버에 원격으로 접속하려면 로컬 PC에서 SSH 포워딩을 실행하고, 브라우저에서 `http://localhost:18080/h2-console/`을 엽니다.

```bash
ssh -L 18080:127.0.0.1:8080 서버사용자@서버IP
```

H2 콘솔은 앱 로그인 쿠키 없이 DB 접속 정보로 로그인합니다. 앱의 세션 쿠키 설정은 기존 안내처럼 HTTP 운영 시 `SESSION_COOKIE_SECURE=false`, HTTPS 운영 시 `true`를 사용합니다. 점검이 끝나면 `H2_CONSOLE_ENABLED=false`로 변경하고 재시작합니다. H2 콘솔은 SQL을 직접 실행하는 도구이므로 조회 외의 SQL은 업무 승인·감사 API를 거치지 않습니다.

Windows에서 소스로 실행한다면 `.env` 대신 실행 환경변수를 지정합니다(백엔드 폴더의 PowerShell).

```powershell
$env:H2_CONSOLE_ENABLED='true'
.\gradlew.bat bootRun
```

Windows 기본 DB의 JDBC URL은 `jdbc:h2:file:G:/stockTrade/Construction-Management/backend/data/construction_management;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE;IFEXISTS=TRUE`입니다.
