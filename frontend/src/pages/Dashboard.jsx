import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Navbar from '../components/Navbar';
import RepoCard from '../components/RepoCard';
import SnakeBackground from '../components/SnakeBackground';
import { useAuth } from '../hooks/useAuth';
import { getRepos, connectRepo, getConnectedRepos, getLatestReview } from '../services/api';

export default function Dashboard() {
    const { username, loading } = useAuth();
    const [repos, setRepos] = useState([]);
    const [connected, setConnected] = useState(new Set());
    const [scores, setScores] = useState({}); // { "owner/repo": score|null }
    const [reposLoading, setReposLoading] = useState(true);
    const navigate = useNavigate();

    const sortedRepos = [...repos].sort((a, b) => {
        const aConnected = connected.has(a.name) ? 0 : 1;
        const bConnected = connected.has(b.name) ? 0 : 1;
        return aConnected - bConnected;
    });

    useEffect(() => {
        if (!loading && !username) {
            navigate('/', { replace: true });
        }
    }, [username, loading, navigate]);

    useEffect(() => {
        if (!username) return;
        Promise.all([getRepos(), getConnectedRepos()])
            .then(([repoData, connectedData]) => {
                setRepos(Array.isArray(repoData) ? repoData : []);
                setConnected(new Set(connectedData));
            })
            .catch(() => setRepos([]))
            .finally(() => setReposLoading(false));
    }, [username]);

    // Once we know which repos are connected, fetch each one's latest score.
    // connectedData from the API is just repo names ("test-repo"), but reviews
    // are keyed by "owner/repo" — so we build the full name from repo.owner.login.
    useEffect(() => {
        if (connected.size === 0 || repos.length === 0) return;

        const connectedRepoObjects = repos.filter((r) => connected.has(r.name));

        connectedRepoObjects.forEach((repo) => {
            const fullName = `${repo.owner?.login ?? username}/${repo.name}`;
            getLatestReview(fullName)
                .then((review) => {
                    const score = review?.payload?.results?.[0]?.quality_score ?? null;
                    setScores((prev) => ({ ...prev, [repo.name]: score }));
                })
                .catch(() => {
                    setScores((prev) => ({ ...prev, [repo.name]: null }));
                });
        });
    }, [connected, repos, username]);

    const handleConnect = async (repo) => {
        if (connected.has(repo.name)) return;
        try {
            await connectRepo(repo.owner.login, repo.name);
            setConnected((prev) => new Set([...prev, repo.name]));
        } catch (e) {
            console.error('Failed to connect repo', e);
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-black flex items-center justify-center text-gray-400 text-lg">
                <span className="text-green-400">~/codereview $</span>
                <span className="ml-3 text-gray-300">initializing</span>
                <span className="cursor-blink ml-1 text-green-400">█</span>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-black relative">
            <SnakeBackground />
            <div className="relative z-10">
                <Navbar username={username} />
                <main className="max-w-5xl mx-auto px-6 py-8">
                    <div className="space-y-4 mb-10">
                        <div className="flex items-start gap-2">
                            <span className="text-green-400 text-base font-semibold shrink-0 mt-0.5">~/codereview $</span>
                            <span className="text-white text-base font-semibold">list --repos</span>
                        </div>
                        <div className="h-px bg-gradient-to-r from-gray-600 via-gray-500 to-transparent" />
                        <div className="flex items-center gap-3">
                            <span className="text-green-400 text-xl font-semibold">{repos.length}</span>
                            <span className="text-gray-300 text-xl font-semibold">
                {repos.length === 1 ? 'repository' : 'repositories'}
              </span>
                            <span className="text-gray-500 text-xl font-semibold">found</span>
                            <span className="ml-2 text-green-400 text-xl">●</span>
                        </div>
                        <div className="flex items-center gap-4 text-xs text-gray-500">
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-green-400 inline-block" />
                  {connected.size} connected
              </span>
                            <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-gray-600 inline-block" />
                                {repos.length - connected.size} available
              </span>
                        </div>
                    </div>

                    {reposLoading ? (
                        <div className="text-gray-400 text-lg flex items-center gap-3">
                            <span>Fetching repos...</span>
                            <span className="cursor-blink text-green-400">█</span>
                        </div>
                    ) : repos.length === 0 ? (
                        <div className="text-gray-400 text-lg border border-gray-700 bg-gray-900/20 p-6 rounded-lg">
                            No repositories found. Connect a GitHub account to continue.
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                            {sortedRepos.map((repo) => (
                                <RepoCard
                                    key={repo.id || repo.name}
                                    repo={repo}
                                    connected={connected.has(repo.name)}
                                    onConnect={handleConnect}
                                    latestScore={connected.has(repo.name) ? scores[repo.name] : undefined}
                                />
                            ))}
                        </div>
                    )}

                    <div className="mt-12 pt-6 border-t border-gray-800 flex items-center gap-2 text-sm text-gray-500">
                        <span className="text-green-400">$</span>
                        <span className="text-gray-400">Select a repository to enable AI-powered code review</span>
                        <span className="cursor-blink text-green-400 ml-1">█</span>
                    </div>
                </main>
            </div>
        </div>
    );
}