const { query } = require('../config/db');

/**
 * GET /api/groups
 */
async function getAllGroups(req, res) {
  try {
    const [groups] = await query(`
      SELECT g.id, g.name, g.max_capacity, g.day_of_week, g.start_time, g.end_time,
             g.venue, g.instructor_name,
             COUNT(s.student_number) AS enrolled_count
      FROM lab_groups g
      LEFT JOIN students s ON s.lab_group_id = g.id AND s.is_deleted = FALSE
      GROUP BY g.id, g.name, g.max_capacity, g.day_of_week, g.start_time, g.end_time, g.venue, g.instructor_name
      ORDER BY g.id ASC
    `);

    const formattedGroups = groups.map(g => {
      const enrolled = Number(g.enrolled_count);
      const capacity = Number(g.max_capacity);
      const available = Math.max(0, capacity - enrolled);

      let status = 'AVAILABLE';
      if (enrolled >= capacity) {
        status = 'FULL';
      } else if (enrolled >= 12) {
        status = 'ALMOST_FULL';
      }

      const startTimeStr = g.start_time ? String(g.start_time).substring(0, 5) : '';
      const endTimeStr = g.end_time ? String(g.end_time).substring(0, 5) : '';

      return {
        id: g.id,
        name: g.name,
        capacity,
        enrolled,
        availableSeats: available,
        status,
        dayOfWeek: g.day_of_week,
        schedule: `${g.day_of_week} ${startTimeStr} – ${endTimeStr}`,
        venue: g.venue,
        instructor: g.instructor_name
      };
    });

    return res.status(200).json({
      success: true,
      data: formattedGroups
    });
  } catch (error) {
    console.error('getAllGroups error:', error);
    return res.status(500).json({
      success: false,
      error: {
        code: 'SERVER_ERROR',
        message: 'Failed to retrieve groups: ' + error.message
      }
    });
  }
}

module.exports = {
  getAllGroups
};
