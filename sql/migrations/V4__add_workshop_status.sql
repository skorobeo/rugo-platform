--
-- V4: статус модерации мастерских (PENDING/APPROVED).
-- Существующие мастерские получают статус PENDING (попадут на модерацию).
--

ALTER TABLE public.workshops
    ADD COLUMN IF NOT EXISTS status character varying(20) DEFAULT 'PENDING' NOT NULL;
