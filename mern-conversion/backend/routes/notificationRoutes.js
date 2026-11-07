const express = require('express');
const router = express.Router();
const {
  getNotifications,
  getUnseenCount,
  markOneSeen,
  markAllSeen,
  createNotification
} = require('../controllers/notificationController');
const { protect } = require('../middleware/authMiddleware');

router.route('/')
  .get(protect, getNotifications)
  .post(protect, createNotification);

router.route('/count')
  .get(protect, getUnseenCount);

router.route('/mark-all-read')
  .put(protect, markAllSeen);

router.route('/mark-read/:id')
  .put(protect, markOneSeen);

module.exports = router;
