--
-- V6: удаление «минимального заказа» из каталога и из сохранённых заявок.
-- Мин. заказ признан ненужным: в форме больше не собирается.
-- Применять вручную: psql -f sql/migrations/V6__drop_min_order.sql
-- Порядок: применить ДО запуска новой версии приложения (ddl-auto=validate).
--

ALTER TABLE public.equipment
    DROP COLUMN IF EXISTS min_order;

ALTER TABLE public.materials
    DROP COLUMN IF EXISTS min_order_tonnes;

-- Чистим ключи мин. заказа в уже сохранённых заявках (details — JSONB).
UPDATE public.partner_leads
SET details = details
    - 'minOrder'
    - 'minOrderByRequest'
    - 'minOrderTonnes'
    - 'minOrderTonnesByRequest'
WHERE details ?| ARRAY['minOrder', 'minOrderByRequest', 'minOrderTonnes', 'minOrderTonnesByRequest'];
