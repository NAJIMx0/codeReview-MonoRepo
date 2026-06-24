function scoreColor(score) {
    if (score >= 85) return { text: 'text-green-400', border: 'border-green-700', bg: 'bg-green-950' };
    if (score >= 60) return { text: 'text-yellow-400', border: 'border-yellow-700', bg: 'bg-yellow-950' };
    return { text: 'text-red-400', border: 'border-red-700', bg: 'bg-red-950' };
}

export default function FileReviewCard({ result }) {
    if (result.skipped) {
        return (
            <div className="border border-gray-800 bg-[#0a0a0a] p-4 text-gray-600 text-xs">
                <span className="font-bold">{result.file}</span> — skipped ({result.reason})
            </div>
        );
    }

    const colors = scoreColor(result.quality_score ?? 0);
    const styleIssues = result.style?.total_issues ?? 0;
    const dupCount = result.duplication?.total_duplications ?? 0;
    const bigO = result.complexity?.overall_big_o ?? '—';

    // Collect all duplication suggestions across the three kinds of checks
    // (duplicate functions, duplicate variables, duplicate loop blocks)
    // so we can show *why* the count is non-zero, not just the number.
    const dupSuggestions = [
        ...(result.duplication?.duplicated_functions ?? []).map(d => d.suggestion),
        ...(result.duplication?.duplicated_variables ?? []).map(d => d.suggestion),
        ...(result.duplication?.duplicated_blocks ?? []).map(d => d.suggestion),
    ];

    return (
        <div className="border border-gray-800 bg-[#0a0a0a] overflow-hidden">
            <div className="px-4 py-3 border-b border-gray-800 flex items-center justify-between">
                <div className="flex items-center gap-2">
                    <span className="text-green-400 text-xs">◈</span>
                    <span className="text-white text-sm font-bold">{result.file}</span>
                </div>
                <span className={`px-2 py-0.5 text-xs font-bold border ${colors.text} ${colors.border} ${colors.bg}`}>
                    {result.quality_score}/100
                </span>
            </div>

            <div className="p-4 space-y-3">
                <div className="grid grid-cols-3 gap-3 text-xs">
                    <div className="border border-gray-800 p-2">
                        <div className="text-gray-600 mb-1">complexity</div>
                        <div className="text-gray-300 font-bold">{bigO}</div>
                    </div>
                    <div className="border border-gray-800 p-2">
                        <div className="text-gray-600 mb-1">style issues</div>
                        <div className={styleIssues > 0 ? 'text-yellow-400 font-bold' : 'text-green-400 font-bold'}>
                            {styleIssues}
                        </div>
                    </div>
                    <div className="border border-gray-800 p-2">
                        <div className="text-gray-600 mb-1">duplication</div>
                        <div className={dupCount > 0 ? 'text-yellow-400 font-bold' : 'text-green-400 font-bold'}>
                            {dupCount}
                        </div>
                    </div>
                </div>

                {dupSuggestions.length > 0 && (
                    <div className="border border-yellow-900 bg-yellow-950/20 p-3 space-y-1.5">
                        <div className="text-yellow-500 text-xs font-bold uppercase tracking-wider">
                            duplication details
                        </div>
                        {dupSuggestions.map((s, i) => (
                            <p key={i} className="text-gray-400 text-xs leading-relaxed">• {s}</p>
                        ))}
                    </div>
                )}

                <div className="border-t border-gray-800 pt-3">
                    <div className="flex items-center gap-2 mb-1.5">
                        <span className="text-green-400 text-xs">💬</span>
                        <span className="text-gray-500 text-xs uppercase tracking-wider">AI review</span>
                    </div>
                    {result.ai_review ? (
                        <p className="text-gray-300 text-xs leading-relaxed pl-5">{result.ai_review}</p>
                    ) : (
                        <p className="text-gray-600 text-xs italic pl-5">waiting for AI review...</p>
                    )}
                </div>
            </div>
        </div>
    );
}