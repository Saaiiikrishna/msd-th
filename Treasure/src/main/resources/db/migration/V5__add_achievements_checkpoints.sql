-- Add achievements and checkpoints system for treasure hunt gamification

-- Create achievement_type table for different types of achievements
CREATE TABLE achievement_type (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL, -- 'COMPLETION', 'SPEED', 'STREAK', 'SOCIAL', 'SPECIAL'
    icon_url TEXT,
    badge_color VARCHAR(20),
    points_awarded INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create user_achievements table for tracking user achievements
CREATE TABLE user_achievements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    achievement_type_id UUID NOT NULL REFERENCES achievement_type(id),
    enrollment_id UUID REFERENCES enrollment(id), -- Optional: specific to an enrollment
    plan_id UUID REFERENCES plan(id), -- Optional: specific to a plan
    
    -- Achievement details
    earned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    points_earned INTEGER NOT NULL DEFAULT 0,
    progress_data JSONB, -- Store additional progress information
    
    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uk_user_achievement UNIQUE (user_id, achievement_type_id, enrollment_id)
);

-- Create checkpoints table for waypoints/locations in treasure hunts
CREATE TABLE checkpoints (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_id UUID NOT NULL REFERENCES plan(id) ON DELETE CASCADE,
    task_id UUID REFERENCES task(id) ON DELETE CASCADE, -- Optional: link to specific task
    
    -- Checkpoint details
    name VARCHAR(200) NOT NULL,
    description TEXT,
    checkpoint_order INTEGER NOT NULL, -- Order within the plan
    
    -- Location data
    geo_point geography(Point, 4326) NOT NULL,
    address_text TEXT,
    landmark_description TEXT,
    
    -- Validation requirements
    requires_photo BOOLEAN NOT NULL DEFAULT FALSE,
    requires_qr_scan BOOLEAN NOT NULL DEFAULT FALSE,
    qr_code_data TEXT, -- QR code content for validation
    validation_radius_meters INTEGER NOT NULL DEFAULT 50, -- GPS accuracy requirement
    
    -- Timing
    estimated_duration_minutes INTEGER,
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create checkpoint_progress table for tracking user progress at checkpoints
CREATE TABLE checkpoint_progress (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enrollment_id UUID NOT NULL REFERENCES enrollment(id) ON DELETE CASCADE,
    checkpoint_id UUID NOT NULL REFERENCES checkpoints(id) ON DELETE CASCADE,
    
    -- Progress details
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'REACHED', 'COMPLETED', 'SKIPPED'
    reached_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    
    -- Validation data
    validation_photo_url TEXT,
    validation_location geography(Point, 4326),
    validation_accuracy_meters DECIMAL(8,2),
    qr_scan_data TEXT,
    
    -- Timing
    time_spent_minutes INTEGER,
    
    -- Metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uk_enrollment_checkpoint UNIQUE (enrollment_id, checkpoint_id),
    CONSTRAINT chk_checkpoint_status CHECK (status IN ('PENDING', 'REACHED', 'COMPLETED', 'SKIPPED'))
);

-- Create user_stats table (comprehensive stats beyond user_statistics)
CREATE TABLE user_stats (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    
    -- Overall statistics
    total_enrollments INTEGER NOT NULL DEFAULT 0,
    completed_enrollments INTEGER NOT NULL DEFAULT 0,
    cancelled_enrollments INTEGER NOT NULL DEFAULT 0,
    
    -- Performance metrics
    total_checkpoints_reached INTEGER NOT NULL DEFAULT 0,
    total_checkpoints_completed INTEGER NOT NULL DEFAULT 0,
    total_distance_traveled_km DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_time_spent_minutes INTEGER NOT NULL DEFAULT 0,
    
    -- Achievement metrics
    total_achievements_earned INTEGER NOT NULL DEFAULT 0,
    total_points_earned INTEGER NOT NULL DEFAULT 0,
    
    -- Streak tracking
    current_enrollment_streak INTEGER NOT NULL DEFAULT 0,
    longest_enrollment_streak INTEGER NOT NULL DEFAULT 0,
    current_completion_streak INTEGER NOT NULL DEFAULT 0,
    longest_completion_streak INTEGER NOT NULL DEFAULT 0,
    
    -- Social metrics
    total_team_enrollments INTEGER NOT NULL DEFAULT 0,
    total_individual_enrollments INTEGER NOT NULL DEFAULT 0,
    favorite_difficulty VARCHAR(20),
    favorite_city VARCHAR(100),
    
    -- Timestamps
    first_enrollment_at TIMESTAMPTZ,
    last_enrollment_at TIMESTAMPTZ,
    last_completion_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT uk_user_stats UNIQUE (user_id)
);

-- Insert default achievement types
INSERT INTO achievement_type (name, description, category, points_awarded) VALUES
('First Steps', 'Complete your first treasure hunt', 'COMPLETION', 100),
('Speed Demon', 'Complete a treasure hunt in record time', 'SPEED', 200),
('Team Player', 'Complete 5 team treasure hunts', 'SOCIAL', 150),
('Explorer', 'Complete treasure hunts in 5 different cities', 'COMPLETION', 300),
('Streak Master', 'Complete 10 treasure hunts in a row', 'STREAK', 500),
('Photo Hunter', 'Take 50 checkpoint photos', 'COMPLETION', 100),
('QR Scanner', 'Scan 100 QR codes', 'COMPLETION', 100),
('Distance Walker', 'Walk 100km across all treasure hunts', 'COMPLETION', 250),
('Night Owl', 'Complete 5 night-time treasure hunts', 'SPECIAL', 200),
('Early Bird', 'Complete 5 morning treasure hunts', 'SPECIAL', 200);

-- Create indexes for performance
CREATE INDEX idx_user_achievements_user ON user_achievements(user_id);
CREATE INDEX idx_user_achievements_type ON user_achievements(achievement_type_id);
CREATE INDEX idx_user_achievements_earned ON user_achievements(earned_at);
CREATE INDEX idx_user_achievements_enrollment ON user_achievements(enrollment_id);

CREATE INDEX idx_checkpoints_plan ON checkpoints(plan_id);
CREATE INDEX idx_checkpoints_order ON checkpoints(plan_id, checkpoint_order);
CREATE INDEX idx_checkpoints_geo ON checkpoints USING GIST(geo_point);
CREATE INDEX idx_checkpoints_active ON checkpoints(is_active);

CREATE INDEX idx_checkpoint_progress_enrollment ON checkpoint_progress(enrollment_id);
CREATE INDEX idx_checkpoint_progress_checkpoint ON checkpoint_progress(checkpoint_id);
CREATE INDEX idx_checkpoint_progress_status ON checkpoint_progress(status);
CREATE INDEX idx_checkpoint_progress_completed ON checkpoint_progress(completed_at);

CREATE INDEX idx_user_stats_user ON user_stats(user_id);
CREATE INDEX idx_user_stats_points ON user_stats(total_points_earned DESC);
CREATE INDEX idx_user_stats_completions ON user_stats(completed_enrollments DESC);
CREATE INDEX idx_user_stats_streaks ON user_stats(longest_completion_streak DESC);

CREATE INDEX idx_achievement_type_category ON achievement_type(category);
CREATE INDEX idx_achievement_type_active ON achievement_type(is_active);

-- Add comments for documentation
COMMENT ON TABLE achievement_type IS 'Defines different types of achievements users can earn';
COMMENT ON TABLE user_achievements IS 'Tracks achievements earned by users';
COMMENT ON TABLE checkpoints IS 'Waypoints/locations within treasure hunt plans';
COMMENT ON TABLE checkpoint_progress IS 'User progress at specific checkpoints';
COMMENT ON TABLE user_stats IS 'Comprehensive user statistics and performance metrics';
