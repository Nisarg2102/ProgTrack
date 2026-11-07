const cron = require('node-cron');
const nodemailer = require('nodemailer');
const User = require('../models/User');

const transporter = nodemailer.createTransport({
  host: process.env.MAIL_HOST || 'smtp.mailtrap.io',
  port: process.env.MAIL_PORT || 2525,
  auth: {
    user: process.env.MAIL_USER || 'user',
    pass: process.env.MAIL_PASS || 'pass'
  }
});

const sendEmail = async (to, subject, text) => {
  try {
    await transporter.sendMail({
      from: '"ProgTrack" <noreply@3spacetechnologies.com>',
      to,
      subject,
      text
    });
  } catch (error) {
    console.error('Error sending email:', error);
  }
};

const sendApprovalEmail = (toEmail, fullName) => {
  sendEmail(
    toEmail,
    'ProgTrack: Account Approved!',
    `Hello ${fullName},\n\nGreat news! Your ProgTrack account has been approved by the Administrator.\nYou can now log in using the credentials you created during registration.\n\nLogin here: http://localhost:8080/login\n\nWelcome to the team!\n- 3Space Technologies`
  );
};

const sendRejectionEmail = (toEmail, fullName) => {
  sendEmail(
    toEmail,
    'ProgTrack: Account Registration Update',
    `Hello ${fullName},\n\nWe are writing to inform you that your request for a ProgTrack account has been declined at this time.\nIf you believe this is an error, please contact your systems administrator.\n\n- 3Space Technologies`
  );
};

const initJobs = () => {
  // Weekly reminder: every hour from 9 to 17 on Thursday (day 4)
  cron.schedule('0 9-17 * * 4', async () => {
    try {
      const interns = await User.find({ role: 'INTERN', isActive: true });
      for (const intern of interns) {
        // Simple demonstration. In production, ensure no duplicate emails on the same day.
        await sendEmail(
          intern.email,
          'ProgTrack: Weekly Report Reminder',
          '⏳ Reminder: Your Weekly Report is due tomorrow (Friday) by 5:30 PM.'
        );
      }
      console.log('✅ Sent weekly reminders to interns.');
    } catch (error) {
      console.error('Error in weekly reminder job:', error);
    }
  });

  // Monthly reminder: every hour from 9 to 17 every day
  cron.schedule('0 9-17 * * *', async () => {
    try {
      const today = new Date();
      const tomorrow = new Date(today);
      tomorrow.setDate(tomorrow.getDate() + 1);

      // Check if tomorrow is the last day of the month
      const lastDayOfMonth = new Date(tomorrow.getFullYear(), tomorrow.getMonth() + 1, 0).getDate();
      
      if (tomorrow.getDate() === lastDayOfMonth) {
        const interns = await User.find({ role: 'INTERN', isActive: true });
        for (const intern of interns) {
          await sendEmail(
            intern.email,
            'ProgTrack: Monthly Report Reminder',
            '⏳ Reminder: Your Monthly Report is due tomorrow (Last day of the month) by 5:30 PM.'
          );
        }
        console.log('✅ Sent monthly reminders to interns.');
      }
    } catch (error) {
      console.error('Error in monthly reminder job:', error);
    }
  });
};

module.exports = { initJobs, sendEmail, sendApprovalEmail, sendRejectionEmail };
