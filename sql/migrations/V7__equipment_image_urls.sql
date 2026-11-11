--
-- V7: галерея фото техники — несколько изображений на карточку.
-- image_url остаётся обложкой, image_urls — список фото (до 3).
-- Применять вручную: psql -f sql/migrations/V7__equipment_image_urls.sql
--

ALTER TABLE public.equipment
    ADD COLUMN IF NOT EXISTS image_urls jsonb;
