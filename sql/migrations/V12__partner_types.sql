-- Тип партнёра (частное лицо / компания), название компании и контактное лицо.
-- Тип: individual | company. Материалы и парк — только company.

ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS partner_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS contact_name VARCHAR(255);

ALTER TABLE public.partner_leads
    ADD COLUMN IF NOT EXISTS partner_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS company_name TEXT,
    ADD COLUMN IF NOT EXISTS contact_name TEXT;
