import { useState, useEffect, useCallback } from 'react';

const STORAGE_KEY = 'codereview-theme';

function getInitialTheme() {
    if (typeof window === 'undefined') return 'dark';
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored === 'light' || stored === 'dark') return stored;
    return 'dark'; // default
}

export function useTheme() {
    const [theme, setThemeState] = useState(getInitialTheme);

    // Apply the theme class to <html> whenever it changes, so any CSS
    // relying on .light / .dark (or Tailwind's dark: variant) works app-wide.
    useEffect(() => {
        const root = document.documentElement;
        root.classList.remove('dark', 'light');
        root.classList.add(theme);
        localStorage.setItem(STORAGE_KEY, theme);
    }, [theme]);

    const setTheme = useCallback((t) => {
        if (t === 'dark' || t === 'light') setThemeState(t);
    }, []);

    return { theme, setTheme };
}