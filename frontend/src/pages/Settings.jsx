import { useEffect, useState } from 'react';
import Navbar from '../components/Navbar';
import SnakeBackground from '../components/SnakeBackground';
import { useAuth } from '../hooks/useAuth';
import { useTheme } from '../hooks/useTheme';
import { getConnectedRepos } from '../services/api';
import { logout } from '../services/auth';

export default function Settings() {
    const { username } = useAuth();
    const { theme, setTheme } = useTheme();
    const [connectedRepos, setConnectedRepos] = useState([]);

    useEffect(() => {
        getConnectedRepos().then(setConnectedRepos).catch(() => {});
    }, []);

    return (
        <div className="min-h-screen bg-black relative">
            <SnakeBackground />
            <div className="relative z-10">
                <Navbar username={username} />
                <main className="max-w-3xl mx-auto px-6 py-8 space-y-8">

                    <div className="flex items-center gap-2">
                        <span className="text-green-400 font-semibold">~/codereview $</span>
                        <span className="text-white font-semibold">settings --open</span>
                    </div>
                    <div className="h-px bg-gradient-to-r from-gray-600 via-gray-500 to-transparent" />

                    <div className="border border-gray-800 bg-black/60 backdrop-blur-sm">
                        <div className="px-4 py-3 border-b border-gray-800 flex items-center gap-2">
                            <span className="text-green-400 text-xs">◈</span>
                            <span className="text-white text-sm font-bold">Connected Repositories</span>
                        </div>
                        <div className="p-4 space-y-2">
                            {connectedRepos.length === 0 ? (
                                <p className="text-gray-600 text-xs">No repositories connected.</p>
                            ) : (
                                connectedRepos.map((repo) => (
                                    <div key={repo} className="flex items-center justify-between px-3 py-2 border border-gray-800 bg-black">
                                        <span className="text-gray-300 text-xs">{repo}</span>
                                        <button className="text-red-500 text-xs hover:text-red-400 border border-red-900 hover:border-red-500 px-2 py-0.5 transition">
                                            disconnect
                                        </button>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>

                    <div className="border border-gray-800 bg-black/60 backdrop-blur-sm">
                        <div className="px-4 py-3 border-b border-gray-800 flex items-center gap-2">
                            <span className="text-green-400 text-xs">◈</span>
                            <span className="text-white text-sm font-bold">Theme</span>
                        </div>
                        <div className="p-4 flex gap-3">
                            {['dark', 'light'].map((t) => (
                                <button
                                    key={t}
                                    onClick={() => setTheme(t)}
                                    className={`px-4 py-2 text-xs font-bold uppercase border transition ${
                                        theme === t
                                            ? 'border-green-500 text-green-400 bg-green-950'
                                            : 'border-gray-700 text-gray-500 hover:border-gray-500'
                                    }`}
                                >
                                    {t}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className="border border-gray-800 bg-black/60 backdrop-blur-sm">
                        <div className="px-4 py-3 border-b border-gray-800 flex items-center gap-2">
                            <span className="text-green-400 text-xs">◈</span>
                            <span className="text-white text-sm font-bold">About</span>
                        </div>
                        <div className="p-4 space-y-2 text-xs text-gray-500">
                            <p>codereview <span className="text-green-400">v1.0</span></p>
                            <p>AI-powered code review triggered by GitHub push events.</p>
                        </div>
                    </div>

                    <div className="border border-red-950 bg-black/60 backdrop-blur-sm">
                        <div className="px-4 py-3 border-b border-red-950 flex items-center gap-2">
                            <span className="text-red-500 text-xs">◈</span>
                            <span className="text-white text-sm font-bold">Danger Zone</span>
                        </div>
                        <div className="p-4">
                            <button
                                onClick={logout}
                                className="px-4 py-2 text-xs font-bold uppercase border border-red-900 text-red-500 hover:bg-red-950 hover:border-red-500 transition"
                            >
                                [ LOGOUT ]
                            </button>
                        </div>
                    </div>

                </main>
            </div>
        </div>
    );
}