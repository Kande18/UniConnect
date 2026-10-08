const express = require('express');
const router = express.Router();
const { authenticate, requireRole } = require('../middleware/auth');
const { submitChangeRequest } = require('../controllers/studentController');

router.post('/change-group', authenticate, requireRole('STUDENT'), submitChangeRequest);

module.exports = router;
