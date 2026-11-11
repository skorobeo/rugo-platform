--
-- RuGo: полная схема пустой базы (актуальное состояние).
--
-- Назначение:
--   * НОВАЯ (пустая) база — выполнить этот файл целиком: psql -d rugo_db -f sql/init_schema.sql
--   * СУЩЕСТВУЮЩАЯ база — применять только недостающие миграции sql/migrations/V*.sql,
--     этот файл повторно НЕ выполнять (создаст конфликты).
--
-- Схема согласована с JPA-сущностями (rugo-core) при spring.jpa.hibernate.ddl-auto=validate.
-- Данных нет — только структура.
--

-- users
CREATE TABLE public.users (
    id           BIGSERIAL PRIMARY KEY,
    phone        VARCHAR(20)  NOT NULL,
    name         VARCHAR(255) NOT NULL,
    role         VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    partner_type VARCHAR(20),
    contact_name VARCHAR(255),
    telegram_id  BIGINT,
    max_id       BIGINT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT users_telegram_id_key UNIQUE (telegram_id),
    CONSTRAINT users_max_id_key UNIQUE (max_id)
);

-- equipment
CREATE TABLE public.equipment (
    id          BIGSERIAL PRIMARY KEY,
    owner_id    BIGINT       NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
    category    VARCHAR(50)  NOT NULL,
    type        VARCHAR(100) NOT NULL,
    brand       VARCHAR(100),
    model       VARCHAR(100),
    year        INTEGER,
    capacity    VARCHAR(100),
    boom_length VARCHAR(100),
    description TEXT,
    location    VARCHAR(255) NOT NULL DEFAULT 'Воронеж',
    price_type  VARCHAR(20)  DEFAULT 'HOUR',
    price       INTEGER,
    consent     BOOLEAN      NOT NULL DEFAULT false,
    status      VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    rating      NUMERIC(3, 2) DEFAULT 0,
    reliability NUMERIC(4, 1) DEFAULT 99.9,
    image_url   TEXT,
    image_urls  JSONB,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP
);

CREATE INDEX idx_equipment_category ON public.equipment (category);
CREATE INDEX idx_equipment_location ON public.equipment (location);
CREATE INDEX idx_equipment_owner ON public.equipment (owner_id);
CREATE INDEX idx_equipment_status ON public.equipment (status);

-- materials
CREATE TABLE public.materials (
    id             BIGSERIAL PRIMARY KEY,
    supplier_id    BIGINT       NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
    type           VARCHAR(100) NOT NULL,
    fraction       VARCHAR(50),
    price_per_unit INTEGER,
    unit           VARCHAR(20)  NOT NULL DEFAULT 'тонна',
    location       VARCHAR(255) NOT NULL DEFAULT 'Воронеж',
    available      BOOLEAN      NOT NULL DEFAULT true,
    delivery_type  VARCHAR(20)  NOT NULL DEFAULT 'PICKUP',
    delivery_zone  VARCHAR(255),
    status         VARCHAR(20)  NOT NULL DEFAULT 'APPROVED',
    consent        BOOLEAN      NOT NULL DEFAULT false,
    image_url      TEXT,
    image_urls     JSONB,
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP
);

CREATE INDEX idx_materials_location ON public.materials (location);
CREATE INDEX idx_materials_type ON public.materials (type);

-- workshops
CREATE TABLE public.workshops (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    specialization TEXT         NOT NULL,
    location       VARCHAR(255) NOT NULL DEFAULT 'Воронеж',
    phone          VARCHAR(20)  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    consent        BOOLEAN      NOT NULL DEFAULT false,
    created_at     TIMESTAMP    NOT NULL DEFAULT now()
);

-- partner_leads (заявки партнёров; offset-время → timestamptz)
CREATE TABLE public.partner_leads (
    id          BIGSERIAL PRIMARY KEY,
    max_user_id  BIGINT,
    name         TEXT        NOT NULL,
    phone        TEXT        NOT NULL,
    offer_types  JSONB       NOT NULL,
    partner_type VARCHAR(20),
    company_name TEXT,
    contact_name TEXT,
    district     TEXT,
    details     JSONB,
    source      TEXT,
    consent_at  TIMESTAMPTZ NOT NULL,
    status      TEXT        NOT NULL DEFAULT 'new',
    notified_at TIMESTAMPTZ,
    decided_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_partner_leads_created_at ON public.partner_leads (created_at);
CREATE INDEX idx_partner_leads_phone ON public.partner_leads (phone);
CREATE INDEX idx_partner_leads_status ON public.partner_leads (status);

-- events (аналитика)
CREATE TABLE public.events (
    id         BIGSERIAL PRIMARY KEY,
    user_hash  TEXT        NOT NULL,
    event      TEXT        NOT NULL,
    props      JSONB,
    source     TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_event_created_at ON public.events (event, created_at);
CREATE INDEX idx_events_user_hash_created_at ON public.events (user_hash, created_at);

-- admins (администраторы: доступ по MAX id; телефон — контакт)
CREATE TABLE public.admins (
    id          BIGSERIAL PRIMARY KEY,
    max_user_id BIGINT UNIQUE,
    phone       VARCHAR(20),
    name        VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- Первого администратора добавляйте вне репозитория: см. sql/seed-admins.example.sql.
-- Реальные MAX id/телефон в коде и в git не хранятся (см. docs/audit/SECRETS-ROTATION.md).

--
-- Права для рабочего пользователя приложения (создаётся отдельно, см. DEPLOY.md):
--   GRANT USAGE ON SCHEMA public TO rugo_user;
--   GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO rugo_user;
--   GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO rugo_user;
--

--
-- Партнёрский каталог ЖБК. Отдельная схема zhbk.
-- Таблицы и индексы дублируют sql/migrations/V15__partner_catalog.sql.
--

CREATE SCHEMA IF NOT EXISTS zhbk;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS zhbk.catalog (
    id          BIGSERIAL PRIMARY KEY,
    name        TEXT        NOT NULL,
    subtitle    TEXT,
    phone       VARCHAR(20),
    logo_url    TEXT,
    facts       JSONB,
    clients     JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ,
    CONSTRAINT chk_zhbk_catalog_singleton CHECK (id = 1)
);

CREATE TABLE IF NOT EXISTS zhbk.groups (
    id          BIGSERIAL PRIMARY KEY,
    title       TEXT        NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    sort_order  INTEGER     NOT NULL DEFAULT 0,
    active      BOOLEAN     NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_zhbk_groups_sort ON zhbk.groups (sort_order);

CREATE TABLE IF NOT EXISTS zhbk.categories (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT      NOT NULL REFERENCES zhbk.groups (id) ON DELETE RESTRICT,
    title       TEXT        NOT NULL,
    slug        VARCHAR(140) NOT NULL UNIQUE,
    image_url   TEXT,
    sort_order  INTEGER     NOT NULL DEFAULT 0,
    active      BOOLEAN     NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_zhbk_categories_group_sort
    ON zhbk.categories (group_id, sort_order);

CREATE TABLE IF NOT EXISTS zhbk.products (
    id             BIGSERIAL PRIMARY KEY,
    category_id    BIGINT      NOT NULL REFERENCES zhbk.categories (id) ON DELETE RESTRICT,
    name           TEXT        NOT NULL,
    external_key   VARCHAR(255) NOT NULL UNIQUE,
    source_url     TEXT,
    unit           VARCHAR(20) NOT NULL DEFAULT 'шт',
    availability   VARCHAR(20) NOT NULL DEFAULT 'ON_ORDER',
    tu             VARCHAR(160),
    weight_t       NUMERIC(10,3),
    length_m       NUMERIC(8,3),
    width_m        NUMERIC(8,3),
    thickness_m    NUMERIC(8,3),
    volume_m3      NUMERIC(10,4),
    image_url      TEXT,
    image_urls     JSONB,
    attributes     JSONB,
    sort_order     INTEGER     NOT NULL DEFAULT 0,
    active         BOOLEAN     NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ,
    CONSTRAINT chk_zhbk_products_availability
        CHECK (availability IN ('IN_STOCK', 'ON_ORDER', 'UNAVAILABLE')),
    CONSTRAINT chk_zhbk_products_unit CHECK (unit IN ('шт', 'м³', 'м2', 'м3'))
);

CREATE INDEX IF NOT EXISTS idx_zhbk_products_category_sort
    ON zhbk.products (category_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_zhbk_products_active
    ON zhbk.products (active);
CREATE INDEX IF NOT EXISTS idx_zhbk_products_source_url
    ON zhbk.products (source_url);
CREATE INDEX IF NOT EXISTS idx_zhbk_products_name_trgm
    ON zhbk.products USING gin (lower(name) gin_trgm_ops);

CREATE TABLE IF NOT EXISTS zhbk.product_prices (
    id                BIGSERIAL PRIMARY KEY,
    product_id        BIGINT      NOT NULL REFERENCES zhbk.products (id) ON DELETE CASCADE,
    variant           VARCHAR(120),
    temperature_band  VARCHAR(20),
    amount            NUMERIC(12,2),
    price_type        VARCHAR(20) NOT NULL DEFAULT 'FIXED',
    currency          VARCHAR(3)  NOT NULL DEFAULT 'RUB',
    unit              VARCHAR(20) NOT NULL DEFAULT 'шт',
    valid_from        DATE        NOT NULL DEFAULT CURRENT_DATE,
    valid_to          DATE,
    note              TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_zhbk_product_prices_type
        CHECK (price_type IN ('FIXED', 'ON_REQUEST')),
    CONSTRAINT chk_zhbk_product_prices_amount
        CHECK ((price_type = 'ON_REQUEST' AND amount IS NULL)
            OR (price_type = 'FIXED' AND amount IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS idx_zhbk_product_prices_product
    ON zhbk.product_prices (product_id, valid_from);
CREATE UNIQUE INDEX IF NOT EXISTS ux_zhbk_product_prices_key
    ON zhbk.product_prices (
        product_id,
        COALESCE(variant, ''),
        COALESCE(temperature_band, ''),
        valid_from
    );

-- Права приложению (rugo_user) на новую схему:
--   GRANT USAGE ON SCHEMA zhbk TO rugo_user;
--   GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA zhbk TO rugo_user;
--   GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA zhbk TO rugo_user;
