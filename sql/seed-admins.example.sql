--
-- Шаблон создания первого администратора RuGo.
--
-- Реальные данные админа НЕ хранятся в репозитории. Скопируйте этот файл
-- в sql/seed-admins.sql (он в .gitignore), подставьте свои значения и примените:
--
--   cp sql/seed-admins.example.sql sql/seed-admins.sql
--   # отредактируйте sql/seed-admins.sql
--   sudo -u postgres psql -d rugo_db -f sql/seed-admins.sql
--
-- max_user_id  — id пользователя MAX (владельца). Его можно узнать в админ-боте
--                или из логов при попытке входа.
-- phone, name  — контактные данные (необязательно, для справки).
--

INSERT INTO public.admins (max_user_id, phone, name)
VALUES (<max_user_id>, '<телефон>', '<имя>')
ON CONFLICT (max_user_id) DO NOTHING;
