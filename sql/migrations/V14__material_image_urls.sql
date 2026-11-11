--
-- V14: галерея фото материалов — несколько изображений на карточку.
-- image_url остаётся обложкой (первое фото), image_urls — список всех фото.
-- Применять вручную: psql -d rugo_db -f sql/migrations/V14__material_image_urls.sql
--

ALTER TABLE public.materials
    ADD COLUMN IF NOT EXISTS image_urls jsonb;

-- Бэкфилл: если есть обложка, а список пуст — кладём её первым фото.
UPDATE public.materials
SET image_urls = to_jsonb(ARRAY[image_url])
WHERE image_url IS NOT NULL AND image_urls IS NULL;
