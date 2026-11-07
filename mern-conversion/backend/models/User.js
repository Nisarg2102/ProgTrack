const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const userSchema = new mongoose.Schema({
  email: {
    type: String,
    required: [true, 'Email is required'],
    unique: true,
    maxlength: 150,
    match: [/.+\@.+\..+/, 'Please provide a valid email address']
  },
  password: {
    type: String,
    required: [true, 'Password is required']
  },
  fullName: {
    type: String,
    required: [true, 'Full name is required'],
    maxlength: 100
  },
  designation: {
    type: String,
    maxlength: 100
  },
  department: {
    type: String,
    maxlength: 100
  },
  projectGroup: {
    type: String,
    maxlength: 100
  },
  role: {
    type: String,
    required: true,
    enum: ['ADMIN', 'CEO', 'LEAD', 'INTERN'],
    maxlength: 20
  },
  isActive: {
    type: Boolean,
    default: true
  },
  isApproved: {
    type: Boolean,
    default: false
  },
  currentLoginTime: {
    type: Date
  },
  profilePicture: {
    type: String,
    maxlength: 255
  },
  phoneNumber: {
    type: String,
    maxlength: 20
  }
}, {
  timestamps: true
});

userSchema.pre('save', async function() {
  if (!this.isModified('password')) {
    return;
  }
  const salt = await bcrypt.genSalt(10);
  this.password = await bcrypt.hash(this.password, salt);
});

userSchema.methods.matchPassword = async function(enteredPassword) {
  return await bcrypt.compare(enteredPassword, this.password);
};

const User = mongoose.model('User', userSchema);
module.exports = User;
