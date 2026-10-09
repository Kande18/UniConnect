const express = require('express');
const router = express.Router();
const { authenticate } = require('../middleware/auth');
const { pullDeltas, pushMutations } = require('../controllers/syncController');

// Sync endpoints require authentication
router.use(authenticate);

router.get('/pull', pullDeltas);
router.post('/push', pushMutations);

module.exports = router;
