const { query, getConnection } = require('../config/db');

const VALID_PROGRAMMES = ['CS', 'IT', 'DS'];

/**
 * GET /api/students/profile
 */
async function getProfile(req, res) {
  const studentNumber = req.user.studentNumber;

  try {
    const [rows] = await query(
      `SELECT s.student_number, s.full_name, s.programme_code, s.lab_group_id, s.version, s.is_deleted,
              g.name AS group_name, g.max_capacity
       FROM students s
       LEFT JOIN lab_groups g ON g.id = s.lab_group_id
       WHERE s.student_number = ? AND s.is_deleted = FALSE`,
      [studentNumber]
    );

    if (rows.length === 0) {
      return res.status(404).json({
        success: false,
        error: {
          code: 'NOT_FOUND',
          message: 'Student record not found.'
        }
      });
    }

    const student = rows[0];
    let groupOccupancy = 0;
    const groupCapacity = student.max_capacity || 15;

    if (student.lab_group_id) {
      const [countRows] = await query(
        'SELECT COUNT(*) AS total FROM students WHERE lab_group_id = ? AND is_deleted = FALSE',
        [student.lab_group_id]
      );
      groupOccupancy = Number(countRows[0].total);
    }

    return res.status(200).json({
      success: true,
      data: {
        studentNumber: student.student_number,
        name: student.full_name,
        programme: student.programme_code,
        labGroup: student.lab_group_id || 'Unassigned',
        groupOccupancy,
        groupCapacity,
        isGroupConfirmed: student.lab_group_id !== null,
        syncStatus: 'SYNCED',
        version: student.version
      }
    });
  } catch (error) {
    console.error('getProfile error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Failed to retrieve profile: ' + error.message
      }
    });
  }
}

/**
 * PUT /api/students/profile
 */
async function updateProfile(req, res) {
  const studentNumber = req.user.studentNumber;
  const { name, programme } = req.body;

  if (name !== undefined) {
    if (typeof name !== 'string' || name.trim().length < 2 || name.trim().length > 100) {
      return res.status(400).json({
        success: false,
        error: {
          code: 'VALIDATION_ERROR',
          message: 'Name must be between 2 and 100 characters.',
          field: 'name'
        }
      });
    }
  }

  if (programme !== undefined) {
    if (!VALID_PROGRAMMES.includes(String(programme).trim().toUpperCase())) {
      return res.status(400).json({
        success: false,
        error: {
          code: 'VALIDATION_ERROR',
          message: 'Programme must be one of: CS, IT, DS.',
          field: 'programme'
        }
      });
    }
  }

  const cleanName = name ? name.trim() : null;
  const cleanProgramme = programme ? String(programme).trim().toUpperCase() : null;

  try {
    const [studentRows] = await query(
      'SELECT student_number, full_name, programme_code, version FROM students WHERE student_number = ? AND is_deleted = FALSE',
      [studentNumber]
    );

    if (studentRows.length === 0) {
      return res.status(404).json({
        success: false,
        error: {
          code: 'NOT_FOUND',
          message: 'Student record not found.'
        }
      });
    }

    const current = studentRows[0];
    const newName = cleanName || current.full_name;
    const newProgramme = cleanProgramme || current.programme_code;
    const newVersion = current.version + 1;

    await query(
      `UPDATE students
       SET full_name = ?, programme_code = ?, version = ?, updated_at = NOW()
       WHERE student_number = ?`,
      [newName, newProgramme, newVersion, studentNumber]
    );

    return res.status(200).json({
      success: true,
      data: {
        studentNumber,
        name: newName,
        programme: newProgramme,
        updatedAt: new Date().toISOString(),
        version: newVersion
      },
      message: 'Profile updated successfully.'
    });
  } catch (error) {
    console.error('updateProfile error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Failed to update profile: ' + error.message
      }
    });
  }
}

/**
 * GET /api/students/my-group
 */
async function getMyGroup(req, res) {
  const studentNumber = req.user.studentNumber;

  try {
    const [studentRows] = await query(
      `SELECT s.lab_group_id, g.id AS group_id, g.name AS group_name, g.max_capacity,
              g.day_of_week, g.start_time, g.end_time, g.venue, g.instructor_name
       FROM students s
       LEFT JOIN lab_groups g ON g.id = s.lab_group_id
       WHERE s.student_number = ? AND s.is_deleted = FALSE`,
      [studentNumber]
    );

    if (studentRows.length === 0) {
      return res.status(404).json({
        success: false,
        error: {
          code: 'NOT_FOUND',
          message: 'Student record not found.'
        }
      });
    }

    const record = studentRows[0];

    if (!record.lab_group_id || !record.group_id) {
      return res.status(200).json({
        success: true,
        data: {
          groupId: null,
          groupName: 'Unassigned',
          schedule: 'Not scheduled',
          venue: 'N/A',
          instructor: 'N/A',
          enrolledCount: 0,
          maxCapacity: 15,
          status: 'UNASSIGNED',
          availableSeats: 0
        },
        message: 'No lab group currently assigned.'
      });
    }

    const [countRows] = await query(
      'SELECT COUNT(*) AS total FROM students WHERE lab_group_id = ? AND is_deleted = FALSE',
      [record.group_id]
    );
    const enrolledCount = Number(countRows[0].total);
    const maxCapacity = record.max_capacity || 15;
    const availableSeats = Math.max(0, maxCapacity - enrolledCount);

    let status = 'AVAILABLE';
    if (enrolledCount >= maxCapacity) {
      status = 'FULL';
    } else if (enrolledCount >= 12) {
      status = 'ALMOST_FULL';
    }

    const startTimeStr = record.start_time ? String(record.start_time).substring(0, 5) : '';
    const endTimeStr = record.end_time ? String(record.end_time).substring(0, 5) : '';
    const schedule = `${record.day_of_week} ${startTimeStr} – ${endTimeStr}`;

    return res.status(200).json({
      success: true,
      data: {
        groupId: record.group_id,
        groupName: record.group_name,
        schedule,
        venue: record.venue,
        instructor: record.instructor_name,
        enrolledCount,
        maxCapacity,
        status,
        availableSeats
      }
    });
  } catch (error) {
    console.error('getMyGroup error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Failed to retrieve group details: ' + error.message
      }
    });
  }
}

/**
 * POST /api/requests/change-group
 */
async function submitChangeRequest(req, res) {
  const studentNumber = req.user.studentNumber;
  const { targetGroupId, reason } = req.body;

  if (!targetGroupId || typeof targetGroupId !== 'string') {
    return res.status(400).json({
      success: false,
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Target group ID is required.',
        field: 'targetGroupId'
      }
    });
  }

  const cleanTargetGroupId = targetGroupId.trim().toUpperCase();

  if (!reason || typeof reason !== 'string' || reason.trim().length < 5 || reason.trim().length > 500) {
    return res.status(400).json({
      success: false,
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Reason must be between 5 and 500 characters.',
        field: 'reason'
      }
    });
  }

  const cleanReason = reason.trim();

  try {
    const [studentRows] = await query(
      'SELECT student_number, lab_group_id FROM students WHERE student_number = ? AND is_deleted = FALSE',
      [studentNumber]
    );

    if (studentRows.length === 0) {
      return res.status(404).json({
        success: false,
        error: {
          code: 'NOT_FOUND',
          message: 'Student record not found.'
        }
      });
    }

    const currentGroupId = studentRows[0].lab_group_id;

    if (!currentGroupId) {
      return res.status(400).json({
        success: false,
        error: {
          code: 'VALIDATION_ERROR',
          message: 'Cannot request a group transfer when you are not assigned to any group.'
        }
      });
    }

    if (currentGroupId === cleanTargetGroupId) {
      return res.status(400).json({
        success: false,
        error: {
          code: 'VALIDATION_ERROR',
          message: 'Target group must be different from your current lab group.',
          field: 'targetGroupId'
        }
      });
    }

    const [targetRows] = await query(
      'SELECT id, name FROM lab_groups WHERE id = ?',
      [cleanTargetGroupId]
    );

    if (targetRows.length === 0) {
      return res.status(404).json({
        success: false,
        error: {
          code: 'NOT_FOUND',
          message: `Target lab group ${cleanTargetGroupId} does not exist.`,
          field: 'targetGroupId'
        }
      });
    }

    const [pendingRows] = await query(
      'SELECT id FROM group_change_requests WHERE student_number = ? AND status = "PENDING"',
      [studentNumber]
    );

    if (pendingRows.length > 0) {
      return res.status(409).json({
        success: false,
        error: {
          code: 'ACTIVE_REQUEST_EXISTS',
          message: 'You already have a pending group change request awaiting review.'
        }
      });
    }

    const requestId = `REQ-${Date.now()}`;
    await query(
      `INSERT INTO group_change_requests (id, student_number, current_group_id, target_group_id, reason, status, created_at)
       VALUES (?, ?, ?, ?, ?, 'PENDING', NOW())`,
      [requestId, studentNumber, currentGroupId, cleanTargetGroupId, cleanReason]
    );

    return res.status(201).json({
      success: true,
      data: {
        requestId,
        studentNumber,
        currentGroupId,
        targetGroupId: cleanTargetGroupId,
        reason: cleanReason,
        status: 'PENDING',
        submittedAt: new Date().toISOString()
      },
      message: 'Your transfer request has been submitted for lecturer review.'
    });
  } catch (error) {
    console.error('submitChangeRequest error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Failed to submit change request: ' + error.message
      }
    });
  }
}

module.exports = {
  getProfile,
  updateProfile,
  getMyGroup,
  submitChangeRequest
};
