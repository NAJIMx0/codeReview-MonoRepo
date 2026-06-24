import { BrowserRouter, Routes, Route } from 'react-router-dom';
import LandingPage from './pages/LandingPage';
import Dashboard from './pages/Dashboard';
import Review from "./pages/Review.jsx";
import Settings from "./pages/Settings.jsx";
import Profile from "./pages/Profile.jsx";
import RepoHistory from "./pages/RepoHistory.jsx";

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<LandingPage />} />
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/review" element={<Review />} />
                <Route path="/settings" element={<Settings />} />
                <Route path="/profile" element={<Profile />} />
                <Route path="/history/:owner/:repoName" element={<RepoHistory />} />
            </Routes>
        </BrowserRouter>
    );
}