const express = require('express');
const router = express.Router();
const {
  registerStudent,
  studentLogin,
  lecturerLogin
} = require('../controllers/authController');

router.post('/register', registerStudent);
router.post('/student-login', studentLogin);
router.post('/lecturer-login', lecturerLogin);

module.exports = router;
