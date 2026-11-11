--
-- V13: индекс по статусу заявки (V-041).
-- Запросы админки и статистики часто фильтруют/считают partner_leads по status.
-- Применять вручную: psql -d rugo_db -f sql/migrations/V13__partner_leads_status_index.sql
--

CREATE INDEX IF NOT EXISTS idx_partner_leads_status ON public.partner_leads (status);
