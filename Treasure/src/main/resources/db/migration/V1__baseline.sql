-- Extensions (safe if already present)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS postgis;

-- Enums
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'difficulty') THEN
    CREATE TYPE difficulty AS ENUM ('BEGINNER','INTERMEDIATE','ADVANCED');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'enrollment_status') THEN
    CREATE TYPE enrollment_status AS ENUM ('PENDING','CONFIRMED','REJECTED','CANCELLED');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'task_status') THEN
    CREATE TYPE task_status AS ENUM ('LOCKED','STARTED','DONE');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'audience_type') THEN
    CREATE TYPE audience_type AS ENUM ('INDIVIDUAL','GROUP');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'time_window_type') THEN
    CREATE TYPE time_window_type AS ENUM ('DAY','NIGHT','FULL_DAY','MULTI_DAY');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'enrollment_mode') THEN
    CREATE TYPE enrollment_mode AS ENUM ('PAY_TO_ENROLL','APPROVAL_REQUIRED');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_status') THEN
    CREATE TYPE payment_status AS ENUM ('NONE','AWAITING','PAID','REFUNDED');
  END IF;
END$$;

-- Reference tables
CREATE TABLE age_band (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  label TEXT NOT NULL,
  min_age INT NOT NULL,
  max_age INT NOT NULL,
  CONSTRAINT ck_age_span CHECK (min_age >= 0 AND max_age >= min_age)
);

CREATE TABLE category (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name TEXT NOT NULL,
  description TEXT,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  audience audience_type NOT NULL,
  tags JSONB
);

CREATE TABLE subcategory (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  category_id UUID NOT NULL REFERENCES category(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  description TEXT,
  active BOOLEAN NOT NULL DEFAULT TRUE
);

-- join: subcategory allowed age bands
CREATE TABLE subcategory_age_band (
  subcategory_id UUID NOT NULL REFERENCES subcategory(id) ON DELETE CASCADE,
  age_band_id   UUID NOT NULL REFERENCES age_band(id)   ON DELETE RESTRICT,
  PRIMARY KEY (subcategory_id, age_band_id)
);

CREATE TABLE plan (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  subcategory_id UUID NOT NULL REFERENCES subcategory(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  summary TEXT,
  venue_text TEXT,
  geo_point geography(Point, 4326),
  city TEXT,
  country TEXT,
  is_virtual BOOLEAN NOT NULL DEFAULT FALSE,
  time_window time_window_type NOT NULL,
  start_at TIMESTAMPTZ,
  end_at   TIMESTAMPTZ,
  total_duration INTERVAL GENERATED ALWAYS AS (end_at - start_at) STORED,
  max_participants INT, -- NULL => open
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE plan_difficulty (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  plan_id UUID NOT NULL REFERENCES plan(id) ON DELETE CASCADE,
  difficulty difficulty NOT NULL,
  level_number INT NOT NULL,
  is_crucial BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE plan_rule (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  plan_id UUID NOT NULL REFERENCES plan(id) ON DELETE CASCADE,
  rule_text TEXT NOT NULL,
  display_order INT NOT NULL DEFAULT 0
);

CREATE TABLE task (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  plan_id UUID NOT NULL REFERENCES plan(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  details TEXT,
  crucial BOOLEAN NOT NULL DEFAULT FALSE
);

-- pricing profile snapshot (immutable for audit)
CREATE TABLE price_profile_snapshot (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  currency TEXT NOT NULL, -- 'INR','USD','EUR', ...
  components JSONB NOT NULL, -- [{type:'GST',calc:'PCT',value:18},...]
  is_enforced BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE plan_price (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  plan_id UUID NOT NULL REFERENCES plan(id) ON DELETE CASCADE,
  currency TEXT NOT NULL,
  base_amount NUMERIC(12,2) NOT NULL,
  validity TSRANGE,
  price_profile_snapshot_id UUID NOT NULL REFERENCES price_profile_snapshot(id)
);

CREATE TABLE plan_slot (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  plan_id UUID NOT NULL REFERENCES plan(id) ON DELETE CASCADE,
  capacity_null_means_open INT,
  reserved INT NOT NULL DEFAULT 0,
  available_view INT NOT NULL DEFAULT 0
);

CREATE TABLE enrollment (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL,
  plan_id UUID NOT NULL REFERENCES plan(id),
  mode enrollment_mode NOT NULL,
  status enrollment_status NOT NULL DEFAULT 'PENDING',
  payment_status payment_status NOT NULL DEFAULT 'NONE',
  approval_by UUID,
  enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE task_progress (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  enrollment_id UUID NOT NULL REFERENCES enrollment(id) ON DELETE CASCADE,
  task_id UUID NOT NULL REFERENCES task(id) ON DELETE CASCADE,
  status task_status NOT NULL DEFAULT 'LOCKED',
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (enrollment_id, task_id)
);

CREATE TABLE user_level (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL,
  difficulty difficulty NOT NULL,
  highest_level_reached INT NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, difficulty)
);

CREATE TABLE progression_policy (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name TEXT NOT NULL,
  scope TEXT NOT NULL, -- 'GLOBAL'|'COHORT'|'USER'
  scope_ref TEXT,
  policy_json JSONB NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE geofence_rule (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  enabled BOOLEAN NOT NULL DEFAULT FALSE,
  scope TEXT NOT NULL,  -- 'CITY'|'STATE'|'COUNTRY'
  values JSONB NOT NULL, -- e.g. ["Hyderabad","Mumbai"]
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
