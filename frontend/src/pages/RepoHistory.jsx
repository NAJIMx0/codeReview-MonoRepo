import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import FileReviewCard from '../components/FileReviewCard';
import { useAuth } from '../hooks/useAuth';
import { getReviewHistory } from '../services/api';

export default function RepoHistory() {
    const { owner, repoName } = useParams(); // /history/:owner/:repoName
    const { username } = useAuth();
    const navigate = useNavigate();

    const fullRepoName = `${owner}/${repoName}`;
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedIndex, setSelectedIndex] = useState(0);

    useEffect(() => {
        getReviewHistory(fullRepoName)
            .then((data) => {
                setHistory(Array.isArray(data) ? data : []);
            })
            .catch(() => setHistory([]))
            .finally(() => setLoading(false));
    }, [fullRepoName]);

    const selected = history[selectedIndex];

    return (
        <div className="min-h-screen bg-black">
            <Navbar username={username} />
            <main className="max-w-5xl mx-auto px-6 py-8 space-y-6">
                <div className="flex items-center gap-2">
                    <button
                        onClick={() => navigate('/dashboard')}
                        className="text-gray-500 hover:text-green-400 text-xs transition"
                    >
                        ← back
                    </button>
                </div>

                <div className="flex items-start gap-2">
                    <span className="text-green-400 text-base font-semibold shrink-0 mt-0.5">~/codereview $</span>
                    <span className="text-white text-base font-semibold">history {fullRepoName}</span>
                </div>
                <div className="h-px bg-gradient-to-r from-gray-600 via-gray-500 to-transparent" />

                {loading ? (
                    <div className="text-gray-400 text-sm">Loading history...</div>
                ) : history.length === 0 ? (
                    <div className="text-gray-600 text-sm border border-gray-800 bg-gray-900/20 p-8 text-center">
                        No reviews yet for this repo. Push some code to get started.
                    </div>
                ) : (
                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                        {/* Left: list of past pushes */}
                        <div className="lg:col-span-1 space-y-2">
                            <p className="text-gray-600 text-xs uppercase tracking-wider mb-2">
                                {history.length} push{history.length !== 1 ? 'es' : ''}
                            </p>
                            {history.map((entry, i) => {
                                const firstResult = entry.payload?.results?.[0];
                                const score = firstResult?.quality_score;
                                const isSelected = i === selectedIndex;
                                return (
                                    <button
                                        key={entry.id || i}
                                        onClick={() => setSelectedIndex(i)}
                                        className={`w-full text-left px-3 py-2.5 border text-xs transition ${
                                            isSelected
                                                ? 'border-green-600 bg-green-950/30'
                                                : 'border-gray-800 bg-[#0a0a0a] hover:border-gray-600'
                                        }`}
                                    >
                                        <div className="flex items-center justify-between mb-1">
                                            <span className={isSelected ? 'text-green-400 font-bold' : 'text-gray-300'}>
                                                {entry.payload?.files_analyzed ?? '?'} file{entry.payload?.files_analyzed !== 1 ? 's' : ''}
                                            </span>
                                            {score !== undefined && (
                                                <span className={score >= 85 ? 'text-green-400' : score >= 60 ? 'text-yellow-400' : 'text-red-400'}>
                                                    {score}/100
                                                </span>
                                            )}
                                        </div>
                                        <span className="text-gray-600">
                                            {entry.receivedAt ? new Date(entry.receivedAt).toLocaleString() : ''}
                                        </span>
                                    </button>
                                );
                            })}
                        </div>

                        {/* Right: detail of selected push */}
                        <div className="lg:col-span-2 space-y-3">
                            {selected ? (
                                selected.payload?.results?.map((result, j) => (
                                    <FileReviewCard key={j} result={result} />
                                ))
                            ) : (
                                <div className="text-gray-600 text-sm">Select a push to see its review.</div>
                            )}
                        </div>
                    </div>
                )}
            </main>
        </div>
    );
}