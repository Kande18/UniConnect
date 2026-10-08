-- ============================================================================
-- UNICONNECT DATABASE SCHEMA DDL MIGRATION (MySQL 8.0+)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS uniconnect_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE uniconnect_db;

-- 1. USERS TABLE (Authentication credentials & RBAC)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    identifier VARCHAR(50) NOT NULL UNIQUE, -- student_number or staff_id
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('STUDENT', 'LECTURER') NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_identifier (identifier)
) ENGINE=InnoDB;

-- 2. PROGRAMMES TABLE (CS, IT, DS)
CREATE TABLE IF NOT EXISTS programmes (
    code VARCHAR(10) PRIMARY KEY, -- 'CS', 'IT', 'DS'
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL DEFAULT 'Computer Science & IT'
) ENGINE=InnoDB;

-- 3. LAB_GROUPS TABLE (G01–G04, capacity 15, schedule, venue)
CREATE TABLE IF NOT EXISTS lab_groups (
    id VARCHAR(10) PRIMARY KEY, -- 'G01', 'G02', 'G03', 'G04'
    name VARCHAR(50) NOT NULL,
    max_capacity INT NOT NULL DEFAULT 15,
    day_of_week VARCHAR(15) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    venue VARCHAR(100) NOT NULL,
    instructor_name VARCHAR(100) NOT NULL,
    CONSTRAINT chk_capacity CHECK (max_capacity > 0)
) ENGINE=InnoDB;

-- 4. STUDENTS TABLE (Preserves 9-digit numbers, programme, group)
CREATE TABLE IF NOT EXISTS students (
    student_number CHAR(9) PRIMARY KEY, -- Exactly 9 digits, preserves leading zeros
    user_id VARCHAR(36) NULL,
    full_name VARCHAR(100) NOT NULL,
    programme_code VARCHAR(10) NOT NULL,
    lab_group_id VARCHAR(10) NULL,
    version INT NOT NULL DEFAULT 1, -- For optimistic concurrency & offline conflict detection
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE, -- Soft delete for offline sync (§18)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_students_programme FOREIGN KEY (programme_code) REFERENCES programmes(code) ON UPDATE CASCADE,
    CONSTRAINT fk_students_group FOREIGN KEY (lab_group_id) REFERENCES lab_groups(id) ON DELETE SET NULL,
    INDEX idx_students_name (full_name),
    INDEX idx_students_group (lab_group_id),
    INDEX idx_students_updated (updated_at)
) ENGINE=InnoDB;

-- 5. LECTURERS TABLE (Staff profile)
CREATE TABLE IF NOT EXISTS lecturers (
    staff_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    academic_title VARCHAR(50) NOT NULL DEFAULT 'Lecturer',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lecturers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 6. CLAIM_CODES TABLE (Department verification codes §13.2)
CREATE TABLE IF NOT EXISTS claim_codes (
    code VARCHAR(50) PRIMARY KEY,
    programme_code VARCHAR(10) NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    used_by_student CHAR(9) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP NULL,
    CONSTRAINT fk_claims_programme FOREIGN KEY (programme_code) REFERENCES programmes(code) ON UPDATE CASCADE,
    CONSTRAINT fk_claims_student FOREIGN KEY (used_by_student) REFERENCES students(student_number) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 7. GROUP_CHANGE_REQUESTS TABLE (Student transfer workflow §13.7 & §14.7)
CREATE TABLE IF NOT EXISTS group_change_requests (
    id VARCHAR(36) PRIMARY KEY,
    student_number CHAR(9) NOT NULL,
    current_group_id VARCHAR(10) NOT NULL,
    target_group_id VARCHAR(10) NOT NULL,
    reason TEXT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    reviewed_by_staff_id VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP NULL,
    CONSTRAINT fk_req_student FOREIGN KEY (student_number) REFERENCES students(student_number) ON DELETE CASCADE,
    CONSTRAINT fk_req_current_group FOREIGN KEY (current_group_id) REFERENCES lab_groups(id),
    CONSTRAINT fk_req_target_group FOREIGN KEY (target_group_id) REFERENCES lab_groups(id),
    CONSTRAINT fk_req_lecturer FOREIGN KEY (reviewed_by_staff_id) REFERENCES lecturers(staff_id) ON DELETE SET NULL,
    INDEX idx_req_status (status),
    INDEX idx_req_student (student_number)
) ENGINE=InnoDB;
