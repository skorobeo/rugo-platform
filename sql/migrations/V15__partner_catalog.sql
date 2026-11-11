--
-- V15: партнёрский каталог АО «Завод ЖБК».
-- Все таблицы каталога лежат в отдельной схеме zhbk, чтобы не смешивать
-- их с существующим ядром RuGo. База остаётся одна — rugo_db.
--
-- Применять под суперпользователем (postgres), потому что приложение
-- работает под rugo_user без права CREATE:
--   psql -d rugo_db -f sql/migrations/V15__partner_catalog.sql
--

CREATE SCHEMA IF NOT EXISTS zhbk;

-- Поиск по названию изделия (ILIKE '%...%').
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------------------------------------------------------------------------
-- Карточка партнёра (одна запись)
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- Разделы каталога
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- Категории внутри разделов
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- Изделия
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- Цены. Для ЖБИ — одна строка; для бетона — варианты по щебню
-- и температурному диапазону.
-- ---------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------
-- Права приложению (rugo_user) на новую схему.
-- ---------------------------------------------------------------------------
GRANT USAGE ON SCHEMA zhbk TO rugo_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA zhbk TO rugo_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA zhbk TO rugo_user;
