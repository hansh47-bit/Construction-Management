# Construction Management Architecture

본 시스템은 공사 관리 프로세스의 4단계 핵심 통제 룰을 준수하고 자동화하기 위한 시스템입니다[cite: 1].
이 시스템은 계약관리, 실행예산관리, 발주관리, 기성관리라는 별개의 프로그램을 만드는 것이 아니다.
하나의 프로젝트 안에서 계약 → 실행예산 → 발주 → 기성 → 지급 → 정산으로 이어지는 하나의 공사비 흐름을 관리하는 시스템이다.
각 단계의 금액은 서로 연결되어 있지만 의미가 다르기 때문에 데이터와 변경이력은 구분해서 저장한다.
사용자는 프로젝트를 열었을 때 현재 계약금액, 실행예산, 발주금액, 기성금액, 실제 지급금액과 예상이익을 한눈에 볼 수 있어야 한다.
대표가 승인한 최초 금액은 나중에 덮어쓰지 않는다. 변경은 별도의 증감 이력으로 기록하고 시스템이 현재금액을 자동 계산한다.
공사 중 하나의 변경사항이 계약, 실행예산, 발주에 모두 영향을 줄 경우 하나의 변경 건으로 연결해서 추적할 수 있어야 한다. 
단, 각각의 금액과 승인 여부는 독립적으로 관리한다.

대표는 프로젝트 통합현황과 통합결재함을 중심으로 사용하고, 직원은 세부자료를 입력하는 구조로 설계한다.
프로젝트 생성부터 최종 정산까지의 원가 흐름(계약 ➔ 실행예산 ➔ 발주 ➔ 기성 ➔ 지급 ➔ 준공정산)을 단일 프로젝트 중심으로 연결하고 추적하는 시스템 스펙 및 단계별 개발 로드맵입니다.
---
1. 문서 정보
   문서명: INC 공사 통합관리시스템 요구사항 명세
   목적: 인테리어/건설 프로젝트의 계약부터 준공정산까지 전체 공사비 흐름을 하나의 웹 시스템에서 통합 관리
   사용환경: PC 웹 및 모바일/태블릿 반응형
   주요 사용자: 대표 / 팀장 / 현장담당자 / 관리·회계 담당자
   상태: 개발 요구사항 초안
   기술스택: - 백엔드: Java 17, Spring Boot 3, Gradle - 프론트엔드: React, TypeScript, Tailwind CSS

### 주요기능
## 1. 멀티 에이전트 역할 및 책임 (Agents & Roles)

### 1. Project Core Agent (프로젝트 총괄 에이전트)
* **담당 영역**: 프로젝트 생성 및 통합 현황/대시보드 조회
* **주요 역할**:
    * 프로젝트 기본정보(번호, 고객, 착공/준공일, 담당자 등) 및 상태 관리
    * 한 화면에서 `계약 ➔ 실행예산 ➔ 발주 ➔ 기성 ➔ 지급` 흐름 통합 요약
    * 목표 원가한도, 예상 최종원가, 예상이익/이익률 자동 계산 및 모니터링

### 2. Budget Agent (실행예산 & 견적 에이전트)
* **담당 영역**: 고객 계약, 세부 실행원가 및 승인 실행예산 관리
* **주요 역할**:
    * 세부 실행원가 산출(공종별 항목, 단가, 수량) 및 실행예산 집계
    * 목표이익률 기준 원가한도 비교 검증 (`계약액 × (1 - 목표이익률)`)

### 3. Procurement Agent (발주 & 비교견적 에이전트)
* **담당 영역**: 업체 비교견적 및 발주 관리
* **주요 역할**:
    * 복수 업체 비교견적 항목 작성 및 선정 사유 검토
    * 발주 품의 생성 (승인 실행예산 대비 초과/절감액 검증)

### 4. Change Management Agent (변경관리 에이전트)
* **담당 영역**: 계약 / 실행예산 / 발주 변경 이력 관리
* **주요 역할**:
    * 최초 금액 보존 및 증감액(`+`/`-`) 기반 이력 추적
    * 고유 변경 ID(`CHG-YYYY-XXX`) 기반 계약, 실행예산, 발주 변경 연동
    * 현재금액 자동계산 (`현재금액 = 최초금액 + 승인 증감액 합계`)

### 5. Settlement & Payment Agent (기성 & 지급 관리 에이전트)
* **담당 영역**: 기성 작성, 결재, 실제 지급 및 최종 정산
* **주요 역할**:
    * 차수 제한 없는 업체별 기성 등록
    * 기성 승인과 실제 지급 처리 분리 (승인 후 미지급, 발주기준 총 미지급 잔액 관리)
    * 준공 시 최종 계약, 실행예산, 발주, 기성, 지급 종합 정산

### 6. Approval & Audit Agent (통합결재 & 감사 로그 에이전트)
* **담당 영역**: 대표/관리자 통합결재함 및 데이터 변경 이력 관리
* **주요 역할**:
    * 단일 통합결재함에서 실행예산, 발주, 변경, 기성/지급 결재 처리
    * 승인 데이터 삭제 불가 원칙 적용 (취소/정정/변경 이력 남김)
    * 변경 로그 (누가, 언제, 어떤 값을 어떻게 변경했는지) 저장

---

## 2. 데이터베이스 스키마 (SQLite DDL)

```sql
-- 1. 프로젝트 테이블
CREATE TABLE IF NOT EXISTS projects (
    id TEXT PRIMARY KEY,                       -- 예: P-2026-001
    name TEXT NOT NULL,
    client_name TEXT NOT NULL,
    address TEXT,
    contract_date DATE,
    start_date DATE,
    end_date DATE,
    target_profit_rate REAL DEFAULT 45.0,      -- 목표 이익률 (%)
    initial_contract_amount REAL DEFAULT 0,    -- 최초 계약금액
    status TEXT DEFAULT '견적',                -- 견적, 계약, 공사준비, 공사진행, 준공, 정산중, 정산완료, 보류
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. 변경 관리 테이블 (변경 건 연동)
CREATE TABLE IF NOT EXISTS change_orders (
    id TEXT PRIMARY KEY,                       -- 예: CHG-2026-001
    project_id TEXT NOT NULL,
    title TEXT NOT NULL,
    reason TEXT,
    created_by TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

-- 3. 금액 변경 이력 (계약, 실행예산, 발주 증감)
CREATE TABLE IF NOT EXISTS amount_change_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    change_order_id TEXT,                      -- 연결된 변경 건 ID (Optional)
    project_id TEXT NOT NULL,
    category TEXT NOT NULL,                    -- CONTRACT, BUDGET, PURCHASE
    target_id TEXT,                            -- 공종 또는 발주 ID
    delta_amount REAL NOT NULL,                -- 증감액 (+/-)
    status TEXT DEFAULT 'PENDING',             -- PENDING, APPROVED, REJECTED
    approval_id INTEGER,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (change_order_id) REFERENCES change_orders(id)
);

-- 4. 세부 실행원가 및 실행예산
CREATE TABLE IF NOT EXISTS budget_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id TEXT NOT NULL,
    category_name TEXT NOT NULL,               -- 공종 (예: 철거, 목공)
    item_name TEXT NOT NULL,                   -- 실행항목
    description TEXT,                          -- 세부내용
    vendor_type TEXT,                          -- 업체 / 직영
    quantity REAL DEFAULT 1,
    unit_price REAL DEFAULT 0,
    initial_budget REAL NOT NULL,              -- 최초 승인 실행예산
    status TEXT DEFAULT 'DRAFT',               -- DRAFT, REQUESTED, APPROVED, REJECTED
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

-- 5. 발주 관리
CREATE TABLE IF NOT EXISTS purchase_orders (
    id TEXT PRIMARY KEY,                       -- 예: PO-2026-001
    project_id TEXT NOT NULL,
    budget_item_id INTEGER NOT NULL,
    vendor_name TEXT NOT NULL,
    initial_po_amount REAL NOT NULL,           -- 최초 발주금액
    order_date DATE,
    status TEXT DEFAULT 'DRAFT',               -- DRAFT, REQUESTED, APPROVED, REJECTED
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (budget_item_id) REFERENCES budget_items(id)
);

-- 6. 기성 관리
CREATE TABLE IF NOT EXISTS progress_claims (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    purchase_order_id TEXT NOT NULL,
    degree INTEGER NOT NULL,                   -- 차수 (1차, 2차...)
    claim_amount REAL NOT NULL,                -- 기성 청구/승인 금액
    status TEXT DEFAULT 'REQUESTED',           -- REQUESTED, APPROVED, REJECTED
    approved_at DATETIME,
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id)
);

-- 7. 지급 처리
CREATE TABLE IF NOT EXISTS payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    progress_claim_id INTEGER NOT NULL,
    paid_amount REAL NOT NULL,                 -- 실제 송금/지급 금액
    payment_date DATE NOT NULL,                -- 실제 지급일
    payment_proof TEXT,                        -- 세금계산서/지급증빙 파일
    status TEXT DEFAULT 'COMPLETED',            -- COMPLETED
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (progress_claim_id) REFERENCES progress_claims(id)
);

-- 8. 통합 결재함
CREATE TABLE IF NOT EXISTS approvals (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id TEXT NOT NULL,
    type TEXT NOT NULL,                        -- BUDGET, BUDGET_CHANGE, PO, PO_CHANGE, CONTRACT_CHANGE, PROGRESS, FINAL_SETTLEMENT
    target_id TEXT NOT NULL,                   -- 해당 항목의 PK
    request_user TEXT NOT NULL,
    request_reason TEXT,
    status TEXT DEFAULT 'PENDING',             -- PENDING, APPROVED, REJECTED
    approver TEXT,
    comment TEXT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);
```

---

## 3. 핵심 자동 계산 규칙 (Core Business Logic)

시스템 내부에서 계산되는 모든 금액 지표는 다음 수식을 따릅니다.

1. **현재 금액 계산**:
    * $\text{현재 계약금액} = \text{최초 계약금액} + \sum(\text{승인된 계약 증감액})$
    * $\text{현재 실행예산} = \text{최초 실행예산} + \sum(\text{승인된 실행예산 증감액})$
    * $\text{현재 발주금액} = \text{최초 발주금액} + \sum(\text{승인된 발주 증감액})$

2. **기성 및 지급 관련**:
    * $\text{미기성 금액} = \text{현재 발주금액} - \text{승인 기성누계}$
    * $\text{승인 후 미지급} = \text{승인 기성누계} - \text{실제 지급누계}$
    * $\text{총 미지급 잔액} = \text{현재 발주금액} - \text{실제 지급누계}$

3. **손익 및 예상이익**:
    * $\text{예상 최종원가} = \text{현재 확정 발주금액} + \text{미발주 항목의 현재 실행예산}$
    * $\text{예상 프로젝트 이익} = \text{현재 계약금액} - \text{예상 최종원가}$
    * $\text{예상 이익률 (\%)} = \left(\frac{\text{예상 프로젝트 이익}}{\text{현재 계약금액}}\right) \times 100$

---

## 4. 단계별 개발 로드맵 (Development Stages)

### 📍 [1단계] 핵심 공사비 데이터 흐름 구축
* **목표**: 단일 프로젝트 중심으로 계약 ➔ 실행예산 ➔ 발주 ➔ 기성 ➔ 지급 데이터가 연결되는 기본 데이터 구조 및 API 구현
* **주요 작업**:
    * 프로젝트 등록/조회 및 상태 관리 API
    * 세부 실행원가 및 실행예산 산출 기능
    * 발주 등록 및 기성(차수별), 지급 연동 CRUD
    * 원가 자동 계산 로직 적용

### 📍 [2단계] 통합 결재 워크플로우 구현
* **목표**: 대표 및 관리자의 통합결재 프로세스 구축
* **주요 작업**:
    * 결재 유형별(예산, 발주, 기성, 변경) 통합 결재 승인/반려 엔드포인트 구현
    * 결재 승인 시 자동 상태 업데이트 (예: 기성 승인 ➔ 지급대기 전환)
    * 기성 승인과 실제 지급 처리(회계 담당)의 UI 및 권한 분리

### 📍 [3단계] 변경관리 및 이력 추적
* **목표**: 원본 금액 수정 금지 원칙 반영 및 변경이력 관리
* **주요 작업**:
    * 변경 ID (`CHG-XXXX`) 기반 계약/예산/발주 증감 처리
    * 변경이력 타임라인 및 원본-현재금액 비교 모달 개발
    * 데이터 임의 삭제 방지 및 정정/취소 로그 기록 (감사로그)

### 📍 [4단계] 경영진 모니터링 & 대시보드
* **목표**: 대표 대시보드 및 프로젝트 통합현황 화면 완성
* **주요 작업**:
    * 대표 대시보드 (전체 프로젝트 통합 지표, 미결재 알림 카드)
    * 프로젝트별 단일 통합현황 UI (원가 흐름 차트, 공종별 원가 추적 테이블)
    * 최종 준공정산 리포트 및 파이프라인 정산 완료 검증 규칙 적용
```
