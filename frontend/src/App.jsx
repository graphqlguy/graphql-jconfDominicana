import { Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import HomePage from './pages/HomePage';
import MovieDetailPage from './pages/MovieDetailPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import AdminPage from './pages/AdminPage';
import PeoplePage from './pages/PeoplePage';
import PersonDetailPage from './pages/PersonDetailPage';
import TvShowsPage from './pages/TvShowsPage';
import TvShowDetailPage from './pages/TvShowDetailPage';
import WatchlistPage from './pages/WatchlistPage';
import { useCaps } from './context/CapabilitiesContext';

function StatusPanel({ emoji, title, children, onRetry }) {
  return (
    <div className="max-w-2xl mx-auto px-4 py-24 text-center">
      <p className="text-5xl mb-4">{emoji}</p>
      <h1 className="text-2xl font-bold text-white mb-4">{title}</h1>
      <div className="text-zinc-400 leading-relaxed space-y-3">{children}</div>
      <button
        onClick={onRetry}
        className="mt-8 bg-yellow-500 hover:bg-yellow-400 text-black font-semibold px-6 py-2.5 rounded-lg transition-colors"
      >
        Try again
      </button>
    </div>
  );
}

export default function App() {
  const { status, reload } = useCaps();

  return (
    <div className="min-h-screen bg-[#0f0f0f]">
      <Navbar />
      <main>
        {status === 'loading' && (
          <div className="text-center py-24 text-zinc-500">Connecting to the API...</div>
        )}

        {status === 'backend-down' && (
          <StatusPanel emoji="🔌" title="Backend is not running" onRetry={reload}>
            <p>Nothing is answering on the backend port.</p>
            <p>
              Start it from the project root with{' '}
              <code className="bg-zinc-800 text-zinc-300 rounded px-2 py-0.5">./mvnw spring-boot:run</code>{' '}
              and try again.
            </p>
          </StatusPanel>
        )}

        {status === 'no-graphql' && (
          <StatusPanel emoji="🚀" title="No GraphQL endpoint yet" onRetry={reload}>
            <p>The backend is running, but it has no <code className="bg-zinc-800 text-zinc-300 rounded px-2 py-0.5">/graphql</code> endpoint yet.</p>
            <p>
              This is where the workshop starts: add the Spring for GraphQL dependency and your
              first schema file, restart the backend, and this app will come alive.
            </p>
          </StatusPanel>
        )}

        {status === 'ready' && (
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/movie/:id" element={<MovieDetailPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/admin" element={<AdminPage />} />
            <Route path="/people" element={<PeoplePage />} />
            <Route path="/person/:id" element={<PersonDetailPage />} />
            <Route path="/tvshows" element={<TvShowsPage />} />
            <Route path="/tvshow/:id" element={<TvShowDetailPage />} />
            <Route path="/watchlist" element={<WatchlistPage />} />
          </Routes>
        )}
      </main>
    </div>
  );
}
