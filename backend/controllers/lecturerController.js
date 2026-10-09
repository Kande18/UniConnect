const { query, getConnection } = require('../config/db');

const VALID_PROGRAMMES = ['CS', 'IT', 'DS'];

/**
 * GET /api/lecturer/dashboard
 */
async function getDashboard(req, res) {
  try {
    const [totalRows] = await query(
      'SELECT COUNT(*) AS total FROM students WHERE is_deleted = FALSE'
    );
    const totalStudents = Number(totalRows[0].total);

    const [unassignedRows] = await query(
      'SELECT COUNT(*) AS total FROM students WHERE lab_group_id IS NULL AND is_deleted = FALSE'
    );
    const unassignedCount = Number(unassignedRows[0].total);

    const [pendingRows] = await query(
      'SELECT COUNT(*) AS total FROM group_change_requests WHERE status = "PENDING"'
    );
    const pendingRequestsCount = Number(pendingRows[0].total);

    const [groupRows] = await query(`
      SELECT g.id, g.name, g.max_capacity, g.venue,
             COUNT(s.student_number) AS enrolled
      FROM lab_groups g
      LEFT JOIN students s ON s.lab_group_id = g.id AND s.is_deleted = FALSE
      GROUP BY g.id, g.name, g.max_capacity, g.venue
      ORDER BY g.id ASC
    `);

    let totalCapacity = 0;
    let totalOccupied = 0;
    let fullGroupsCount = 0;

    const groupSummaries = groupRows.map(g => {
      const enrolled = Number(g.enrolled);
      const capacity = Number(g.max_capacity);
      totalCapacity += capacity;
      totalOccupied += enrolled;

      let status = 'AVAILABLE';
      if (enrolled >= capacity) {
        status = 'FULL';
        fullGroupsCount++;
      } else if (enrolled >= 12) {
        status = 'ALMOST_FULL';
      }

      return {
        id: g.id,
        enrolled,
        capacity,
        status,
        venue: g.venue
      };
    });

    const occupancyPercentage = totalCapacity > 0 ? Math.round((totalOccupied / totalCapacity) * 100) : 0;

    const [recentReqs] = await query(`
      SELECT r.id, r.student_number, s.full_name, r.current_group_id, r.target_group_id,
             r.status, r.created_at
      FROM group_change_requests r
      LEFT JOIN students s ON s.student_number = r.student_number
      ORDER BY r.created_at DESC
      LIMIT 10
    `);

    const recentActivity = recentReqs.map(r => ({
      id: r.id,
      title: `Transfer requested by ${r.full_name || r.student_number}`,
      description: `From ${r.current_group_id} to ${r.target_group_id} (${r.status})`,
      timestamp: r.created_at
    }));

    return res.status(200).json({
      success: true,
      data: {
        metrics: {
          totalStudents,
          unassignedCount,
          fullGroupsCount,
          pendingRequestsCount,
          totalCapacity,
          totalOccupied,
          occupancyPercentage
        },
        groupSummaries,
        recentActivity
      }
    });
  } catch (error) {
    console.error('getDashboard error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Failed to retrieve dashboard: ' + error.message
      }
    });
  }
}

/**
 * GET /api/lecturer/roster
 */
async function getRoster(req, res) {
  const { query: searchQuery, programme, group, page = 1, limit = 50 } = req.query;

  const pageNum = Math.max(1, parseInt(page, 10) || 1);
  const limitNum = Math.min(100, Math.max(1, parseInt(limit, 10) || 50));
  const offset = (pageNum - 1) * limitNum;

  try {
    const [allCountRows] = await query(
      'SELECT COUNT(*) AS total FROM students WHERE is_deleted = FALSE'
    );
    const totalCount = Number(allCountRows[0].total);

    let sqlConditions = ['s.is_deleted = FALSE'];
    let sqlParams = [];

    if (searchQuery && searchQuery.trim()) {
      sqlConditions.push('(s.full_name LIKE ? OR s.student_number LIKE ?)');
      const q = `%${searchQuery.trim()}%`;
      sqlParams.push(q, q);
    }

    if (programme && programme.toUpperCase() !== 'ALL') {
      sqlConditions.push('s.programme_code = ?');
      sqlParams.push(programme.trim().toUpperCase());
    }

    if (group && group.toUpperCase() !== 'ALL') {
      if (group.toLowerCase() === 'unassigned') {
        sqlConditions.push('s.lab_group_id IS NULL');
      } else {
        sqlConditions.push('s.lab_group_id = ?');
        sqlParams.push(group.trim().toUpperCase());
      }
    }

    const whereClause = sqlConditions.join(' AND ');

    const [filteredCountRows] = await query(
      `SELECT COUNT(*) AS total FROM students s WHERE ${whereClause}`,
      sqlParams
    );
    const filteredCount = Number(filteredCountRows[0].total);

    const [students] = await query(
      `SELECT s.student_number, s.full_name, s.programme_code, s.lab_group_id, s.updated_at
       FROM students s
       WHERE ${whereClause}
       ORDER BY s.student_number ASC
       LIMIT ? OFFSET ?`,
      [...sqlParams, limitNum, offset]
    );

    const formattedStudents = students.map(s => ({
      studentNumber: s.student_number,
      name: s.full_name,
      programme: s.programme_code,
      labGroup: s.lab_group_id || 'Unassigned',
      status: 'ACTIVE',
      updatedAt: s.updated_at
    }));

    return res.status(200).json({
      success: true,
      data: {
        totalCount,
        filteredCount,
        page: pageNum,
        limit: limitNum,
        students: formattedStudents
      }
    });
  } catch (error) {
    console.error('getRoster error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Failed to retrieve roster: ' + error.message
      }
    });
  }
}

/**
 * POST /api/lecturer/students
 */
async function createStudent(req, res) {
  const { studentNumber, name, programme, labGroup } = req.body;

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
  let cleanLabGroup = labGroup ? String(labGroup).trim().toUpperCase() : null;
  if (cleanLabGroup === 'UNASSIGNED') cleanLabGroup = null;

  const connection = await getConnection();
  try {
    await connection.beginTransaction();

    const [existing] = await connection.query(
      'SELECT student_number FROM students WHERE student_number = ?',
      [cleanStudentNumber]
    );

    if (existing.length > 0) {
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

    if (cleanLabGroup) {
      const [groupRows] = await connection.query(
        'SELECT id, max_capacity FROM lab_groups WHERE id = ? FOR UPDATE',
        [cleanLabGroup]
      );

      if (groupRows.length === 0) {
        await connection.rollback();
        return res.status(404).json({
          success: false,
          error: {
            code: 'NOT_FOUND',
            message: `Lab group ${cleanLabGroup} does not exist.`,
            field: 'labGroup'
          }
        });
      }

      const maxCapacity = groupRows[0].max_capacity;
      const [enrolledRows] = await connection.query(
        'SELECT COUNT(*) AS total FROM students WHERE lab_group_id = ? AND is_deleted = FALSE FOR UPDATE',
        [cleanLabGroup]
      );

      if (Number(enrolledRows[0].total) >= maxCapacity) {
        await connection.rollback();
        return res.status(409).json({
          success: false,
          error: {
            code: 'GROUP_FULL',
            message: `Target laboratory group ${cleanLabGroup} has reached its ${maxCapacity}-student capacity.`,
            field: 'labGroup'
          }
        });
      }
    }

    await connection.query(
      `INSERT INTO students (student_number, user_id, full_name, programme_code, lab_group_id, version, is_deleted, created_at, updated_at)
       VALUES (?, NULL, ?, ?, ?, 1, FALSE, NOW(), NOW())`,
      [cleanStudentNumber, cleanName, cleanProgramme, cleanLabGroup]
    );

    await connection.commit();

    return res.status(201).json({
      success: true,
      data: {
        studentNumber: cleanStudentNumber,
        name: cleanName,
        programme: cleanProgramme,
        labGroup: cleanLabGroup || 'Unassigned',
        status: 'ACTIVE',
        createdAt: new Date().toISOString()
      },
      message: 'Student created successfully.'
    });
  } catch (error) {
    await connection.rollback();
    console.error('createStudent error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Failed to create student: ' + error.message
      }
    });
  } finally {
    connection.release();
  }
}

/**
 * PUT /api/lecturer/students/:studentNumber
 */
async function updateStudent(req, res) {
  const { studentNumber } = req.params;
  const { name, programme, labGroup } = req.body;

  const cleanStudentNumber = String(studentNumber).trim();

  const connection = await getConnection();
  try {
    await connection.beginTransaction();

    const [studentRows] = await connection.query(
      'SELECT student_number, full_name, programme_code, lab_group_id, version FROM students WHERE student_number = ? AND is_deleted = FALSE FOR UPDATE',
      [cleanStudentNumber]
    );

    if (studentRows.length === 0) {
      await connection.rollback();
      return res.status(404).json({
        success: false,
        error: {
          code: 'NOT_FOUND',
          message: 'Student record not found.'
        }
      });
    }

    const current = studentRows[0];
    const newName = name ? String(name).trim() : current.full_name;
    const newProgramme = programme ? String(programme).trim().toUpperCase() : current.programme_code;

    let newLabGroup = current.lab_group_id;
    if (labGroup !== undefined) {
      if (labGroup === null || labGroup.toUpperCase() === 'UNASSIGNED') {
        newLabGroup = null;
      } else {
        newLabGroup = String(labGroup).trim().toUpperCase();
      }
    }

    if (newLabGroup && newLabGroup !== current.lab_group_id) {
      const [groupRows] = await connection.query(
        'SELECT id, max_capacity FROM lab_groups WHERE id = ? FOR UPDATE',
        [newLabGroup]
      );

      if (groupRows.length === 0) {
        await connection.rollback();
        return res.status(404).json({
          success: false,
          error: {
            code: 'NOT_FOUND',
            message: `Target lab group ${newLabGroup} does not exist.`
          }
        });
      }

      const maxCapacity = groupRows[0].max_capacity;
      const [countRows] = await connection.query(
        'SELECT COUNT(*) AS total FROM students WHERE lab_group_id = ? AND is_deleted = FALSE FOR UPDATE',
        [newLabGroup]
      );

      if (Number(countRows[0].total) >= maxCapacity) {
        await connection.rollback();
        return res.status(409).json({
          success: false,
          error: {
            code: 'GROUP_FULL',
            message: `Target laboratory group ${newLabGroup} has reached its ${maxCapacity}-student capacity.`
          }
        });
      }
    }

    const newVersion = current.version + 1;
    await connection.query(
      `UPDATE students
       SET full_name = ?, programme_code = ?, lab_group_id = ?, version = ?, updated_at = NOW()
       WHERE student_number = ?`,
      [newName, newProgramme, newLabGroup, newVersion, cleanStudentNumber]
    );

    await connection.commit();

    return res.status(200).json({
      success: true,
      data: {
        studentNumber: cleanStudentNumber,
        name: newName,
        programme: newProgramme,
        labGroup: newLabGroup || 'Unassigned',
        version: newVersion,
        updatedAt: new Date().toISOString()
      },
      message: 'Student updated successfully.'
    });
  } catch (error) {
    await connection.rollback();
    console.error('updateStudent error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Failed to update student: ' + error.message
      }
    });
  } finally {
    connection.release();
  }
}

/**
 * DELETE /api/lecturer/students/:studentNumber
 */
async function deleteStudent(req, res) {
  const { studentNumber } = req.params;
  const cleanStudentNumber = String(studentNumber).trim();

  const connection = await getConnection();
  try {
    await connection.beginTransaction();

    const [studentRows] = await connection.query(
      'SELECT student_number, user_id, version FROM students WHERE student_number = ? AND is_deleted = FALSE FOR UPDATE',
      [cleanStudentNumber]
    );

    if (studentRows.length === 0) {
      await connection.rollback();
      return res.status(404).json({
        success: false,
        error: {
          code: 'NOT_FOUND',
          message: 'Student record not found.'
        }
      });
    }

    const student = studentRows[0];
    const newVersion = student.version + 1;

    await connection.query(
      `UPDATE students
       SET is_deleted = TRUE, lab_group_id = NULL, version = ?, updated_at = NOW()
       WHERE student_number = ?`,
      [newVersion, cleanStudentNumber]
    );

    if (student.user_id) {
      await connection.query(
        'UPDATE users SET is_active = FALSE, updated_at = NOW() WHERE id = ?',
        [student.user_id]
      );
    }

    await connection.commit();

    return res.status(200).json({
      success: true,
      message: `Student record ${cleanStudentNumber} deactivated and removed from active roster.`
    });
  } catch (error) {
    await connection.rollback();
    console.error('deleteStudent error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Failed to delete student: ' + error.message
      }
    });
  } finally {
    connection.release();
  }
}

/**
 * GET /api/lecturer/requests
 */
async function getPendingRequests(req, res) {
  try {
    const [requests] = await query(`
      SELECT r.id, r.student_number, s.full_name AS student_name, s.programme_code,
             r.current_group_id, r.target_group_id, r.reason, r.status, r.created_at
      FROM group_change_requests r
      JOIN students s ON s.student_number = r.student_number
      WHERE r.status = 'PENDING'
      ORDER BY r.created_at ASC
    `);

    const [groupCounts] = await query(`
      SELECT lab_group_id, COUNT(*) AS count
      FROM students
      WHERE is_deleted = FALSE AND lab_group_id IS NOT NULL
      GROUP BY lab_group_id
    `);

    const countMap = {};
    groupCounts.forEach(gc => {
      countMap[gc.lab_group_id] = Number(gc.count);
    });

    const formattedRequests = requests.map(r => ({
      id: r.id,
      studentNumber: r.student_number,
      studentName: r.student_name,
      programme: r.programme_code,
      currentGroup: r.current_group_id,
      currentGroupCount: countMap[r.current_group_id] || 0,
      targetGroup: r.target_group_id,
      targetGroupCount: countMap[r.target_group_id] || 0,
      maxCapacity: 15,
      reason: r.reason,
      status: r.status,
      submittedAt: r.created_at
    }));

    return res.status(200).json({
      success: true,
      data: {
        pendingCount: formattedRequests.length,
        requests: formattedRequests
      }
    });
  } catch (error) {
    console.error('getPendingRequests error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Failed to retrieve transfer requests: ' + error.message
      }
    });
  }
}

/**
 * POST /api/lecturer/requests/:id/approve
 */
async function approveRequest(req, res) {
  const { id } = req.params;
  const staffId = req.user.staffId;

  const connection = await getConnection();
  try {
    await connection.beginTransaction();

    const [reqRows] = await connection.query(
      'SELECT id, student_number, current_group_id, target_group_id, status FROM group_change_requests WHERE id = ? FOR UPDATE',
      [id]
    );

    if (reqRows.length === 0) {
      await connection.rollback();
      return res.status(404).json({
        success: false,
        error: {
          code: 'NOT_FOUND',
          message: `Transfer request ${id} not found.`
        }
      });
    }

    const changeReq = reqRows[0];
    if (changeReq.status !== 'PENDING') {
      await connection.rollback();
      return res.status(400).json({
        success: false,
        error: {
          code: 'VALIDATION_ERROR',
          message: `Request is already ${changeReq.status}.`
        }
      });
    }

    const [targetGroupRows] = await connection.query(
      'SELECT id, max_capacity FROM lab_groups WHERE id = ? FOR UPDATE',
      [changeReq.target_group_id]
    );

    if (targetGroupRows.length === 0) {
      await connection.rollback();
      return res.status(404).json({
        success: false,
        error: {
          code: 'NOT_FOUND',
          message: `Target group ${changeReq.target_group_id} does not exist.`
        }
      });
    }

    const maxCapacity = targetGroupRows[0].max_capacity;

    const [countRows] = await connection.query(
      'SELECT COUNT(*) AS total FROM students WHERE lab_group_id = ? AND is_deleted = FALSE FOR UPDATE',
      [changeReq.target_group_id]
    );

    const targetEnrolled = Number(countRows[0].total);
    if (targetEnrolled >= maxCapacity) {
      await connection.rollback();
      return res.status(409).json({
        success: false,
        error: {
          code: 'GROUP_FULL',
          message: `Target group ${changeReq.target_group_id} has reached maximum capacity (${maxCapacity} students).`
        }
      });
    }

    await connection.query(
      `UPDATE students
       SET lab_group_id = ?, version = version + 1, updated_at = NOW()
       WHERE student_number = ?`,
      [changeReq.target_group_id, changeReq.student_number]
    );

    await connection.query(
      `UPDATE group_change_requests
       SET status = 'APPROVED', reviewed_by_staff_id = ?, reviewed_at = NOW()
       WHERE id = ?`,
      [staffId, id]
    );

    await connection.commit();

    return res.status(200).json({
      success: true,
      message: 'Group change request approved successfully.'
    });
  } catch (error) {
    await connection.rollback();
    console.error('approveRequest error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Failed to approve request: ' + error.message
      }
    });
  } finally {
    connection.release();
  }
}

/**
 * POST /api/lecturer/requests/:id/decline
 */
async function declineRequest(req, res) {
  const { id } = req.params;
  const staffId = req.user.staffId;

  const connection = await getConnection();
  try {
    await connection.beginTransaction();

    const [reqRows] = await connection.query(
      'SELECT id, status FROM group_change_requests WHERE id = ? FOR UPDATE',
      [id]
    );

    if (reqRows.length === 0) {
      await connection.rollback();
      return res.status(404).json({
        success: false,
        error: {
          code: 'NOT_FOUND',
          message: `Transfer request ${id} not found.`
        }
      });
    }

    if (reqRows[0].status !== 'PENDING') {
      await connection.rollback();
      return res.status(400).json({
        success: false,
        error: {
          code: 'VALIDATION_ERROR',
          message: `Request is already ${reqRows[0].status}.`
        }
      });
    }

    await connection.query(
      `UPDATE group_change_requests
       SET status = 'REJECTED', reviewed_by_staff_id = ?, reviewed_at = NOW()
       WHERE id = ?`,
      [staffId, id]
    );

    await connection.commit();

    return res.status(200).json({
      success: true,
      message: 'Group change request declined.'
    });
  } catch (error) {
    await connection.rollback();
    console.error('declineRequest error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Failed to decline request: ' + error.message
      }
    });
  } finally {
    connection.release();
  }
}

module.exports = {
  getDashboard,
  getRoster,
  createStudent,
  updateStudent,
  deleteStudent,
  getPendingRequests,
  approveRequest,
  declineRequest
};
