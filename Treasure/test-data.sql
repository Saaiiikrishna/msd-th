-- Insert test data for comprehensive API testing

-- Insert categories
INSERT INTO category (id, name, description, audience, active) VALUES 
('550e8400-e29b-41d4-a716-446655440001', 'Technology', 'Technology and programming courses', 'INDIVIDUAL'::audience_type, true),
('550e8400-e29b-41d4-a716-446655440002', 'Arts', 'Creative arts and design courses', 'GROUP'::audience_type, true);

-- Insert subcategories
INSERT INTO subcategory (id, name, description, category_id, active) VALUES 
('550e8400-e29b-41d4-a716-446655440011', 'Programming', 'Software development and programming', '550e8400-e29b-41d4-a716-446655440001', true),
('550e8400-e29b-41d4-a716-446655440012', 'Web Development', 'Frontend and backend web development', '550e8400-e29b-41d4-a716-446655440001', true),
('550e8400-e29b-41d4-a716-446655440021', 'Digital Art', 'Digital art and illustration', '550e8400-e29b-41d4-a716-446655440002', true);

-- Insert age bands
INSERT INTO age_band (id, name, min_age, max_age, active) VALUES 
('550e8400-e29b-41d4-a716-446655440031', 'Kids', 6, 12, true),
('550e8400-e29b-41d4-a716-446655440032', 'Teens', 13, 17, true),
('550e8400-e29b-41d4-a716-446655440033', 'Adults', 18, 65, true);
