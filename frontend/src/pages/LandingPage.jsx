import { loginWithGitHub } from '../services/auth';
import { useAuth } from '../hooks/useAuth';
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import SnakeBackground from '../components/SnakeBackground';

export default function LandingPage() {
  const navigate = useNavigate();
  const { username, loading } = useAuth();

  useEffect(() => {
    if (!loading && username) {
      navigate('/dashboard', { replace: true });
    }
  }, [username, loading, navigate]);

  if (loading) return null;

  return (
      <div className="min-h-screen bg-black text-gray-300 flex items-center justify-center p-6 relative">
        <SnakeBackground />
        <div className="max-w-2xl w-full relative z-10">
          <div className="border border-gray-700 rounded-lg overflow-hidden shadow-2xl shadow-black/30">
            <div className="flex items-center justify-between px-4 py-3 border-b border-gray-700 bg-[#0a0a0a]">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded-full bg-gray-700" />
                <div className="w-3 h-3 rounded-full bg-gray-600" />
                <div className="w-3 h-3 rounded-full bg-gray-500" />
              </div>
              <span className="text-xs text-gray-500">codereview@local ~ /auth</span>
            </div>
            <div className="p-8 md:p-12 space-y-6 bg-black/60 backdrop-blur-sm">
<pre className="text-green-400 text-sm leading-tight hidden md:block">
{`  ╔═╗╔═╗╔╦╗╔═╗  ╦═╗╔═╗╦  ╦╦╔═╗╦ ╦
  ║  ║ ║ ║║├╣   ╠╦╝║╣ ╚╗╔╝║║╣ ║║║
  ╚═╝╚═╝═╩╝╚═╝  ╩╚═╚═╝ ╚╝ ╩╚═╝╚╩╝`}
</pre>
              <h1 className="text-3xl md:text-4xl font-bold text-white tracking-wider md:hidden">
                CODE REVIEW
              </h1>
              <div className="h-px bg-gradient-to-r from-gray-600 via-gray-500 to-transparent" />
              <div className="space-y-3">
                <div className="flex items-start gap-2">
                  <span className="text-green-400 text-sm shrink-0">~/codereview $</span>
                  <span className="text-white text-sm">echo $DESCRIPTION</span>
                </div>
                <p className="text-lg text-gray-300 leading-relaxed pl-20 md:pl-24">
                  AI-Powered Code Review — connect your GitHub repositories and get instant intelligent feedback on every push.
                </p>
              </div>
              <div className="space-y-2 pl-4">
                <div className="flex items-center gap-3 text-sm">
                  <span className="text-green-400">[✓]</span>
                  <span className="text-gray-300">Automated PR reviews</span>
                </div>
                <div className="flex items-center gap-3 text-sm">
                  <span className="text-green-400">[✓]</span>
                  <span className="text-gray-300">Security vulnerability detection</span>
                </div>
                <div className="flex items-center gap-3 text-sm">
                  <span className="text-green-400">[✓]</span>
                  <span className="text-gray-300">Code quality scoring</span>
                </div>
              </div>
              <div className="h-px bg-gray-800" />
              <div className="flex items-center gap-3 text-sm">
                <span className="text-gray-500">STATUS:</span>
                <span className="text-green-400 font-bold">AWAITING AUTHENTICATION</span>
              </div>
              <button
                  onClick={loginWithGitHub}
                  className="w-full py-4 text-base font-bold text-black bg-green-400 hover:bg-green-300 uppercase tracking-widest border-2 border-green-400 transition-all hover:shadow-lg hover:shadow-green-500/20"
              >
                &gt; AUTHENTICATE WITH GITHUB
              </button>
              <div className="flex items-center gap-2 text-sm text-gray-600 pt-1">
                <span className="text-green-400">$</span>
                <span className="text-gray-400">Press the button above or type</span>
                <code className="px-2 py-0.5 bg-gray-900 text-gray-300 border border-gray-700 text-xs">auth github</code>
                <span className="text-gray-400">to continue</span>
                <span className="cursor-blink text-green-400 ml-1">█</span>
              </div>
            </div>
          </div>
        </div>
      </div>
  );
}