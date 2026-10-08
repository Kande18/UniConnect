-- ============================================================================
-- UNICONNECT SEED DATA (PostgreSQL / Supabase)
-- Passwords:
--   Students: 'Password123!'
--   Lecturers: 'LecturerPass2026!'
-- ============================================================================

-- 1. Insert Programmes
INSERT INTO programmes (code, name, department) VALUES
('CS', 'Computer Science', 'Computer Science & IT'),
('IT', 'Information Technology', 'Computer Science & IT'),
('DS', 'Data Science', 'Computer Science & IT')
ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name;

-- 2. Insert Lab Groups with Venues & Instructors
INSERT INTO lab_groups (id, name, max_capacity, day_of_week, start_time, end_time, venue, instructor_name) VALUES
('G01', 'Lab Group 01', 15, 'Monday',    '08:00:00', '10:00:00', 'Lab 3B (Systems Lab)',   'Prof. Davis'),
('G02', 'Lab Group 02', 15, 'Tuesday',   '14:00:00', '16:00:00', 'Lab 1A (Main Lab)',      'Dr. A. Banda'),
('G03', 'Lab Group 03', 15, 'Wednesday', '10:00:00', '12:00:00', 'Lab 2C (Software Lab)',  'Dr. Phiri'),
('G04', 'Lab Group 04', 15, 'Thursday',  '16:00:00', '18:00:00', 'Lab 3B (Networking Lab)','Prof. Davis')
ON CONFLICT (id) DO UPDATE SET venue = EXCLUDED.venue;

-- 3. Insert Department Claim Codes
INSERT INTO claim_codes (code, programme_code, is_used) VALUES
('CLAIM-CS-2024', 'CS', FALSE),
('CLAIM-IT-2024', 'IT', FALSE),
('CLAIM-DS-2024', 'DS', FALSE),
('CLAIM-TEST-99', 'CS', FALSE)
ON CONFLICT (code) DO UPDATE SET programme_code = EXCLUDED.programme_code;

-- 4. Insert Lecturer Account (Dr. A. Banda)
INSERT INTO users (id, identifier, password_hash, role) VALUES
('usr-lecturer-001', 'STF-2024-88', '$2a$10$q2pWKpWP/7.oIGFFKwj.Ve0H3mBmac8VcUvOF3l.uDkg2iOxy/lXu', 'LECTURER')
ON CONFLICT (identifier) DO UPDATE SET password_hash = EXCLUDED.password_hash;

INSERT INTO lecturers (staff_id, user_id, full_name, department, academic_title) VALUES
('STF-2024-88', 'usr-lecturer-001', 'Dr. A. Banda', 'Computer Science & IT', 'Senior Lab Coordinator')
ON CONFLICT (staff_id) DO UPDATE SET full_name = EXCLUDED.full_name;

-- 5. Insert Primary Student (Peculiar Banda)
INSERT INTO users (id, identifier, password_hash, role) VALUES
('usr-student-001', '202400123', '$2a$10$GV2BhsMu28vtk5q4TdvafuiwRRDXc9BsMc2fW95LCsN5ujF0p9HM2', 'STUDENT')
ON CONFLICT (identifier) DO UPDATE SET password_hash = EXCLUDED.password_hash;

INSERT INTO students (student_number, user_id, full_name, programme_code, lab_group_id, version) VALUES
('202400123', 'usr-student-001', 'Peculiar Banda', 'CS', 'G01', 1)
ON CONFLICT (student_number) DO UPDATE SET full_name = EXCLUDED.full_name;

-- 6. Insert Cohort Mock Students (Matching Roster in UI)
INSERT INTO students (student_number, user_id, full_name, programme_code, lab_group_id, version) VALUES
('202400456', NULL, 'Amara Dlamini', 'IT', 'G01', 1),
('202400789', NULL, 'Tendai Moyo',   'DS', 'G02', 1),
('202401011', NULL, 'Nkosi Zulu',    'CS', 'G02', 1),
('202401234', NULL, 'Fatima Patel',  'IT', 'G03', 1),
('202401567', NULL, 'Sipho Ndlovu',  'DS', 'G03', 1),
('202401890', NULL, 'Grace Mwangi',  'CS', 'G04', 1),
('202402123', NULL, 'Chidi Okafor',  'IT', NULL,  1)
ON CONFLICT (student_number) DO UPDATE SET full_name = EXCLUDED.full_name;

-- 7. Insert Initial Transfer Requests (Matching Lecturer Tab 4)
INSERT INTO group_change_requests (id, student_number, current_group_id, target_group_id, reason, status) VALUES
('REQ-1001', '202400123', 'G01', 'G02', 'Timetable conflict with CS302 Advanced Algorithms lecture on Monday mornings.', 'PENDING'),
('REQ-1002', '202400789', 'G02', 'G03', 'Medical appointment scheduled weekly during Tuesday afternoon slot.', 'PENDING')
ON CONFLICT (id) DO UPDATE SET reason = EXCLUDED.reason;
