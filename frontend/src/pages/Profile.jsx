import Navbar from '../components/Navbar';
import { useAuth } from '../hooks/useAuth';

export default function Profile() {
    const { username } = useAuth();

    return (
        <div className="min-h-screen bg-black">
            <Navbar username={username} />
            <main className="max-w-3xl mx-auto px-6 py-8 space-y-8">
                <div className="flex items-center gap-2">
                    <span className="text-green-400 font-semibold">~/codereview $</span>
                    <span className="text-white font-semibold">profile --show</span>
                </div>
                <div className="h-px bg-gradient-to-r from-gray-600 via-gray-500 to-transparent" />
                <div className="border border-gray-800 bg-[#0a0a0a]">
                    <div className="px-4 py-3 border-b border-gray-800 flex items-center gap-2">
                        <span className="text-green-400 text-xs">◈</span>
                        <span className="text-white text-sm font-bold">GitHub Account</span>
                    </div>
                    <div className="p-6 flex items-center gap-6">
                        <img
                            src={`https://github.com/${username}.png?size=80`}
                            alt={username}
                            className="w-20 h-20 rounded-full border border-green-900"
                        />
                        <div className="space-y-2">
                            <p className="text-white text-xl font-bold">{username}</p>
                            <p className="text-green-400 text-xs">github.com/{username}</p>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    );
}