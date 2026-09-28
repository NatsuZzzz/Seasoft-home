-- ============================================================
-- V4: du an cua khach + moc tien do + nhat ky cap nhat
-- ============================================================

CREATE SEQUENCE project_code_seq START 1;

CREATE TABLE projects (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(20)  NOT NULL UNIQUE,          -- SS-2026-0001
    name            VARCHAR(200) NOT NULL,
    customer_id     UUID NOT NULL REFERENCES users(id),
    -- Lead goc (moi lead chi tao duoc 1 du an)
    consultation_id UUID UNIQUE REFERENCES consultation_requests(id) ON DELETE SET NULL,
    manager_id      UUID REFERENCES users(id) ON DELETE SET NULL,
    service_type    VARCHAR(30)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PLANNING',
    description     TEXT,
    start_date      DATE,
    due_date        DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_project_status CHECK (status IN ('PLANNING', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_project_service CHECK (service_type IN
        ('CORPORATE_WEBSITE', 'LANDING_PAGE', 'ECOMMERCE', 'CUSTOM', 'MAINTENANCE', 'OTHER')),
    CONSTRAINT chk_project_dates CHECK (due_date IS NULL OR start_date IS NULL OR due_date >= start_date)
);

CREATE INDEX idx_projects_customer ON projects(customer_id);
CREATE INDEX idx_projects_manager  ON projects(manager_id);
CREATE INDEX idx_projects_status   ON projects(status);

CREATE TRIGGER trg_projects_updated_at
BEFORE UPDATE ON projects
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE project_milestones (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    sort_order   INT NOT NULL DEFAULT 0,
    status       VARCHAR(20) NOT NULL DEFAULT 'TODO',
    due_date     DATE,
    completed_at TIMESTAMPTZ,

    CONSTRAINT chk_milestone_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE'))
);

CREATE INDEX idx_milestones_project ON project_milestones(project_id, sort_order);

CREATE TABLE project_updates (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id          UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    author_id           UUID REFERENCES users(id) ON DELETE SET NULL,
    content             TEXT NOT NULL,
    visible_to_customer BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_updates_project ON project_updates(project_id, created_at DESC);
