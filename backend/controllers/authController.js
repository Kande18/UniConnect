const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const { query, getConnection } = require('../config/db');
const { signToken, JWT_EXPIRES_IN } = require('../utils/jwt');

const VALID_PROGRAMMES = ['CS', 'IT', 'DS'];

/**
 * POST /api/auth/register
 * Student self-registration with department claim code validation.
 */
async function registerStudent(req, res) {
  const { studentNumber, name, programme, claimCode, password } = req.body;

  if (!studentNumber || !/^\d{9}$/.test(String(studentNumber).trim())) {
    return res.status(400).json({
      success: false,
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Student number must contain exactly 9 numeric digits.',
        field: 'studentNumber'
      }
    });
  }

  const cleanStudentNumber = String(studentNumber).trim();

  if (!name || typeof name !== 'string' || name.trim().length < 2 || name.trim().length > 100) {
    return res.status(400).json({
      success: false,
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Name must be between 2 and 100 characters.',
        field: 'name'
      }
    });
  }

  const cleanName = name.trim();

  if (!programme || !VALID_PROGRAMMES.includes(programme.toUpperCase())) {
    return res.status(400).json({
      success: false,
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Programme must be one of: CS, IT, DS.',
        field: 'programme'
      }
    });
  }

  const cleanProgramme = programme.toUpperCase();

  if (!claimCode || typeof claimCode !== 'string' || claimCode.trim().length === 0) {
    return res.status(400).json({
      success: false,
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Department claim code is required.',
        field: 'claimCode'
      }
    });
  }

  const cleanClaimCode = claimCode.trim();

  if (!password || typeof password !== 'string' || password.length < 6) {
    return res.status(400).json({
      success: false,
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Password must be at least 6 characters.',
        field: 'password'
      }
    });
  }

  const connection = await getConnection();
  try {
    await connection.beginTransaction();

    // 2. Check for duplicate student number
    const [existingStudent] = await connection.query(
      'SELECT student_number FROM students WHERE student_number = ?',
      [cleanStudentNumber]
    );

    const [existingUser] = await connection.query(
      'SELECT id FROM users WHERE identifier = ?',
      [cleanStudentNumber]
    );

    if (existingStudent.length > 0 || existingUser.length > 0) {
      await connection.rollback();
      return res.status(409).json({
        success: false,
        error: {
          code: 'DUPLICATE_STUDENT_NUMBER',
          message: 'Student number is already registered in the university system.',
          field: 'studentNumber'
        }
      });
    }

    // 3. Validate claim code
    const [claimRows] = await connection.query(
      'SELECT code, programme_code, is_used FROM claim_codes WHERE code = ?',
      [cleanClaimCode]
    );

    if (claimRows.length === 0) {
      await connection.rollback();
      return res.status(400).json({
        success: false,
        error: {
          code: 'INVALID_CLAIM_CODE',
          message: 'Provided claim code does not exist.',
          field: 'claimCode'
        }
      });
    }

    const claim = claimRows[0];
    if (claim.is_used) {
      await connection.rollback();
      return res.status(400).json({
        success: false,
        error: {
          code: 'INVALID_CLAIM_CODE',
          message: 'Provided claim code has already been used.',
          field: 'claimCode'
        }
      });
    }

    if (claim.programme_code !== cleanProgramme) {
      await connection.rollback();
      return res.status(400).json({
        success: false,
        error: {
          code: 'INVALID_CLAIM_CODE',
          message: `Claim code is valid for programme ${claim.programme_code}, but requested for ${cleanProgramme}.`,
          field: 'claimCode'
        }
      });
    }

    // 4. Hash password and insert records
    const passwordHash = await bcrypt.hash(password, 10);
    const userId = crypto.randomUUID();

    await connection.query(
      `INSERT INTO users (id, identifier, password_hash, role, is_active, created_at, updated_at)
       VALUES (?, ?, ?, 'STUDENT', TRUE, NOW(), NOW())`,
      [userId, cleanStudentNumber, passwordHash]
    );

    await connection.query(
      `INSERT INTO students (student_number, user_id, full_name, programme_code, lab_group_id, version, is_deleted, created_at, updated_at)
       VALUES (?, ?, ?, ?, NULL, 1, FALSE, NOW(), NOW())`,
      [cleanStudentNumber, userId, cleanName, cleanProgramme]
    );

    await connection.query(
      `UPDATE claim_codes
       SET is_used = TRUE, used_by_student = ?, used_at = NOW()
       WHERE code = ?`,
      [cleanStudentNumber, cleanClaimCode]
    );

    await connection.commit();

    return res.status(201).json({
      success: true,
      data: {
        studentNumber: cleanStudentNumber,
        name: cleanName,
        programme: cleanProgramme,
        assignedGroup: null,
        status: 'REGISTERED',
        createdAt: new Date().toISOString()
      },
      message: 'Student registration completed. You can now sign in.'
    });
  } catch (error) {
    await connection.rollback();
    console.error('Registration error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Failed to complete registration: ' + error.message
      }
    });
  } finally {
    connection.release();
  }
}

/**
 * POST /api/auth/student-login
 */
async function studentLogin(req, res) {
  const { studentNumber, password } = req.body;

  if (!studentNumber || !password) {
    return res.status(400).json({
      success: false,
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Student number and password are required.'
      }
    });
  }

  const cleanStudentNumber = String(studentNumber).trim();

  try {
    const [userRows] = await query(
      `SELECT u.id, u.identifier, u.password_hash, u.is_active, s.full_name, s.programme_code, s.lab_group_id, s.is_deleted
       FROM users u
       JOIN students s ON s.user_id = u.id
       WHERE u.identifier = ? AND u.role = 'STUDENT'`,
      [cleanStudentNumber]
    );

    if (userRows.length === 0) {
      return res.status(401).json({
        success: false,
        error: {
          code: 'UNAUTHORIZED',
          message: 'Invalid student number or password.'
        }
      });
    }

    const user = userRows[0];

    if (!user.is_active || user.is_deleted) {
      return res.status(403).json({
        success: false,
        error: {
          code: 'FORBIDDEN',
          message: 'Account has been deactivated or disabled.'
        }
      });
    }

    const passwordMatch = await bcrypt.compare(password, user.password_hash);
    if (!passwordMatch) {
      return res.status(401).json({
        success: false,
        error: {
          code: 'UNAUTHORIZED',
          message: 'Invalid student number or password.'
        }
      });
    }

    const token = signToken({
      userId: user.id,
      identifier: user.identifier,
      role: 'STUDENT',
      studentNumber: cleanStudentNumber
    });

    return res.status(200).json({
      success: true,
      data: {
        token,
        expiresIn: JWT_EXPIRES_IN,
        user: {
          role: 'STUDENT',
          studentNumber: cleanStudentNumber,
          name: user.full_name,
          programme: user.programme_code,
          labGroup: user.lab_group_id || 'Unassigned'
        }
      }
    });
  } catch (error) {
    console.error('Student login error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Internal login error: ' + error.message
      }
    });
  }
}

/**
 * POST /api/auth/lecturer-login
 */
async function lecturerLogin(req, res) {
  const { staffId, password } = req.body;

  if (!staffId || !password) {
    return res.status(400).json({
      success: false,
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Staff ID and password are required.'
      }
    });
  }

  const cleanStaffId = String(staffId).trim();

  try {
    const [userRows] = await query(
      `SELECT u.id, u.identifier, u.password_hash, u.is_active, l.full_name, l.department, l.academic_title
       FROM users u
       JOIN lecturers l ON l.user_id = u.id
       WHERE u.identifier = ? AND u.role = 'LECTURER'`,
      [cleanStaffId]
    );

    if (userRows.length === 0) {
      return res.status(401).json({
        success: false,
        error: {
          code: 'UNAUTHORIZED',
          message: 'Invalid staff identifier or password.'
        }
      });
    }

    const user = userRows[0];

    if (!user.is_active) {
      return res.status(403).json({
        success: false,
        error: {
          code: 'FORBIDDEN',
          message: 'Staff account has been deactivated.'
        }
      });
    }

    const passwordMatch = await bcrypt.compare(password, user.password_hash);
    if (!passwordMatch) {
      return res.status(401).json({
        success: false,
        error: {
          code: 'UNAUTHORIZED',
          message: 'Invalid staff identifier or password.'
        }
      });
    }

    const token = signToken({
      userId: user.id,
      identifier: user.identifier,
      role: 'LECTURER',
      staffId: cleanStaffId
    });

    return res.status(200).json({
      success: true,
      data: {
        token,
        expiresIn: JWT_EXPIRES_IN,
        user: {
          role: 'LECTURER',
          staffId: cleanStaffId,
          name: user.full_name,
          department: user.department,
          title: user.academic_title
        }
      }
    });
  } catch (error) {
    console.error('Lecturer login error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Internal login error: ' + error.message
      }
    });
  }
}

module.exports = {
  registerStudent,
  studentLogin,
  lecturerLogin
};
