--
-- V10: администраторы RuGo (доступ по MAX id; телефон — контакт).
-- Применять вручную: psql -d rugo_db -f sql/migrations/V10__admins.sql
--

CREATE TABLE IF NOT EXISTS public.admins (
    id          BIGSERIAL PRIMARY KEY,
    max_user_id BIGINT UNIQUE,
    phone       VARCHAR(20),
    name        VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- Первого администратора добавляйте вне репозитория: см. sql/seed-admins.example.sql.
-- (Ранее здесь был INSERT с реальными данными — вынесено из git.)
