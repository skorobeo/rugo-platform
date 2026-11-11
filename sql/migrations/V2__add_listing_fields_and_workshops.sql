--
-- V2: новые поля карточек техники/материалов + таблица мастерских (сбор заявок на размещение).
-- Применяется к существующей БД. Для свежих установок эти поля уже включены в sql/init_schema.sql.
-- Никакие существующие данные не удаляются и не изменяются.
--

ALTER TABLE public.equipment
    ADD COLUMN IF NOT EXISTS min_order character varying(100);

ALTER TABLE public.equipment
    ADD COLUMN IF NOT EXISTS consent boolean DEFAULT false NOT NULL;

ALTER TABLE public.materials
    ADD COLUMN IF NOT EXISTS min_order_tonnes integer;

ALTER TABLE public.materials
    ADD COLUMN IF NOT EXISTS delivery_type character varying(20) DEFAULT 'PICKUP' NOT NULL;

ALTER TABLE public.materials
    ADD COLUMN IF NOT EXISTS delivery_zone character varying(255);

ALTER TABLE public.materials
    ADD COLUMN IF NOT EXISTS consent boolean DEFAULT false NOT NULL;

CREATE TABLE IF NOT EXISTS public.workshops (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    specialization text NOT NULL,
    location character varying(255) DEFAULT 'Воронеж'::character varying NOT NULL,
    phone character varying(20) NOT NULL,
    consent boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS public.workshops_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.workshops_id_seq OWNED BY public.workshops.id;

ALTER TABLE ONLY public.workshops ALTER COLUMN id SET DEFAULT nextval('public.workshops_id_seq'::regclass);

ALTER TABLE ONLY public.workshops
    ADD CONSTRAINT workshops_pkey PRIMARY KEY (id);
