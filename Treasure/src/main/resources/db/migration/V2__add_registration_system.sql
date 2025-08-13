-- Add registration system tables and columns

-- Create registration_sequence table for tracking registration ID sequences
CREATE TABLE registration_sequence (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    month_year VARCHAR(4) NOT NULL,
    enrollment_type VARCHAR(20) NOT NULL,
    plan_id UUID NOT NULL REFERENCES plan(id) ON DELETE CASCADE,
    current_sequence BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_registration_sequence UNIQUE (month_year, enrollment_type, plan_id)
);

-- Add new columns to enrollment table
ALTER TABLE enrollment 
ADD COLUMN enrollment_type VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL',
ADD COLUMN registration_id VARCHAR(50) UNIQUE,
ADD COLUMN team_name VARCHAR(255),
ADD COLUMN team_size INTEGER;

-- Create index on registration_id for fast lookups
CREATE INDEX idx_enrollment_registration_id ON enrollment(registration_id);

-- Create index on enrollment_type for filtering
CREATE INDEX idx_enrollment_type ON enrollment(enrollment_type);

-- Add check constraint for team enrollments
ALTER TABLE enrollment
ADD CONSTRAINT chk_team_enrollment
CHECK (
    (enrollment_type = 'INDIVIDUAL' AND team_name IS NULL AND team_size IS NULL) OR
    (enrollment_type = 'TEAM' AND team_name IS NOT NULL AND team_size >= 2)
);

-- Create user_statistics table for comprehensive user performance tracking
CREATE TABLE user_statistics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    total_plans_enrolled INTEGER NOT NULL DEFAULT 0,
    total_plans_completed INTEGER NOT NULL DEFAULT 0,
    total_tasks_completed INTEGER NOT NULL DEFAULT 0,
    highest_level_reached INTEGER NOT NULL DEFAULT 0,
    total_score DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    average_completion_time_minutes INTEGER,
    fastest_completion_time_minutes INTEGER,
    current_rank INTEGER,
    best_rank_achieved INTEGER,
    total_achievements INTEGER NOT NULL DEFAULT 0,
    current_streak_days INTEGER NOT NULL DEFAULT 0,
    longest_streak_days INTEGER NOT NULL DEFAULT 0,
    last_activity_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_statistics UNIQUE (user_id, difficulty)
);

-- Create leaderboard table for rankings
CREATE TABLE leaderboard (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    leaderboard_type VARCHAR(20) NOT NULL,
    rank_position INTEGER NOT NULL,
    total_score DECIMAL(12,2) NOT NULL,
    plans_completed INTEGER NOT NULL,
    tasks_completed INTEGER NOT NULL,
    average_completion_time_minutes INTEGER,
    period_start TIMESTAMPTZ,
    period_end TIMESTAMPTZ,
    enrollment_type VARCHAR(20),
    team_name VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create promo_code table for discount system
CREATE TABLE promo_code (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200) NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    max_discount_amount DECIMAL(10,2),
    min_order_amount DECIMAL(10,2),
    usage_limit INTEGER,
    usage_count INTEGER NOT NULL DEFAULT 0,
    usage_limit_per_user INTEGER,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    applicable_enrollment_type VARCHAR(20),
    applicable_difficulty VARCHAR(20),
    applicable_plan_ids TEXT,
    first_time_users_only BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create promo_code_usage table
CREATE TABLE promo_code_usage (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    promo_code_id UUID NOT NULL REFERENCES promo_code(id),
    user_id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    order_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL,
    final_amount DECIMAL(10,2) NOT NULL,
    used_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create promotion table
CREATE TABLE promotion (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    promotion_type VARCHAR(20) NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    max_discount_amount DECIMAL(10,2),
    min_order_amount DECIMAL(10,2),
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    applicable_enrollment_type VARCHAR(20),
    applicable_difficulty VARCHAR(20),
    applicable_plan_ids TEXT,
    first_time_users_only BOOLEAN NOT NULL DEFAULT FALSE,
    priority INTEGER NOT NULL DEFAULT 0,
    can_stack_with_promo_codes BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes for leaderboard performance
CREATE INDEX idx_leaderboard_difficulty_rank ON leaderboard(difficulty, rank_position);
CREATE INDEX idx_leaderboard_period ON leaderboard(leaderboard_type, period_start, period_end);
CREATE INDEX idx_leaderboard_user ON leaderboard(user_id);
CREATE INDEX idx_user_statistics_user_difficulty ON user_statistics(user_id, difficulty);
CREATE INDEX idx_user_statistics_difficulty_score ON user_statistics(difficulty, total_score DESC);

-- Create indexes for promo codes and promotions
CREATE INDEX idx_promo_code_code ON promo_code(code);
CREATE INDEX idx_promo_code_active ON promo_code(is_active, valid_from, valid_until);
CREATE INDEX idx_promo_usage_user ON promo_code_usage(user_id);
CREATE INDEX idx_promo_usage_code ON promo_code_usage(promo_code_id);
CREATE INDEX idx_promo_usage_enrollment ON promo_code_usage(enrollment_id);
CREATE INDEX idx_promotion_active ON promotion(is_active, start_date, end_date);
CREATE INDEX idx_promotion_type ON promotion(promotion_type);
