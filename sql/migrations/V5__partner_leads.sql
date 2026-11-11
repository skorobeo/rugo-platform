--
-- V5: заявки партнёров (промо-мини-приложение) и аналитические события.
--
-- Отклонение от ТЗ §7.3: offer_types хранится как JSONB, а не TEXT[].
-- Причина: сущность Hibernate маппится через @JdbcTypeCode(SqlTypes.JSON);
-- при ddl-auto: validate (ослаблять нельзя) тип колонки должен совпадать с
-- ожидаемым Hibernate для PostgreSQL, где SqlTypes.JSON = jsonb.
--

CREATE TABLE public.partner_leads (
    id          BIGSERIAL PRIMARY KEY,
    max_user_id BIGINT,
    name        TEXT NOT NULL,
    phone       TEXT NOT NULL,
    offer_types JSONB NOT NULL,
    district    TEXT,
    details     JSONB,
    source      TEXT,
    consent_at  TIMESTAMPTZ NOT NULL,
    status      TEXT NOT NULL DEFAULT 'new',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_partner_leads_created_at ON public.partner_leads (created_at);
CREATE INDEX idx_partner_leads_phone ON public.partner_leads (phone);

CREATE TABLE public.events (
    id         BIGSERIAL PRIMARY KEY,
    user_hash  TEXT NOT NULL,
    event      TEXT NOT NULL,
    props      JSONB,
    source     TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_event_created_at ON public.events (event, created_at);
CREATE INDEX idx_events_user_hash_created_at ON public.events (user_hash, created_at);
