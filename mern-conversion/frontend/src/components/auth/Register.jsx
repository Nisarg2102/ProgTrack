import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

const Register = () => {
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    confirmPassword: '',
    department: 'Engineering',
    designation: 'Intern',
    projectGroup: 'Web Platform',
    role: 'INTERN',
    phoneNumber: ''
  });
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();
  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5001';

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setSuccessMsg('');

    if (formData.password !== formData.confirmPassword) {
      setErrorMsg('Passwords do not match');
      return;
    }

    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          fullName: formData.fullName,
          email: formData.email,
          password: formData.password,
          department: formData.department,
          designation: formData.designation,
          projectGroup: formData.projectGroup,
          role: formData.role,
          phoneNumber: formData.phoneNumber
        })
      });

      const data = await res.json();
      if (!res.ok) {
        setErrorMsg(data.message || 'Registration failed');
        return;
      }

      setSuccessMsg('Account registered successfully! Redirecting to login...');
      setTimeout(() => {
        navigate('/login');
      }, 1500);
    } catch (err) {
      setErrorMsg('Network error. Check connection and try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center py-10 bg-gray-50 dark:bg-slate-900 text-slate-800 dark:text-slate-200">
      <div className="container mx-auto px-4 max-w-md">
        
        <div className="flex justify-center mb-6">
          <Link to="/" className="text-3xl font-bold text-blue-600">
            Prog<span className="text-blue-900 dark:text-white">Track</span>
          </Link>
        </div>

        <div className="bg-white dark:bg-slate-800 rounded-xl shadow-lg border border-gray-100 dark:border-slate-700 p-8">
          <div className="text-center mb-6">
            <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Create an Account</h2>
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">Join your organization workspace</p>
          </div>

          {errorMsg && (
            <div className="bg-red-50 border border-red-300 text-red-700 text-sm px-4 py-3 rounded-lg mb-4">
              {errorMsg}
            </div>
          )}

          {successMsg && (
            <div className="bg-green-50 border border-green-300 text-green-700 text-sm px-4 py-3 rounded-lg mb-4">
              {successMsg}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4 text-sm">
            <div>
              <label className="block text-xs font-semibold mb-1 text-gray-700 dark:text-slate-300">Full Name</label>
              <input
                type="text"
                name="fullName"
                required
                value={formData.fullName}
                onChange={handleChange}
                placeholder="John Doe"
                className="w-full rounded-lg border border-gray-300 dark:border-slate-600 dark:bg-slate-900 dark:text-white px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold mb-1 text-gray-700 dark:text-slate-300">Work Email</label>
              <input
                type="email"
                name="email"
                required
                value={formData.email}
                onChange={handleChange}
                placeholder="john@progtrack.com"
                className="w-full rounded-lg border border-gray-300 dark:border-slate-600 dark:bg-slate-900 dark:text-white px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold mb-1 text-gray-700 dark:text-slate-300">Department</label>
                <select
                  name="department"
                  value={formData.department}
                  onChange={handleChange}
                  className="w-full rounded-lg border border-gray-300 dark:border-slate-600 dark:bg-slate-900 dark:text-white px-2 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                >
                  <option value="Engineering">Engineering</option>
                  <option value="Product">Product</option>
                  <option value="Design">Design</option>
                  <option value="Operations">Operations</option>
                  <option value="Management">Management</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold mb-1 text-gray-700 dark:text-slate-300">Role</label>
                <select
                  name="role"
                  value={formData.role}
                  onChange={handleChange}
                  className="w-full rounded-lg border border-gray-300 dark:border-slate-600 dark:bg-slate-900 dark:text-white px-2 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                >
                  <option value="INTERN">Intern</option>
                  <option value="LEAD">Team Lead</option>
                  <option value="CEO">Executive / CEO</option>
                  <option value="ADMIN">Admin</option>
                </select>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold mb-1 text-gray-700 dark:text-slate-300">Password</label>
              <input
                type="password"
                name="password"
                required
                value={formData.password}
                onChange={handleChange}
                placeholder="••••••••"
                className="w-full rounded-lg border border-gray-300 dark:border-slate-600 dark:bg-slate-900 dark:text-white px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold mb-1 text-gray-700 dark:text-slate-300">Confirm Password</label>
              <input
                type="password"
                name="confirmPassword"
                required
                value={formData.confirmPassword}
                onChange={handleChange}
                placeholder="••••••••"
                className="w-full rounded-lg border border-gray-300 dark:border-slate-600 dark:bg-slate-900 dark:text-white px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full mt-4 bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2.5 rounded-lg shadow transition-colors disabled:opacity-50"
            >
              {loading ? 'Creating Account...' : 'Register Account'}
            </button>
          </form>

          <p className="text-center text-xs text-gray-500 dark:text-gray-400 mt-6">
            Already have an account?{' '}
            <Link to="/login" className="text-blue-600 font-semibold hover:underline">
              Sign In
            </Link>
          </p>
        </div>

      </div>
    </div>
  );
};

export default Register;
