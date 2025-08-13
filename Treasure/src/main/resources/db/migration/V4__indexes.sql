CREATE INDEX IF NOT EXISTS idx_plan_subcategory ON plan(subcategory_id);
CREATE INDEX IF NOT EXISTS idx_plan_time ON plan(start_at, end_at);
CREATE INDEX IF NOT EXISTS idx_plan_city ON plan(city);
CREATE INDEX IF NOT EXISTS idx_plan_geo ON plan USING GIST(geo_point);
CREATE INDEX IF NOT EXISTS idx_price_plan_currency ON plan_price(plan_id, currency);
CREATE INDEX IF NOT EXISTS idx_policy_active ON progression_policy(active);
CREATE INDEX IF NOT EXISTS idx_subcat_ageband ON subcategory_age_band(subcategory_id, age_band_id);
CREATE INDEX IF NOT EXISTS idx_category_tags_gin ON category USING GIN(tags);
