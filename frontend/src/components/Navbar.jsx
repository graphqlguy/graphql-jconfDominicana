import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCaps } from '../context/CapabilitiesContext';

export default function Navbar() {
  const { isLoggedIn, isAdmin, user, logout } = useAuth();
  const { caps, docs } = useCaps();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  // Before the security class exists there is no login, so everyone may admin.
  const authBuilt = !!docs.LOGIN;
  const canAdmin = !!docs.CREATE_MOVIE && (isAdmin || !authBuilt);

  return (
    <nav className="bg-zinc-900 border-b border-zinc-800 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center gap-8">
            <Link to="/" className="text-yellow-400 font-bold text-xl tracking-tight">
              🎬 MovieDB
            </Link>
            <div className="hidden md:flex items-center gap-6">
              <Link to="/" className="text-zinc-300 hover:text-white transition-colors text-sm">Movies</Link>
              {caps?.query('people') && (
                <Link to="/people" className="text-zinc-300 hover:text-white transition-colors text-sm">People</Link>
              )}
              {caps?.query('tvShows') && (
                <Link to="/tvshows" className="text-zinc-300 hover:text-white transition-colors text-sm">TV Shows</Link>
              )}
              {caps?.query('watchlist') && isLoggedIn && (
                <Link to="/watchlist" className="text-zinc-300 hover:text-white transition-colors text-sm">Watch List</Link>
              )}
              {canAdmin && (
                <Link to="/admin" className="text-yellow-400 hover:text-yellow-300 transition-colors text-sm font-medium">
                  + Add Movie
                </Link>
              )}
            </div>
          </div>
          <div className="flex items-center gap-4">
            {isLoggedIn ? (
              <>
                <span className="text-zinc-400 text-sm hidden md:block">
                  {user?.username}
                  {isAdmin && <span className="ml-2 text-xs bg-yellow-500/20 text-yellow-400 px-2 py-0.5 rounded">ADMIN</span>}
                </span>
                <button
                  onClick={handleLogout}
                  className="text-sm text-zinc-400 hover:text-white transition-colors"
                >
                  Sign out
                </button>
              </>
            ) : authBuilt && (
              <>
                <Link to="/login" className="text-zinc-300 hover:text-white transition-colors text-sm">Sign in</Link>
                {docs.REGISTER && (
                  <Link
                    to="/register"
                    className="bg-yellow-500 hover:bg-yellow-400 text-black text-sm font-semibold px-4 py-2 rounded transition-colors"
                  >
                    Register
                  </Link>
                )}
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
