const express = require('express');
const router = express.Router();
const { getTodayTasks, submitDailyGrid, getTaskHistory, getTeamTasks, deleteTask } = require('../controllers/taskController');
const { protect, authorize } = require('../middleware/authMiddleware');

router.route('/today').get(protect, getTodayTasks);
router.route('/submit-daily').post(protect, submitDailyGrid);
router.route('/history').get(protect, getTaskHistory);
router.route('/team').get(protect, authorize('LEAD', 'CEO', 'ADMIN'), getTeamTasks);
router.route('/:id').delete(protect, deleteTask);

module.exports = router;
