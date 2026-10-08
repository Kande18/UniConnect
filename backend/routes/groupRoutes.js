const express = require('express');
const router = express.Router();
const { authenticate } = require('../middleware/auth');
const { getAllGroups } = require('../controllers/groupController');

router.get('/', authenticate, getAllGroups);

module.exports = router;
