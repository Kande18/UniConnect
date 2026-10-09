const express = require('express');
const router = express.Router();
const { authenticate, requireRole } = require('../middleware/auth');
const {
  getDashboard,
  getRoster,
  createStudent,
  updateStudent,
  deleteStudent,
  getPendingRequests,
  approveRequest,
  declineRequest
} = require('../controllers/lecturerController');

router.use(authenticate);
router.use(requireRole('LECTURER'));

router.get('/dashboard', getDashboard);
router.get('/roster', getRoster);
router.post('/students', createStudent);
router.put('/students/:studentNumber', updateStudent);
router.delete('/students/:studentNumber', deleteStudent);
router.get('/requests', getPendingRequests);
router.post('/requests/:id/approve', approveRequest);
router.post('/requests/:id/decline', declineRequest);

module.exports = router;
