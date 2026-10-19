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

CREATE TABLE IF NOT EXISTS execution_budget_approval_attachments (
	attachment_id VARCHAR(64) PRIMARY KEY,
	project_id VARCHAR(64) NOT NULL,
	approval_id VARCHAR(64) NOT NULL,
	file_name VARCHAR(1000) NOT NULL,
	file_path VARCHAR(1000) NOT NULL,
	content_type VARCHAR(255),
	uploaded_at TIMESTAMP NOT NULL
);
