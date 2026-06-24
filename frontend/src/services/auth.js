export const AUTH_BASE = '';

export const loginWithGitHub = () => {
  window.location.href = `${AUTH_BASE}/oauth2/authorization/github`;
};

export const logout = async () => {
  // 1. Revoke first while session is still alive
  try {
    await fetch(`${AUTH_BASE}/api/auth/revoke`, {
      method: 'POST',
      credentials: 'include',
    });
  } catch (e) {
    console.log('revoke failed, continuing');
  }

  // 2. Kill the Spring session
  try {
    await fetch(`${AUTH_BASE}/api/auth/logout`, {
      method: 'POST',
      credentials: 'include',
      redirect: 'manual',
    });
  } catch (e) {
    console.log('logout failed, continuing');
  }

  // 3. Clear localStorage AFTER server calls complete
  localStorage.clear(); // ✅ clear everything, not just username

  // 4. Navigate — use href not replace so page fully reloads
  window.location.href = 'http://localhost:5173';
};