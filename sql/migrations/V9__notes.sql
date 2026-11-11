--
-- V9: заметки/идеи владельца (пишутся из админ-бота).
-- Применять вручную: psql -f sql/migrations/V9__notes.sql
--

CREATE TABLE IF NOT EXISTS public.notes (
    id         BIGSERIAL PRIMARY KEY,
    text       TEXT NOT NULL,
    status     TEXT NOT NULL DEFAULT 'open',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notes_created_at ON public.notes (created_at);
