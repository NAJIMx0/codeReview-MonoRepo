import { useState, useEffect } from 'react';
import Navbar from '../components/Navbar';
import { useAuth } from '../hooks/useAuth';
import FileReviewCard from '../components/FileReviewCard';

export default function Review() {
    const { username } = useAuth();
    const [reviews, setReviews] = useState([]); // array of {repo, files_analyzed, results, receivedAt}
    const [connected, setConnected] = useState(false);

    useEffect(() => {
        const eventSource = new EventSource('/api/generate/stream', {
            withCredentials: true
        });

        eventSource.onopen = () => setConnected(true);

        eventSource.onmessage = (event) => {
            const data = JSON.parse(event.data);
            setReviews((prev) => {
                // If this push (same repo) already has an entry, merge/replace it
                // (the AI-enriched message arrives slightly after the raw one).
                const existingIndex = prev.findIndex((r) => r.repo === data.repo);
                const withTimestamp = { ...data, receivedAt: new Date() };
                if (existingIndex === -1) {
                    return [withTimestamp, ...prev];
                }
                const updated = [...prev];
                updated[existingIndex] = withTimestamp;
                return updated;
            });
        };

        eventSource.onerror = () => {
            setConnected(false);
            eventSource.close();
        };

        return () => eventSource.close();
    }, []);

    return (
        <div className="min-h-screen bg-black">
            <Navbar username={username} />
            <main className="max-w-4xl mx-auto px-6 py-8 space-y-6">
                <div className="flex items-center justify-between">
                    <div className="flex items-start gap-2">
                        <span className="text-green-400 text-base font-semibold shrink-0 mt-0.5">~/codereview $</span>
                        <span className="text-white text-base font-semibold">tail -f review.log</span>
                    </div>
                    <div className="flex items-center gap-2 text-xs">
                        <span className={`w-2 h-2 rounded-full inline-block ${connected ? 'bg-green-400 animate-pulse' : 'bg-red-500'}`} />
                        <span className="text-gray-500">{connected ? 'live' : 'disconnected'}</span>
                    </div>
                </div>
                <div className="h-px bg-gradient-to-r from-gray-600 via-gray-500 to-transparent" />

                {reviews.length === 0 ? (
                    <div className="text-gray-600 text-sm border border-gray-800 bg-gray-900/20 p-8 text-center">
                        Waiting for a push... push code to a connected repo and the review will appear here automatically.
                    </div>
                ) : (
                    reviews.map((review, i) => (
                        <div key={i} className="space-y-3">
                            <div className="flex items-center gap-2 text-xs text-gray-500">
                                <span className="text-green-400 font-bold">{review.repo}</span>
                                <span>·</span>
                                <span>{review.files_analyzed} file{review.files_analyzed !== 1 ? 's' : ''}</span>
                                {review.receivedAt && (
                                    <>
                                        <span>·</span>
                                        <span>{review.receivedAt.toLocaleTimeString()}</span>
                                    </>
                                )}
                            </div>
                            <div className="space-y-3">
                                {review.results?.map((result, j) => (
                                    <FileReviewCard key={j} result={result} />
                                ))}
                            </div>
                        </div>
                    ))
                )}
            </main>
        </div>
    );
}