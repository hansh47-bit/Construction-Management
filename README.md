# Construction Management

공사 관리 프로세스의 프로젝트 등록, 실행예산, 발주품의, 변경계약, 기성 정산 흐름을 관리하기 위한 내부 시스템입니다.

## Stack

- Backend: Java 17, Spring Boot 3.5, Gradle Wrapper, H2
- Frontend: React, TypeScript, Vite, Tailwind CSS
- Encoding: UTF-8

## Run Backend

```powershell
cd backend
.\gradlew.bat bootRun
```

- API: `http://localhost:8080/api/dashboard`
- H2 Console is disabled by default. Set `H2_CONSOLE_ENABLED=true`, restart, and open `/h2-console/` locally to sign in with database credentials. See [console setup](deploy/linux/README.md#7-h2-웹-콘솔-접속).
- IntelliJ Run Configurations:
  - `BackendApplication`: runs `com.construction.management.BackendApplication`
  - `backend bootRun`: runs Gradle `bootRun`
- H2 data is persisted under `backend/data/`.
- Login lock settings: copy `backend/.env.example` to `backend/.env` and start from the backend directory. `LOGIN_MAX_FAILED_ATTEMPTS=5` and `LOGIN_LOCK_MINUTES=15` are the defaults. Use positive integers and restart after changes. Linux packages read the `.env` next to `manage.sh`.

## Run Frontend

```powershell
cd frontend
npm install
npm run dev
```

- App: `http://localhost:5173`
- API requests use the same-origin `/api` path by default; Vite proxies them to the backend during development.
- To override it, create `frontend/.env` from `frontend/.env.example` and set `VITE_API_BASE_URL`.

## Verify

```powershell
cd backend
.\gradlew.bat test

cd ..\frontend
npm run build
```

## Linux release

Build the frontend, test/build the backend and create the Linux archive on Windows:

```powershell
.\scripts\build-linux.ps1
```

Or build from a Linux checkout:

```bash
bash scripts/build-linux.sh
```

Output: `build/linux/construction-management-linux.tar.gz`. The archive contains `app.jar`, the built frontend, Linux configuration, start/stop/status/admin scripts, a systemd unit example, and SHA-256 checksums. Local accounts, databases, uploads and private environment files are excluded.

The runtime server needs Java 17+, Bash, curl and flock; Node.js and a separate frontend web server are not needed. The default configuration uses port 8080 on loopback with Secure cookies for an HTTPS Tunnel/reverse proxy. See [the Linux deployment guide](deploy/linux/README.md) for direct HTTP testing, data migration, first-admin creation and automatic startup.
