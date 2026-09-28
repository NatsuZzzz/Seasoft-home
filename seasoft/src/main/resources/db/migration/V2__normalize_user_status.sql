-- DB tao tu ban db.sql cu + Hibernate ddl-auto=update co cot users.status la
-- varchar nhung default van cast sang enum user_status. Chuan hoa ve
-- VARCHAR(30) + CHECK va bo enum. Tren DB moi tao tu V1 script nay vo hai.

ALTER TABLE users ALTER COLUMN status DROP DEFAULT;
ALTER TABLE users ALTER COLUMN status TYPE VARCHAR(30) USING status::text;
ALTER TABLE users ALTER COLUMN status SET DEFAULT 'PENDING_VERIFICATION';
DROP TYPE IF EXISTS user_status;

ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_status;
ALTER TABLE users ADD CONSTRAINT chk_users_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION'));
