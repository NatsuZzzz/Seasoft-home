-- ============================================================
-- SEASOFT - SCHEMA NGƯỜI DÙNG (PostgreSQL)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto; -- cho gen_random_uuid()

-- ------------------------------------------------------------
-- 1. Bảng vai trò (roles)
-- ------------------------------------------------------------
CREATE TABLE roles (
    id          SMALLSERIAL PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,   -- CUSTOMER, STAFF, MANAGER, ADMIN
    name        VARCHAR(100) NOT NULL,
    description TEXT
);

INSERT INTO roles (code, name, description) VALUES
    ('CUSTOMER', 'Khách hàng',      'Người dùng đăng ký tư vấn / theo dõi dự án'),
    ('STAFF',    'Nhân viên',       'Nhân viên tư vấn, thiết kế, kỹ thuật'),
    ('MANAGER',  'Quản lý',         'Trưởng nhóm / quản lý dự án'),
    ('ADMIN',    'Quản trị viên',   'Toàn quyền quản trị hệ thống');

-- ------------------------------------------------------------
-- 2. Bảng người dùng chính (users)
-- ------------------------------------------------------------
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION');

CREATE TABLE users (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id            SMALLINT NOT NULL REFERENCES roles(id),
    full_name          VARCHAR(150) NOT NULL,
    email              VARCHAR(150) NOT NULL UNIQUE,
    phone              VARCHAR(20),
    password_hash      VARCHAR(255) NOT NULL,
    avatar_url         VARCHAR(500),
    status             user_status NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified_at  TIMESTAMPTZ,
    last_login_at      TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role  ON users(role_id);

-- ------------------------------------------------------------
-- 3. Hồ sơ khách hàng (mở rộng cho role CUSTOMER)
-- ------------------------------------------------------------
CREATE TABLE customer_profiles (
    user_id      UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    company_name VARCHAR(200),
    tax_code     VARCHAR(30),
    industry     VARCHAR(100),
    address      VARCHAR(300),
    website      VARCHAR(200),
    source       VARCHAR(50)   -- kênh tiếp cận: Facebook, Google Ads, Referral...
);

-- ------------------------------------------------------------
-- 4. Hồ sơ nhân viên (mở rộng cho role STAFF/MANAGER/ADMIN)
-- ------------------------------------------------------------
CREATE TABLE staff_profiles (
    user_id       UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    employee_code VARCHAR(30) UNIQUE,
    department    VARCHAR(100),   -- Design, Dev, Sales, CSKH...
    position      VARCHAR(100),
    hire_date     DATE
);

-- ------------------------------------------------------------
-- 5. Refresh token (đăng nhập / JWT refresh)
-- ------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- ------------------------------------------------------------
-- 6. Token đặt lại mật khẩu
-- ------------------------------------------------------------
CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ------------------------------------------------------------
-- 7. Trigger tự động cập nhật updated_at
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();