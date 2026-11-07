const express = require('express');
const router = express.Router();
const {
  getProductivityAnalytics,
  getAttendanceAnalytics
} = require('../controllers/analyticsController');
const { protect, authorize } = require('../middleware/authMiddleware');

router.get('/productivity', protect, authorize('LEAD', 'CEO', 'ADMIN'), getProductivityAnalytics);
router.get('/attendance', protect, authorize('CEO', 'ADMIN'), getAttendanceAnalytics);

module.exports = router;
