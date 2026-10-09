const { pool, query, getConnection } = require('../config/db');

/**
 * GET /api/sync/pull?since={timestamp}
 * Returns all student records updated or soft-deleted since given timestamp, plus live group counts.
 */
async function pullDeltas(req, res) {
  const { since } = req.query;

  try {
    let studentSql = 'SELECT student_number, full_name, programme_code, lab_group_id, version, is_deleted, updated_at FROM students';
    let params = [];

    if (since) {
      studentSql += ' WHERE updated_at >= ?';
      params.push(new Date(since).toISOString().slice(0, 19).replace('T', ' '));
    }

    const [rows] = await query(studentSql, params);

    const updatedStudents = [];
    const deletedStudentNumbers = [];

    rows.forEach(s => {
      if (s.is_deleted) {
        deletedStudentNumbers.push(s.student_number);
      } else {
        updatedStudents.push({
          studentNumber: s.student_number,
          name: s.full_name,
          programme: s.programme_code,
          labGroup: s.lab_group_id || 'Unassigned',
          version: s.version,
          updatedAt: s.updated_at
        });
      }
    });

    // Group counts
    const [groupCountRows] = await query(`
      SELECT g.id, COUNT(s.student_number) AS total
      FROM lab_groups g
      LEFT JOIN students s ON s.lab_group_id = g.id AND s.is_deleted = FALSE
      GROUP BY g.id
    `);

    const groupCounts = {};
    groupCountRows.forEach(gc => {
      groupCounts[gc.id] = Number(gc.total);
    });

    return res.status(200).json({
      success: true,
      data: {
        serverTimestamp: new Date().toISOString(),
        updatedStudents,
        deletedStudentNumbers,
        groupCounts
      }
    });
  } catch (error) {
    console.error('pullDeltas error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Sync pull failed: ' + error.message
      }
    });
  }
}

/**
 * POST /api/sync/push
 * Processes a batch of offline client mutations with optimistic version conflict detection.
 */
async function pushMutations(req, res) {
  const { mutations } = req.body;

  if (!Array.isArray(mutations)) {
    return res.status(400).json({
      success: false,
      error: {
        code: 'VALIDATION_ERROR',
        message: 'Mutations array is required.'
      }
    });
  }

  const applied = [];
  const conflicts = [];

  const connection = await getConnection();
  try {
    await connection.beginTransaction();

    for (const mut of mutations) {
      const { mutationId, entity, entityId, action, clientVersion, payload } = mut;

      if (entity === 'STUDENT') {
        const [studentRows] = await connection.query(
          'SELECT student_number, full_name, programme_code, lab_group_id, version, is_deleted FROM students WHERE student_number = ? FOR UPDATE',
          [entityId]
        );

        if (studentRows.length === 0) {
          conflicts.push({
            mutationId,
            entityId,
            reason: 'NOT_FOUND',
            message: 'Student record does not exist on server.'
          });
          continue;
        }

        const serverStudent = studentRows[0];

        // Optimistic concurrency check
        if (serverStudent.version > clientVersion) {
          conflicts.push({
            mutationId,
            entityId,
            reason: 'SYNC_CONFLICT',
            serverVersion: serverStudent.version,
            clientVersion,
            serverState: {
              name: serverStudent.full_name,
              programme: serverStudent.programme_code,
              labGroup: serverStudent.lab_group_id
            }
          });
          continue;
        }

        // Apply mutation
        if (action === 'UPDATE' && payload) {
          const newName = payload.name || serverStudent.full_name;
          const newProgramme = payload.programme || serverStudent.programme_code;
          const newGroup = payload.labGroup !== undefined ? payload.labGroup : serverStudent.lab_group_id;
          const newVersion = serverStudent.version + 1;

          await connection.query(
            `UPDATE students
             SET full_name = ?, programme_code = ?, lab_group_id = ?, version = ?, updated_at = NOW()
             WHERE student_number = ?`,
            [newName, newProgramme, newGroup, newVersion, entityId]
          );

          applied.push(mutationId);
        } else if (action === 'DELETE') {
          const newVersion = serverStudent.version + 1;
          await connection.query(
            `UPDATE students
             SET is_deleted = TRUE, lab_group_id = NULL, version = ?, updated_at = NOW()
             WHERE student_number = ?`,
            [newVersion, entityId]
          );

          applied.push(mutationId);
        }
      }
    }

    await connection.commit();

    return res.status(200).json({
      success: true,
      data: {
        applied,
        conflicts
      }
    });
  } catch (error) {
    await connection.rollback();
    console.error('pushMutations error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Sync push failed: ' + error.message
      }
    });
  } finally {
    connection.release();
  }
}

module.exports = {
  pullDeltas,
  pushMutations
};
