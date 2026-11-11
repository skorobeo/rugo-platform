--
-- V11: длина стрелы у техники (манипуляторы/вышки).
-- Применять вручную: psql -d rugo_db -f sql/migrations/V11__equipment_boom_length.sql
--

ALTER TABLE public.equipment
    ADD COLUMN IF NOT EXISTS boom_length varchar(100);
