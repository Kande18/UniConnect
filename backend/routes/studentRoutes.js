const express = require('express');
const router = express.Router();
const { authenticate, requireRole } = require('../middleware/auth');
const {
  getProfile,
  updateProfile,
  getMyGroup
} = require('../controllers/studentController');

router.use(authenticate);
router.use(requireRole('STUDENT'));

router.get('/profile', getProfile);
router.put('/profile', updateProfile);
router.get('/my-group', getMyGroup);

module.exports = router;
