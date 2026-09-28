-- ============================================================
-- V3: yeu cau tu van (lead) tu form tren website
-- ============================================================

CREATE TABLE consultation_requests (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name         VARCHAR(150) NOT NULL,
    email             VARCHAR(150) NOT NULL,
    phone             VARCHAR(20)  NOT NULL,
    company_name      VARCHAR(200),
    service_type      VARCHAR(30)  NOT NULL,
    budget_range      VARCHAR(30)  NOT NULL DEFAULT 'UNDECIDED',
    message           TEXT,
    status            VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    -- Nhan vien phu trach (STAFF/MANAGER/ADMIN)
    assigned_staff_id UUID REFERENCES users(id) ON DELETE SET NULL,
    -- Khach dang dang nhap luc gui form (null neu khach vang lai)
    customer_id       UUID REFERENCES users(id) ON DELETE SET NULL,
    internal_note     TEXT,
    source            VARCHAR(50),
    ip_address        VARCHAR(45),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_consult_status CHECK (status IN ('NEW', 'CONTACTED', 'QUOTED', 'WON', 'LOST')),
    CONSTRAINT chk_consult_service CHECK (service_type IN
        ('CORPORATE_WEBSITE', 'LANDING_PAGE', 'ECOMMERCE', 'CUSTOM', 'MAINTENANCE', 'OTHER')),
    CONSTRAINT chk_consult_budget CHECK (budget_range IN
        ('UNDER_20M', 'FROM_20M_TO_50M', 'FROM_50M_TO_100M', 'OVER_100M', 'UNDECIDED'))
);

CREATE INDEX idx_consult_status     ON consultation_requests(status);
CREATE INDEX idx_consult_assigned   ON consultation_requests(assigned_staff_id);
CREATE INDEX idx_consult_customer   ON consultation_requests(customer_id);
CREATE INDEX idx_consult_created_at ON consultation_requests(created_at DESC);

CREATE TRIGGER trg_consult_updated_at
BEFORE UPDATE ON consultation_requests
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
