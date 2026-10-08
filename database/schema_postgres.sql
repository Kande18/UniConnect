-- ============================================================================
-- UNICONNECT DATABASE SCHEMA DDL MIGRATION (PostgreSQL / Supabase)
-- ============================================================================

-- Create Enums if they do not exist
DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('STUDENT', 'LECTURER');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE request_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    identifier VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role user_role NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_identifier ON users(identifier);

-- 2. PROGRAMMES TABLE
CREATE TABLE IF NOT EXISTS programmes (
    code VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL DEFAULT 'Computer Science & IT'
);

-- 3. LAB_GROUPS TABLE
CREATE TABLE IF NOT EXISTS lab_groups (
    id VARCHAR(10) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    max_capacity INT NOT NULL DEFAULT 15,
    day_of_week VARCHAR(15) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    venue VARCHAR(100) NOT NULL,
    instructor_name VARCHAR(100) NOT NULL,
    CONSTRAINT chk_capacity CHECK (max_capacity > 0)
);

-- 4. STUDENTS TABLE
CREATE TABLE IF NOT EXISTS students (
    student_number CHAR(9) PRIMARY KEY,
    user_id VARCHAR(36) NULL REFERENCES users(id) ON DELETE SET NULL,
    full_name VARCHAR(100) NOT NULL,
    programme_code VARCHAR(10) NOT NULL REFERENCES programmes(code) ON UPDATE CASCADE,
    lab_group_id VARCHAR(10) NULL REFERENCES lab_groups(id) ON DELETE SET NULL,
    version INT NOT NULL DEFAULT 1,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_students_name ON students(full_name);
CREATE INDEX IF NOT EXISTS idx_students_group ON students(lab_group_id);
CREATE INDEX IF NOT EXISTS idx_students_updated ON students(updated_at);

-- 5. LECTURERS TABLE
CREATE TABLE IF NOT EXISTS lecturers (
    staff_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    academic_title VARCHAR(50) NOT NULL DEFAULT 'Lecturer',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. CLAIM_CODES TABLE
CREATE TABLE IF NOT EXISTS claim_codes (
    code VARCHAR(50) PRIMARY KEY,
    programme_code VARCHAR(10) NOT NULL REFERENCES programmes(code) ON UPDATE CASCADE,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    used_by_student CHAR(9) NULL REFERENCES students(student_number) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP WITH TIME ZONE NULL
);

-- 7. GROUP_CHANGE_REQUESTS TABLE
CREATE TABLE IF NOT EXISTS group_change_requests (
    id VARCHAR(36) PRIMARY KEY,
    student_number CHAR(9) NOT NULL REFERENCES students(student_number) ON DELETE CASCADE,
    current_group_id VARCHAR(10) NOT NULL REFERENCES lab_groups(id),
    target_group_id VARCHAR(10) NOT NULL REFERENCES lab_groups(id),
    reason TEXT NOT NULL,
    status request_status NOT NULL DEFAULT 'PENDING',
    reviewed_by_staff_id VARCHAR(50) NULL REFERENCES lecturers(staff_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP WITH TIME ZONE NULL
);
CREATE INDEX IF NOT EXISTS idx_req_status ON group_change_requests(status);
CREATE INDEX IF NOT EXISTS idx_req_student ON group_change_requests(student_number);
