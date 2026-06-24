import { useState } from 'react';
import { Link } from 'react-router-dom';

export default function Navbar({ username }) {
  const [open, setOpen] = useState(false);

  return (
      <nav className="flex items-center justify-between px-6 py-3 border-b border-gray-800 bg-black relative">
        <Link to="/dashboard" className="flex items-center gap-2">
          <span className="text-gray-400 text-lg font-bold tracking-wider">codereview</span>
          <span className="text-gray-600 text-xs">v1.0</span>
        </Link>

        <div className="flex items-center gap-6 relative">
          <div className="flex items-center gap-2">
            <div className="w-2 h-2 rounded-full bg-green-400 animate-pulse" />
            <span className="text-xs text-gray-500">user:</span>
            <span className="text-sm text-green-400 font-bold">{username}</span>
          </div>

          <button
              onClick={() => setOpen(!open)}
              className="flex flex-col gap-1 px-2.5 py-2 border border-green-900 hover:border-green-500 hover:bg-green-950 transition"
          >
            <span className={`block w-4 h-0.5 bg-green-400 transition-all ${open ? 'rotate-45 translate-y-1.5' : ''}`} />
            <span className={`block w-4 h-0.5 bg-green-400 transition-all ${open ? 'opacity-0' : ''}`} />
            <span className={`block w-4 h-0.5 bg-green-400 transition-all ${open ? '-rotate-45 -translate-y-1.5' : ''}`} />
          </button>

          {open && (
              <div className="absolute top-full right-0 mt-3 w-52 bg-[#0a0a0a] border border-gray-800 z-50">
                <div className="flex items-center gap-3 px-4 py-3 border-b border-gray-800">
                  <img
                      src={`https://github.com/${username}.png?size=40`}
                      alt={username}
                      className="w-9 h-9 rounded-full border border-green-900"
                  />
                  <div className="flex flex-col">
                    <span className="text-white text-sm font-bold">{username}</span>
                    <span className="text-gray-600 text-xs">github account</span>
                  </div>
                </div>

                <p className="px-4 pt-2 pb-1 text-gray-700 text-xs uppercase tracking-widest">navigate</p>
                <Link
                    to="/profile"
                    onClick={() => setOpen(false)}
                    className="flex items-center gap-3 px-4 py-2.5 text-gray-400 text-xs hover:bg-[#0d0d0d] hover:text-white transition border-b border-[#111]"
                >
                  <span className="text-green-400">◈</span> Profile
                </Link>
                <Link
                    to="/review"
                    onClick={() => setOpen(false)}
                    className="flex items-center gap-3 px-4 py-2.5 text-gray-400 text-xs hover:bg-[#0d0d0d] hover:text-white transition border-b border-[#111]"
                >
                  <span className="text-green-400">◈</span> Review
                </Link>

                <p className="px-4 pt-2 pb-1 text-gray-700 text-xs uppercase tracking-widest">system</p>
                <Link
                    to="/settings"
                    onClick={() => setOpen(false)}
                    className="flex items-center gap-3 px-4 py-2.5 text-gray-400 text-xs hover:bg-[#0d0d0d] hover:text-white transition"
                >
                  <span className="text-green-400">◈</span> Settings
                </Link>
              </div>
          )}
        </div>

        {open && (
            <div className="fixed inset-0 z-40" onClick={() => setOpen(false)} />
        )}
      </nav>
  );
}