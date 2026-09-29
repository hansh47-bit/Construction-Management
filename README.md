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
- H2 Console: `http://localhost:8080/h2-console`
- IntelliJ Run Configurations:
  - `BackendApplication`: runs `com.construction.management.BackendApplication`
  - `backend bootRun`: runs Gradle `bootRun`
- H2 data is persisted under `backend/data/`.

## Run Frontend

```powershell
cd frontend
npm install
npm run dev
```

- App: `http://localhost:5173`
- API base URL defaults to `http://localhost:8080`.
- To override it, create `frontend/.env` from `frontend/.env.example` and set `VITE_API_BASE_URL`.

## Verify

```powershell
cd backend
.\gradlew.bat test

cd ..\frontend
npm run build
```
