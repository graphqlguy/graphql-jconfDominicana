import { useState, useEffect } from 'react';
import { useMutation, useQuery, useLazyQuery } from '@apollo/client/react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useCaps } from '../context/CapabilitiesContext';
import { NOOP } from '../graphql/documents';
import { useAuth } from '../context/AuthContext';
import FeaturePending from '../components/FeaturePending';

const GENRES = ['ACTION','COMEDY','CRIME','DRAMA','FANTASY',
  'HORROR','MYSTERY','ROMANCE','SCIFI','THRILLER','WAR','WESTERN'];

const EMPTY_FORM = {
  title: '', releaseYear: new Date().getFullYear(), genre: 'DRAMA',
  rating: '', runtime: '', plot: '',
  posterUrl: '', tmdbId: '',
};

export default function AdminPage() {
  const { isAdmin } = useAuth();
  const { docs } = useCaps();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const editId = searchParams.get('edit');

  const [form, setForm] = useState(EMPTY_FORM);
  const [tmdbQuery, setTmdbQuery] = useState('');
  const [tmdbResults, setTmdbResults] = useState([]);
  const [message, setMessage] = useState('');
  const [tmdbSearched, setTmdbSearched] = useState(false);
  const [tmdbNotConfigured, setTmdbNotConfigured] = useState(false);

  const [createMovie, { loading: creating }] = useMutation(docs.CREATE_MOVIE ?? NOOP);
  const [updateMovie, { loading: updating }] = useMutation(docs.UPDATE_MOVIE ?? NOOP);
  const [searchTmdb, { loading: tmdbLoading }] = useLazyQuery(docs.TMDB_SEARCH ?? NOOP);

  const { data: movieData } = useQuery(docs.GET_MOVIE ?? NOOP, {
    variables: { id: editId },
    skip: !editId || !docs.GET_MOVIE,
    fetchPolicy: 'network-only',
  });

  useEffect(() => {
    if (movieData?.movie) {
      const m = movieData.movie;
      // eslint-disable-next-line react-hooks/set-state-in-effect -- prefilling the form from a fetched movie is inherently effect-driven
      setForm({
        title: m.title, releaseYear: m.releaseYear, genre: m.genre,
        rating: m.rating || '', runtime: m.runtime || '', plot: m.plot || '',
        posterUrl: m.posterUrl || '', tmdbId: m.tmdbId || '',
      });
    }
  }, [movieData]);

  if (!docs.CREATE_MOVIE) {
    return (
      <FeaturePending
        title="Mutations aren't built yet"
        hint="Add the createMovie mutation during the mutations class and this page will start working."
      />
    );
  }

  // Before the security class exists there is no login, so everyone may admin.
  if (docs.LOGIN && !isAdmin) {
    return (
      <div className="max-w-7xl mx-auto px-4 py-16 text-center">
        <h1 className="text-2xl text-white mb-4">Access Denied</h1>
        <p className="text-zinc-400 mb-6">You need admin privileges to access this page.</p>
        <button onClick={() => navigate('/login')} className="bg-yellow-500 text-black px-6 py-2 rounded-lg font-semibold">
          Sign in as admin
        </button>
      </div>
    );
  }

  const handleTmdbSearch = async (e) => {
    e.preventDefault();
    if (!tmdbQuery.trim()) return;
    const { data, error } = await searchTmdb({ variables: { title: tmdbQuery } });
    if (error?.message?.includes('TMDB_API_KEY')) {
      setTmdbNotConfigured(true);
      setTmdbResults([]);
    } else {
      setTmdbNotConfigured(false);
      setTmdbResults(data?.tmdbSearch || []);
    }
    setTmdbSearched(true);
  };

  const fillFromTmdb = (result) => {
    setForm(f => ({
      ...f,
      title: result.title,
      releaseYear: result.releaseYear || f.releaseYear,
      plot: result.overview || f.plot,
      rating: result.rating ? Math.round(result.rating * 10) / 10 : f.rating,
      posterUrl: result.posterUrl || f.posterUrl,
      tmdbId: result.tmdbId,
    }));
    setTmdbResults([]);
    setTmdbQuery('');
    setMessage('Movie details filled from TMDB!');
    setTimeout(() => setMessage(''), 3000);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage('');
    try {
      const input = {
        title: form.title,
        releaseYear: parseInt(form.releaseYear),
        genre: form.genre,
        rating: form.rating ? parseFloat(form.rating) : null,
        runtime: form.runtime ? parseInt(form.runtime) : null,
        plot: form.plot || null,
        posterUrl: form.posterUrl || null,
        tmdbId: form.tmdbId ? parseInt(form.tmdbId) : null,
      };

      if (editId) {
        await updateMovie({ variables: { input: { id: editId, ...input } } });
        setMessage('Movie updated successfully!');
      } else {
        const { data } = await createMovie({ variables: { input } });
        setMessage('Movie created!');
        setForm(EMPTY_FORM);
        setTimeout(() => navigate(`/movie/${data.createMovie.id}`), 1000);
      }
    } catch (err) {
      setMessage('Error: ' + err.message);
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-8">
      <h1 className="text-3xl font-bold text-white mb-2">
        {editId ? 'Edit Movie' : 'Add New Movie'}
      </h1>
      <p className="text-zinc-400 mb-8">
        {editId ? 'Update movie details' : 'Fill in the details or search TMDB to auto-fill'}
      </p>

      {/* TMDB Search (appears once the tmdbSearch query exists) */}
      {!editId && docs.TMDB_SEARCH && (
        <div className="bg-zinc-900 border border-zinc-800 rounded-xl p-6 mb-6">
          <h2 className="text-white font-semibold mb-4">Search TMDB</h2>
          <form onSubmit={handleTmdbSearch} className="flex gap-2">
            <input
              type="text"
              value={tmdbQuery}
              onChange={e => setTmdbQuery(e.target.value)}
              placeholder="Search movies on TMDB..."
              className="flex-1 bg-zinc-800 border border-zinc-700 rounded-lg px-4 py-2.5 text-white placeholder-zinc-500 focus:outline-none focus:border-yellow-500"
            />
            <button type="submit" disabled={tmdbLoading}
              className="bg-blue-600 hover:bg-blue-500 disabled:bg-blue-900 text-white font-semibold px-5 py-2.5 rounded-lg transition-colors">
              {tmdbLoading ? '...' : 'Search'}
            </button>
          </form>
          {tmdbResults.length > 0 && (
            <div className="mt-4 space-y-2 max-h-64 overflow-y-auto">
              {tmdbResults.map(r => (
                <button key={r.tmdbId} onClick={() => fillFromTmdb(r)}
                  className="w-full flex items-center gap-3 bg-zinc-800 hover:bg-zinc-700 rounded-lg p-3 text-left transition-colors">
                  {r.posterUrl && <img src={r.posterUrl} alt={r.title} className="w-10 h-14 object-cover rounded" />}
                  <div>
                    <div className="text-white font-medium">{r.title}</div>
                    <div className="text-zinc-400 text-sm">{r.releaseYear} · ★ {r.rating?.toFixed(1)}</div>
                  </div>
                </button>
              ))}
            </div>
          )}
          {tmdbNotConfigured && (
            <div className="mt-4 bg-yellow-900/30 border border-yellow-800 rounded-lg p-4 text-sm">
              <p className="text-yellow-400 font-medium mb-2">TMDB API key not configured</p>
              <p className="text-zinc-400 mb-2">To enable TMDB search, set your API key:</p>
              <ol className="text-zinc-400 list-decimal list-inside space-y-1">
                <li>Get a free API key at <span className="text-yellow-500">themoviedb.org/settings/api</span></li>
                <li>Copy the <span className="text-white">Read Access Token</span> (v4 bearer token)</li>
                <li>Set the environment variable before starting the backend:
                  <code className="block mt-1 bg-zinc-800 text-zinc-300 rounded px-3 py-1.5">TMDB_API_KEY=your_token_here ./mvnw spring-boot:run</code>
                </li>
                <li>Restart the backend</li>
              </ol>
            </div>
          )}
          {tmdbSearched && !tmdbLoading && !tmdbNotConfigured && tmdbResults.length === 0 && (
            <p className="mt-4 text-zinc-500 text-sm">
              No results found. If you expected results, note that TMDB search requires the backend
              to be started with a <code className="text-zinc-400">TMDB_API_KEY</code>; without one it returns no matches.
            </p>
          )}
        </div>
      )}

      {message && (
        <div className={`px-4 py-3 rounded-lg text-sm mb-6 ${
          message.startsWith('Error') ? 'bg-red-900/30 border border-red-800 text-red-400' : 'bg-green-900/30 border border-green-800 text-green-400'
        }`}>
          {message}
        </div>
      )}

      <form onSubmit={handleSubmit} className="bg-zinc-900 border border-zinc-800 rounded-xl p-6 space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="md:col-span-2">
            <label className="block text-zinc-400 text-sm mb-1.5">Title *</label>
            <input type="text" value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))}
              required className="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:border-yellow-500" />
          </div>
          <div>
            <label className="block text-zinc-400 text-sm mb-1.5">Release Year *</label>
            <input type="number" value={form.releaseYear} onChange={e => setForm(f => ({ ...f, releaseYear: e.target.value }))}
              required min="1888" max="2030" className="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:border-yellow-500" />
          </div>
          <div>
            <label className="block text-zinc-400 text-sm mb-1.5">Genre *</label>
            <select value={form.genre} onChange={e => setForm(f => ({ ...f, genre: e.target.value }))}
              className="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:border-yellow-500">
              {GENRES.map(g => <option key={g} value={g}>{g}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-zinc-400 text-sm mb-1.5">Rating (0-10)</label>
            <input type="number" value={form.rating} onChange={e => setForm(f => ({ ...f, rating: e.target.value }))}
              min="0" max="10" step="0.1" className="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:border-yellow-500" />
          </div>
          <div>
            <label className="block text-zinc-400 text-sm mb-1.5">Runtime (minutes)</label>
            <input type="number" value={form.runtime} onChange={e => setForm(f => ({ ...f, runtime: e.target.value }))}
              min="1" className="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:border-yellow-500" />
          </div>
          <div className="md:col-span-2">
            <label className="block text-zinc-400 text-sm mb-1.5">Plot</label>
            <textarea value={form.plot} onChange={e => setForm(f => ({ ...f, plot: e.target.value }))}
              rows={3} className="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:border-yellow-500 resize-none" />
          </div>
          <div className="md:col-span-2">
            <label className="block text-zinc-400 text-sm mb-1.5">Poster URL</label>
            <div className="flex gap-3">
              <input type="url" value={form.posterUrl} onChange={e => setForm(f => ({ ...f, posterUrl: e.target.value }))}
                className="flex-1 bg-zinc-800 border border-zinc-700 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:border-yellow-500" placeholder="https://..." />
              {form.posterUrl && (
                <img src={form.posterUrl} alt="poster preview" className="w-12 h-16 object-cover rounded" onError={e => e.target.style.display='none'} />
              )}
            </div>
          </div>
          <div>
            <label className="block text-zinc-400 text-sm mb-1.5">TMDB ID</label>
            <input type="number" value={form.tmdbId} onChange={e => setForm(f => ({ ...f, tmdbId: e.target.value }))}
              className="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:border-yellow-500" />
          </div>
        </div>

        <div className="flex gap-3 pt-2">
          <button type="submit" disabled={creating || updating}
            className="bg-yellow-500 hover:bg-yellow-400 disabled:bg-yellow-800 text-black font-semibold px-8 py-3 rounded-lg transition-colors">
            {creating || updating ? 'Saving...' : editId ? 'Update Movie' : 'Add Movie'}
          </button>
          <button type="button" onClick={() => navigate(-1)}
            className="bg-zinc-800 hover:bg-zinc-700 text-zinc-300 px-6 py-3 rounded-lg transition-colors">
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
