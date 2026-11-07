import React from 'react';

const Footer = () => {
  return (
    <footer className="p-4 bg-gray-50 dark:bg-slate-900 border-t border-gray-200 dark:border-slate-700 text-center text-sm text-gray-500 dark:text-gray-400 mt-auto transition-colors duration-300">
      <div className="mb-1">
        &copy; {new Date().getFullYear()} <strong className="text-gray-700 dark:text-slate-300">3Space Technologies</strong>. All Rights Reserved. ProgTrack v1.0
      </div>
      <div>
        Developed by <span className="text-blue-600 dark:text-blue-400">Bhavik & Sarthak</span>
      </div>
    </footer>
  );
};
export default Footer;
