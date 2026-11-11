--
-- V3: статус модерации материалов (PENDING/APPROVED).
-- Существующие материалы получают статус APPROVED (видимы в каталоге).
-- Никакие существующие данные не удаляются и не изменяются по сути.
--

ALTER TABLE public.materials
    ADD COLUMN IF NOT EXISTS status character varying(20) DEFAULT 'APPROVED' NOT NULL;
