ALTER TABLE IF EXISTS project_execution_budget_items
	ADD COLUMN IF NOT EXISTS approval_status VARCHAR(255) DEFAULT 'DRAFT' NOT NULL;

ALTER TABLE IF EXISTS project_execution_budget_items
	ADD COLUMN IF NOT EXISTS approved_budget_amount DECIMAL(18, 2);

ALTER TABLE IF EXISTS project_execution_budget_items
	ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;

ALTER TABLE IF EXISTS project_execution_budget_items
	ADD COLUMN IF NOT EXISTS approval_id VARCHAR(64);

ALTER TABLE IF EXISTS project_execution_budget_items
	ADD COLUMN IF NOT EXISTS approval_comment VARCHAR(1000);

ALTER TABLE IF EXISTS project_revisions
	ADD COLUMN IF NOT EXISTS execution_budget_rewrite_available BOOLEAN DEFAULT FALSE NOT NULL;

ALTER TABLE IF EXISTS approvals
	ADD COLUMN IF NOT EXISTS cancelled_by VARCHAR(255);

ALTER TABLE IF EXISTS approvals
	ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(1000);

ALTER TABLE IF EXISTS approvals
	ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP;

ALTER TABLE IF EXISTS approvals
	ALTER COLUMN status ENUM('DRAFT', 'REQUESTED', 'APPROVED', 'REJECTED', 'CANCELLED');

ALTER TABLE IF EXISTS po_requisitions
	ADD COLUMN IF NOT EXISTS approver VARCHAR(255);

ALTER TABLE IF EXISTS po_requisitions
	ADD COLUMN IF NOT EXISTS comment VARCHAR(1000);

ALTER TABLE IF EXISTS po_requisitions
	ADD COLUMN IF NOT EXISTS decided_at TIMESTAMP;

ALTER TABLE IF EXISTS po_requisitions
	ADD COLUMN IF NOT EXISTS approval_id VARCHAR(64);

ALTER TABLE IF EXISTS purchase_orders
	ADD COLUMN IF NOT EXISTS requisition_id VARCHAR(64);

ALTER TABLE IF EXISTS purchase_orders
	ADD COLUMN IF NOT EXISTS approval_id VARCHAR(64);

ALTER TABLE IF EXISTS purchase_orders
	ADD COLUMN IF NOT EXISTS selected_comparison_id VARCHAR(64);

ALTER TABLE IF EXISTS purchase_orders
	ADD COLUMN IF NOT EXISTS approved_budget_amount DECIMAL(18, 2);

ALTER TABLE IF EXISTS purchase_orders
	ADD COLUMN IF NOT EXISTS budget_variance DECIMAL(18, 2);

ALTER TABLE IF EXISTS purchase_orders
	ADD COLUMN IF NOT EXISTS selection_reason VARCHAR(1000);

ALTER TABLE IF EXISTS purchase_orders
	ADD COLUMN IF NOT EXISTS construction_period VARCHAR(255);

ALTER TABLE IF EXISTS purchase_orders
	ADD COLUMN IF NOT EXISTS scope_and_notes VARCHAR(1000);

ALTER TABLE IF EXISTS purchase_orders
	ADD COLUMN IF NOT EXISTS payment_terms VARCHAR(255);

ALTER TABLE IF EXISTS purchase_orders
	ADD COLUMN IF NOT EXISTS estimate_file_url VARCHAR(1000);

ALTER TABLE IF EXISTS purchase_orders
	ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;

ALTER TABLE IF EXISTS purchase_orders
	ADD COLUMN IF NOT EXISTS approver VARCHAR(255);

ALTER TABLE IF EXISTS purchase_orders
	ADD COLUMN IF NOT EXISTS approval_comment VARCHAR(1000);

CREATE TABLE IF NOT EXISTS payment_requests (
	payment_request_id VARCHAR(64) PRIMARY KEY,
	project_id VARCHAR(64) NOT NULL,
	po_id VARCHAR(64) NOT NULL,
	claim_ids VARCHAR(2000) NOT NULL,
	requested_amount DECIMAL(18, 2) NOT NULL,
	approval_status VARCHAR(64) NOT NULL,
	approval_id VARCHAR(64),
	requested_at TIMESTAMP NOT NULL,
	decided_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS change_orders (
	change_order_id VARCHAR(64) PRIMARY KEY,
	project_id VARCHAR(64) NOT NULL,
	title VARCHAR(255) NOT NULL,
	reason VARCHAR(1000) NOT NULL,
	requested_by VARCHAR(255) NOT NULL,
	status VARCHAR(32) NOT NULL,
	created_at TIMESTAMP NOT NULL
);

-- Change-order revision flags were introduced after existing change orders had
-- already been stored. Backfill those rows before Hibernate maps them to
-- primitive booleans; old change orders represent the full change scope.
ALTER TABLE IF EXISTS change_orders
	ADD COLUMN IF NOT EXISTS revision_number INTEGER;

ALTER TABLE IF EXISTS change_orders
	ADD COLUMN IF NOT EXISTS contract_change_enabled BOOLEAN DEFAULT TRUE;

ALTER TABLE IF EXISTS change_orders
	ADD COLUMN IF NOT EXISTS budget_change_enabled BOOLEAN DEFAULT TRUE;

UPDATE change_orders
	SET contract_change_enabled = TRUE
	WHERE contract_change_enabled IS NULL;

UPDATE change_orders
	SET budget_change_enabled = TRUE
	WHERE budget_change_enabled IS NULL;

ALTER TABLE IF EXISTS change_orders
	ALTER COLUMN contract_change_enabled SET DEFAULT TRUE;

ALTER TABLE IF EXISTS change_orders
	ALTER COLUMN budget_change_enabled SET DEFAULT TRUE;

ALTER TABLE IF EXISTS change_orders
	ALTER COLUMN contract_change_enabled SET NOT NULL;

ALTER TABLE IF EXISTS change_orders
	ALTER COLUMN budget_change_enabled SET NOT NULL;

CREATE TABLE IF NOT EXISTS execution_budget_approval_attachments (
	attachment_id VARCHAR(64) PRIMARY KEY,
	project_id VARCHAR(64) NOT NULL,
	approval_id VARCHAR(64) NOT NULL,
	file_name VARCHAR(1000) NOT NULL,
	file_path VARCHAR(1000) NOT NULL,
	content_type VARCHAR(255),
	uploaded_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS daily_logs (
	daily_log_id VARCHAR(64) PRIMARY KEY,
	project_id VARCHAR(64) NOT NULL,
	log_date DATE NOT NULL,
	weather_condition VARCHAR(100),
	temperature_c DECIMAL(4, 1),
	site_manager_name VARCHAR(255) NOT NULL,
	today_work_content VARCHAR(8000),
	tomorrow_plan VARCHAR(8000),
	safety_training_completed BOOLEAN NOT NULL DEFAULT FALSE,
	safety_training_note VARCHAR(2000),
	complaint_note VARCHAR(4000),
	design_change_issue_note VARCHAR(4000),
	other_note VARCHAR(4000),
	status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
	created_by VARCHAR(255) NOT NULL,
	created_at TIMESTAMP NOT NULL,
	updated_by VARCHAR(255),
	updated_at TIMESTAMP NOT NULL,
	CONSTRAINT uq_daily_logs_project_date UNIQUE (project_id, log_date)
);

CREATE TABLE IF NOT EXISTS daily_log_labor_entries (
	id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, daily_log_id VARCHAR(64) NOT NULL,
	trade_name VARCHAR(255) NOT NULL, workforce_type VARCHAR(32), vendor_name VARCHAR(255),
	headcount DECIMAL(5, 1) NOT NULL DEFAULT 0, note VARCHAR(1000), sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS daily_log_equipment_entries (
	id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, daily_log_id VARCHAR(64) NOT NULL,
	equipment_name VARCHAR(255) NOT NULL, quantity DECIMAL(8, 2) NOT NULL DEFAULT 1, note VARCHAR(1000), sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS daily_log_material_entries (
	id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, daily_log_id VARCHAR(64) NOT NULL,
	entry_type VARCHAR(32) NOT NULL, item_name VARCHAR(255) NOT NULL, quantity DECIMAL(12, 3), unit VARCHAR(64), note VARCHAR(1000), sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS daily_log_photos (
	photo_id VARCHAR(64) PRIMARY KEY, daily_log_id VARCHAR(64) NOT NULL, phase VARCHAR(16) NOT NULL,
	storage_key VARCHAR(1000) NOT NULL, original_file_name VARCHAR(1000) NOT NULL, mime_type VARCHAR(255) NOT NULL,
	file_size BIGINT NOT NULL, caption VARCHAR(1000), sort_order INTEGER NOT NULL DEFAULT 0,
	uploaded_by VARCHAR(255) NOT NULL, uploaded_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS meeting_minutes (
	meeting_id VARCHAR(64) PRIMARY KEY, project_id VARCHAR(64) NOT NULL, meeting_at TIMESTAMP NOT NULL,
	location VARCHAR(500), subject VARCHAR(1000) NOT NULL, agenda_content VARCHAR(16000), decision_content VARCHAR(16000),
	status VARCHAR(32) NOT NULL DEFAULT 'DRAFT', created_by VARCHAR(255) NOT NULL, created_at TIMESTAMP NOT NULL,
	updated_by VARCHAR(255), updated_at TIMESTAMP NOT NULL
);
CREATE TABLE IF NOT EXISTS meeting_attendees (
	id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, meeting_id VARCHAR(64) NOT NULL,
	organization_type VARCHAR(32) NOT NULL, organization_name VARCHAR(255) NOT NULL, attendee_name VARCHAR(255) NOT NULL,
	position_name VARCHAR(255), contact VARCHAR(100), sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS meeting_action_items (
	id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, meeting_id VARCHAR(64) NOT NULL,
	action_content VARCHAR(4000) NOT NULL, assignee_name VARCHAR(255), assignee_user_id VARCHAR(255), due_date DATE,
	status VARCHAR(32) NOT NULL DEFAULT 'TODO', completed_at TIMESTAMP, sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS meeting_attachments (
	attachment_id VARCHAR(64) PRIMARY KEY, meeting_id VARCHAR(64) NOT NULL, storage_key VARCHAR(1000) NOT NULL,
	original_file_name VARCHAR(1000) NOT NULL, mime_type VARCHAR(255) NOT NULL, file_size BIGINT NOT NULL,
	uploaded_by VARCHAR(255) NOT NULL, uploaded_at TIMESTAMP NOT NULL
);

-- Serialize explicit first-admin bootstrap across concurrent application processes.
CREATE TABLE IF NOT EXISTS security_locks (id VARCHAR(64) PRIMARY KEY);
INSERT INTO security_locks(id)
 SELECT 'ADMIN_BOOTSTRAP' WHERE NOT EXISTS (SELECT 1 FROM security_locks WHERE id='ADMIN_BOOTSTRAP');
