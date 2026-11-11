--
-- V8: модерация заявок партнёров (админ-бот).
-- status: new → accepted/rejected; notified_at — когда админ-бот отправил карточку;
-- decided_at — когда принято решение.
-- Применять вручную: psql -f sql/migrations/V8__partner_lead_moderation.sql
--

ALTER TABLE public.partner_leads
    ADD COLUMN IF NOT EXISTS notified_at timestamptz;

ALTER TABLE public.partner_leads
    ADD COLUMN IF NOT EXISTS decided_at timestamptz;
