const express = require('express');
const router = express.Router();
const { getMyWorkLog, getActiveSession, startSession, endSession } = require('../controllers/workLogController');
const { protect } = require('../middleware/authMiddleware');

router.route('/').get(protect, getMyWorkLog);
router.route('/active').get(protect, getActiveSession);
router.route('/start').post(protect, startSession);
router.route('/end').post(protect, endSession);

module.exports = router;
