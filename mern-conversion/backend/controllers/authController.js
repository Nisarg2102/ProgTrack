const User = require('../models/User');
const jwt = require('jsonwebtoken');

const generateToken = (id) => {
  return jwt.sign({ id }, process.env.JWT_SECRET || 'secret', {
    expiresIn: '30d'
  });
};

const registerUser = async (req, res) => {
  try {
    const { email, password, fullName, designation, department, projectGroup, role, phoneNumber } = req.body;
    
    const userExists = await User.findOne({ email });
    if (userExists) {
      return res.status(400).json({ message: 'User already exists' });
    }

    const user = await User.create({
      email,
      password,
      fullName,
      designation,
      department,
      projectGroup,
      role,
      phoneNumber,
      isApproved: false, // Explicitly require approval
      isActive: true
    });

    if (user) {
      res.status(201).json({
        _id: user._id,
        email: user.email,
        fullName: user.fullName,
        role: user.role,
        token: generateToken(user._id)
      });
    } else {
      res.status(400).json({ message: 'Invalid user data' });
    }
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

const loginUser = async (req, res) => {
  try {
    const { email, password } = req.body;
    
    const user = await User.findOne({ email });
    
    if (user && (await user.matchPassword(password))) {
      if (!user.isActive) {
        return res.status(403).json({ message: 'Account is deactivated' });
      }
      
      user.currentLoginTime = new Date();
      await user.save();
      
      res.json({
        _id: user._id,
        email: user.email,
        fullName: user.fullName,
        role: user.role,
        token: generateToken(user._id)
      });
    } else {
      res.status(401).json({ message: 'Invalid email or password' });
    }
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

const getAllUsers = async (req, res) => {
  try {
    const users = await User.find({ isActive: true })
      .select('fullName email designation department role currentLoginTime')
      .sort({ role: 1, fullName: 1 });
    res.json(users);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

module.exports = {
  registerUser,
  loginUser,
  getAllUsers
};
