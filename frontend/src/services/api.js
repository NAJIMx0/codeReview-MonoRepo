const BASE = '/api/auth';
const REVIEWS_BASE = '/api/reviews';

export const getMe = () =>
    fetch(`${BASE}/me`, { credentials: 'include' })
        .then(r => {
            if (!r.ok) throw new Error('Unauthorized');
            return r.text();
        });

export const getRepos = () =>
    fetch(`${BASE}/repo`, { credentials: 'include' })
        .then(r => {
            if (!r.ok) throw new Error('Failed to fetch repos');
            return r.json();
        });

export const connectRepo = (owner, repoName) =>
    fetch(`${BASE}/connect/${owner}/${repoName}`, {
        method: 'POST',
        credentials: 'include',
    }).then(r => {
        if (!r.ok) throw new Error('Failed to connect repo');
        return r.json();
    });

export const getConnectedRepos = () =>
    fetch(`${BASE}/connected-repos`, { credentials: 'include' })
        .then(r => {
            if (!r.ok) throw new Error('Failed to fetch connected repos');
            return r.json();
        });

// Returns the latest saved review for a repo, or null if none exists yet.
export const getLatestReview = (repoName) =>
    fetch(`${REVIEWS_BASE}/latest?repo=${encodeURIComponent(repoName)}`, { credentials: 'include' })
        .then(r => {
            if (r.status === 204) return null;
            if (!r.ok) throw new Error('Failed to fetch latest review');
            return r.json();
        });

// Returns every saved review for a repo, most recent first.
export const getReviewHistory = (repoName) =>
    fetch(`${REVIEWS_BASE}/history?repo=${encodeURIComponent(repoName)}`, { credentials: 'include' })
        .then(r => {
            if (!r.ok) throw new Error('Failed to fetch review history');
            return r.json();
        });

export const disconnectRepo = (repoName) =>
    fetch(`${BASE}/connected-repos/${encodeURIComponent(repoName)}`, {
        method: 'DELETE',
        credentials: 'include',
    }).then(r => {
        if (!r.ok) throw new Error('Failed to disconnect repo');
        return r.json();
    });