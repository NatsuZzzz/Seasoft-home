-- ============================================================
-- V5: noi dung marketing - du an tieu bieu, blog, danh gia khach hang
-- ============================================================

CREATE TABLE portfolio_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(200) NOT NULL,
    slug            VARCHAR(220) NOT NULL UNIQUE,
    category        VARCHAR(100),
    client_name     VARCHAR(150),
    summary         VARCHAR(500),
    content         TEXT,
    cover_image_url VARCHAR(1000),
    project_url     VARCHAR(500),
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_portfolio_published ON portfolio_items(published, sort_order);

CREATE TRIGGER trg_portfolio_updated_at
BEFORE UPDATE ON portfolio_items
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE blog_posts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(200) NOT NULL,
    slug            VARCHAR(220) NOT NULL UNIQUE,
    excerpt         VARCHAR(500),
    content         TEXT NOT NULL,
    cover_image_url VARCHAR(1000),
    tags            VARCHAR(200),
    author_id       UUID REFERENCES users(id) ON DELETE SET NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_blog_status CHECK (status IN ('DRAFT', 'PUBLISHED'))
);

CREATE INDEX idx_blog_published ON blog_posts(status, published_at DESC);

CREATE TRIGGER trg_blog_updated_at
BEFORE UPDATE ON blog_posts
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE testimonials (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_name VARCHAR(150) NOT NULL,
    company       VARCHAR(200),
    position      VARCHAR(100),
    quote         TEXT NOT NULL,
    rating        SMALLINT NOT NULL DEFAULT 5,
    avatar_url    VARCHAR(1000),
    published     BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order    INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_testimonial_rating CHECK (rating BETWEEN 1 AND 5)
);

-- 3 du an tieu bieu dang hien tren trang chu (giu nguyen noi dung cu)
INSERT INTO portfolio_items (title, slug, category, client_name, summary, cover_image_url, published, sort_order) VALUES
    ('Hệ thống website doanh nghiệp VinTech Corp', 'he-thong-website-doanh-nghiep-vintech-corp', 'Website Doanh Nghiệp', 'VinTech Corp', 'Kiến trúc website đa ngôn ngữ, tích hợp cổng thông tin nội bộ và giới thiệu danh mục sản phẩm công nghệ cao.', 'https://lh3.googleusercontent.com/aida-public/AB6AXuDX8z0NAiATrmcDmnvsSkovL4JSMunNAXMjKT4488VixavI2c2wz1ISd3iyLvmBxEQ9VfB-yJcrY45fgVVOLQHVBYiELj_3-K8v-9UfCWvho2kU69l9c-8u4cmeNXQtry1f_29c8exUwqCJy1XsjFREjj912v6_YwKI15e9TWcShUKFSOFekZjqp6k5WnZbDpj4b0Mit4OX9I9b3ZkegrxCNSfWrF_3IOC0KFaPS4sdG7BH54jqsSeeQl0UaBykVJHotD-JkExfNslX', TRUE, 1),
    ('Landing Page bất động sản Marina Bay', 'landing-page-bat-dong-san-marina-bay', 'Landing Page BĐS', 'Marina Bay', 'Trang đích tối ưu tỷ lệ đặt chỗ với hiệu ứng 3D trực quan và giao diện tương tác bảng giá căn hộ thời gian thực.', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAsDj6vggB4bXwlaX0ugrmgem3O6M6vcenfdMTTbBx3cvszO802Hz-CRILyVy8WuBrutEgjSko8vBhE1w8MI2qZyNxCzFEgcqNLhzo_E8q1YZsR3lcZt-iZRBanoKg1BFUBc7hfLL0qZbGfyG3lJz9Q4WyL3gJgdRsYNnnRNWNRGcwj5M2pHH22Xr5vigxR-WYP2YjSXaQye9LXN2O61Csb8qtH2T4qJQ-naKFfMOahBq_peWyPf5PYv4V_ZlGipGCeNf2us0_UITvb', TRUE, 2),
    ('Sàn thương mại điện tử EcoMart', 'san-thuong-mai-dien-tu-ecomart', 'E-Commerce Platform', 'EcoMart', 'Giải pháp sàn bán hàng trực tuyến với tốc độ tải dưới 1.2 giây, đồng bộ kho vận và thanh toán tức thì.', 'https://lh3.googleusercontent.com/aida-public/AB6AXuD6MIOJCZO1A5oZGrlibeHFaTk-0eea9sxe0hpg5zLV1M0cNFGvG2E4YtWI9WWWCoL9JzewzqZw8Cxjs2h1HpDuHJ-ZBG7McSa8sQMCVp0TLb5Y-Of2rS1vQZTDQV_ckC0yWodfqw91g7Ear1Y9qLt-CisaVHB504jhpJHrfgMXdindUjrcEP78eaFoU_rwHYNgkVij6Kcfhk6ifZozB19R63c-qEAUaTLYVUT2xZy6iRNsgeXxgU0Fjj_IBH3KwT-n24oV9B_0AhMc', TRUE, 3);
