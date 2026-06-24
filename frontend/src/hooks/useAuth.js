import { useState, useEffect } from 'react';
import { getMe } from '../services/api';

export function useAuth() {
    const [username, setUsername] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [succesgs, setSuccessg] = useState(null);
///// test the generate  day2 v4
    useEffect(() => {
        // ✅ No localStorage, no hasFetched — always ask the server
        getMe()
            .then((name) => {
                setUsername(name);
            })
            .catch(() => {
                setUsername(null);
            })
            .finally(() => setLoading(false));
    }, []);

    return { username, loading };
}



// test the generate-service get code v5