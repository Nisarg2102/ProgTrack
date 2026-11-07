const mongoose = require('mongoose');
const dotenv = require('dotenv');
const User = require('./models/User');

dotenv.config();

const MONGO_URI = process.env.MONGO_URI || 'mongodb://localhost:27017/progtrack_local';

const defaultUsers = [
  {
    email: 'admin@progtrack.com',
    password: 'Admin@123',
    fullName: 'Admin User',
    designation: 'System Administrator',
    department: 'IT',
    projectGroup: 'Infrastructure',
    role: 'ADMIN',
    isActive: true,
    isApproved: true,
    phoneNumber: '+1234567890'
  },
  {
    email: 'ceo@progtrack.com',
    password: 'Ceo@123',
    fullName: 'Chief Executive Officer',
    designation: 'CEO',
    department: 'Management',
    projectGroup: 'Executive',
    role: 'CEO',
    isActive: true,
    isApproved: true,
    phoneNumber: '+1234567891'
  },
  {
    email: 'lead@progtrack.com',
    password: 'Lead@123',
    fullName: 'Team Lead',
    designation: 'Engineering Lead',
    department: 'Engineering',
    projectGroup: 'Frontend & Backend',
    role: 'LEAD',
    isActive: true,
    isApproved: true,
    phoneNumber: '+1234567892'
  },
  {
    email: 'intern@progtrack.com',
    password: 'Intern@123',
    fullName: 'Junior Intern',
    designation: 'Software Engineering Intern',
    department: 'Engineering',
    projectGroup: 'Frontend & Backend',
    role: 'INTERN',
    isActive: true,
    isApproved: true,
    phoneNumber: '+1234567893'
  }
];

const seedDB = async () => {
  try {
    await mongoose.connect(MONGO_URI);
    console.log('Connected to MongoDB for seeding...');

    for (const userData of defaultUsers) {
      const existing = await User.findOne({ email: userData.email });
      if (existing) {
        console.log(`User already exists: ${userData.email}`);
      } else {
        await User.create(userData);
        console.log(`Created user [${userData.role}]: ${userData.email}`);
      }
    }

    console.log('\n--- Default Seed Credentials ---');
    defaultUsers.forEach(u => {
      console.log(`Role: ${u.role.padEnd(7)} | Email: ${u.email.padEnd(22)} | Password: ${u.password}`);
    });
    console.log('--------------------------------\n');

    process.exit(0);
  } catch (error) {
    console.error('Seeding error:', error);
    process.exit(1);
  }
};

seedDB();
