import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

const Login = () => {
  const [theme, setTheme] = useState(localStorage.getItem('theme') || 'light');
  const [showPassword, setShowPassword] = useState(false);
  const [formData, setFormData] = useState({ email: '', password: '', rememberMe: false });
  const [errorMsg, setErrorMsg] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000';

  useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme(theme === 'dark' ? 'light' : 'dark');
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');
    setIsLoading(true);
    try {
      const response = await fetch(`${API_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: formData.email, password: formData.password })
      });
      const data = await response.json();

      if (!response.ok) {
        setErrorMsg(data.message || 'Login failed');
        return;
      }

      // Store token and user info
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify(data));
      if (formData.rememberMe) {
        localStorage.setItem('rememberMe', 'true');
      }
      // Redirect to dashboard
      window.location.href = '/';
    } catch (error) {
      setErrorMsg('Network error. Please check your connection and try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center py-8 bg-gray-50 dark:bg-slate-900 text-slate-800 dark:text-slate-200 transition-colors duration-300">
      <div className="container mx-auto px-4">
        <div className="flex justify-center">
          <div className="w-full md:w-1/2 lg:w-1/3 flex flex-col items-center justify-center">
            
            <div className="flex justify-center py-4">
              <Link to="/" className="flex items-center w-auto no-underline">
                <span className="font-bold text-3xl text-blue-600">
                  Prog<span className="text-blue-900 dark:text-slate-200">Track</span>
                </span>
              </Link>
            </div>

            <div className="bg-white dark:bg-slate-800 w-full rounded-xl shadow-lg border border-gray-100 dark:border-slate-700 mb-6">
              <div className="p-6 sm:p-8">
                <div className="pt-2 pb-4">
                  <h5 className="text-center font-bold text-2xl mb-1 text-gray-900 dark:text-white">Sign In to Your Account</h5>
                  <p className="text-center text-sm text-gray-500 dark:text-slate-400">Enter your credentials to continue</p>
                </div>

                {errorMsg && (
                  <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded relative mb-4" role="alert">
                    <span className="block sm:inline">{errorMsg}</span>
                    <button onClick={() => setErrorMsg('')} className="absolute top-0 bottom-0 right-0 px-4 py-3">
                      <span className="sr-only">Close</span>
                      <svg className="fill-current h-6 w-6 text-red-500" role="button" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><title>Close</title><path d="M14.348 14.849a1.2 1.2 0 0 1-1.697 0L10 11.819l-2.651 3.029a1.2 1.2 0 1 1-1.697-1.697l2.758-3.15-2.759-3.152a1.2 1.2 0 1 1 1.697-1.697L10 8.183l2.651-3.031a1.2 1.2 0 1 1 1.697 1.697l-2.758 3.152 2.758 3.15a1.2 1.2 0 0 1 0 1.698z"/></svg>
                    </button>
                  </div>
                )}

                <form className="space-y-5" onSubmit={handleSubmit}>
                  <div>
                    <label htmlFor="email" className="block text-sm font-semibold mb-1 text-gray-700 dark:text-slate-300">Email Address</label>
                    <div className="flex">
                      <span className="inline-flex items-center px-3 rounded-l-md border border-r-0 border-gray-300 dark:border-slate-600 bg-gray-50 dark:bg-slate-700 text-gray-500 dark:text-slate-400">
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                          <path d="M2.003 5.884L10 9.882l7.997-3.998A2 2 0 0016 4H4a2 2 0 00-1.997 1.884z" />
                          <path d="M18 8.118l-8 4-8-4V14a2 2 0 002 2h12a2 2 0 002-2V8.118z" />
                        </svg>
                      </span>
                      <input 
                        type="email" 
                        name="email" 
                        id="email" 
                        placeholder="your@email.com" 
                        required 
                        className="flex-1 block w-full rounded-none rounded-r-md sm:text-sm border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-900 text-gray-900 dark:text-white focus:ring-blue-500 focus:border-blue-500 px-3 py-2 border"
                        value={formData.email}
                        onChange={handleInputChange}
                      />
                    </div>
                  </div>

                  <div>
                    <label htmlFor="password" className="block text-sm font-semibold mb-1 text-gray-700 dark:text-slate-300">Password</label>
                    <div className="flex relative">
                      <span className="inline-flex items-center px-3 rounded-l-md border border-r-0 border-gray-300 dark:border-slate-600 bg-gray-50 dark:bg-slate-700 text-gray-500 dark:text-slate-400">
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                          <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
                        </svg>
                      </span>
                      <input 
                        type={showPassword ? 'text' : 'password'} 
                        name="password" 
                        id="password" 
                        placeholder="••••••••" 
                        required 
                        className="flex-1 block w-full rounded-none sm:text-sm border-gray-300 dark:border-slate-600 bg-white dark:bg-slate-900 text-gray-900 dark:text-white focus:ring-blue-500 focus:border-blue-500 px-3 py-2 border"
                        value={formData.password}
                        onChange={handleInputChange}
                      />
                      <button 
                        type="button" 
                        className="inline-flex items-center px-3 rounded-r-md border border-l-0 border-gray-300 dark:border-slate-600 bg-gray-50 dark:bg-slate-700 text-gray-500 dark:text-slate-400 hover:bg-gray-100 dark:hover:bg-slate-600"
                        onClick={() => setShowPassword(!showPassword)}
                      >
                        {showPassword ? (
                          <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                            <path fillRule="evenodd" d="M3.707 2.293a1 1 0 00-1.414 1.414l14 14a1 1 0 001.414-1.414l-1.473-1.473A10.014 10.014 0 0019.542 10C18.268 5.943 14.478 3 10 3a9.958 9.958 0 00-4.512 1.074l-1.78-1.781zm4.261 4.26l1.514 1.515a2.003 2.003 0 012.45 2.45l1.514 1.514a4 4 0 00-5.478-5.478z" clipRule="evenodd" />
                            <path d="M12.454 16.697L9.75 13.992a4 4 0 01-3.742-3.741L2.335 6.578A9.98 9.98 0 00.458 10c1.274 4.057 5.065 7 9.542 7 .847 0 1.669-.105 2.454-.303z" />
                          </svg>
                        ) : (
                          <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                            <path d="M10 12a2 2 0 100-4 2 2 0 000 4z" />
                            <path fillRule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clipRule="evenodd" />
                          </svg>
                        )}
                      </button>
                    </div>
                  </div>

                  <div className="flex items-center">
                    <input 
                      id="rememberMe" 
                      name="rememberMe" 
                      type="checkbox" 
                      className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                      checked={formData.rememberMe}
                      onChange={handleInputChange}
                    />
                    <label htmlFor="rememberMe" className="ml-2 block text-sm text-gray-700 dark:text-slate-300">
                      Keep me signed in
                    </label>
                  </div>

                  <div>
                    <button 
                      type="submit" 
                      className="w-full flex justify-center py-2.5 px-4 border border-transparent rounded-md shadow-sm text-sm font-semibold text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2" viewBox="0 0 20 20" fill="currentColor">
                        <path fillRule="evenodd" d="M3 3a1 1 0 00-1 1v12a1 1 0 102 0V4a1 1 0 00-1-1zm10.293 9.293a1 1 0 001.414 1.414l3-3a1 1 0 000-1.414l-3-3a1 1 0 10-1.414 1.414L14.586 9H7a1 1 0 100 2h7.586l-1.293 1.293z" clipRule="evenodd" />
                      </svg>
                      Sign In
                    </button>
                  </div>

                  <div className="text-center text-sm">
                    <p className="text-gray-500 dark:text-slate-400">
                      Don't have an account?{' '}
                      <Link to="/register" className="font-medium text-blue-600 hover:text-blue-500 dark:text-blue-400 dark:hover:text-blue-300">
                        Register here
                      </Link>
                    </p>
                  </div>
                </form>

              </div>
            </div>

            <div className="text-center text-xs text-gray-500 dark:text-slate-400">
              Powered by <strong className="text-gray-700 dark:text-slate-300">3Space Technologies</strong> — ProgTrack v1.0
            </div>

          </div>
        </div>
      </div>

      <button 
        className="fixed bottom-6 right-6 w-12 h-12 rounded-full flex items-center justify-center shadow-lg transition-transform hover:scale-110 z-50 bg-white dark:bg-slate-700 border border-gray-200 dark:border-slate-600 text-blue-600 dark:text-amber-500"
        onClick={toggleTheme}
        title="Toggle Dark/Light Mode"
      >
        {theme === 'dark' ? (
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
        ) : (
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
          </svg>
        )}
      </button>
    </div>
  );
};

export default Login;
