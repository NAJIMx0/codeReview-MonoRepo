import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import App from './App';

// Apply saved theme immediately, before React even renders, so there's
// no flash of dark mode before light mode kicks in (or vice versa).
const savedTheme = localStorage.getItem('codereview-theme');
document.documentElement.classList.add(savedTheme === 'light' ? 'light' : 'dark');

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <App />
    </StrictMode>,
);