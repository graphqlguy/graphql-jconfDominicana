import { useQuery, useMutation } from '@apollo/client/react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useCaps } from '../context/CapabilitiesContext';
import { NOOP } from '../graphql/documents';
import FeaturePending from '../components/FeaturePending';

const STATUS_LABEL = {
  WANT_TO_WATCH: 'Want to watch',
  WATCHED: 'Watched',
};

function itemLink(content) {
  return content.__typename === 'Movie' ? `/movie/${content.id}` : `/tvshow/${content.id}`;
}

export default function WatchlistPage() {
  const { isLoggedIn } = useAuth();
  const { docs } = useCaps();

  const { data, loading, error } = useQuery(docs.GET_WATCHLIST ?? NOOP, {
    skip: !docs.GET_WATCHLIST || !isLoggedIn,
  });

  const refetch = { refetchQueries: [{ query: docs.GET_WATCHLIST }] };
  const [setWatchStatus] = useMutation(docs.SET_WATCH_STATUS ?? NOOP, refetch);
  const [removeFromWatchlist] = useMutation(docs.REMOVE_FROM_WATCHLIST ?? NOOP, refetch);

  if (!docs.GET_WATCHLIST) return (
    <FeaturePending
      title="No watch list yet"
      hint="Add the watchlist query and the WatchlistItem type to the schema to track what you want to watch."
    />
  );

  if (!isLoggedIn) return (
    <div className="max-w-2xl mx-auto px-4 py-24 text-center text-zinc-400">
      <p className="text-4xl mb-4">🔒</p>
      <p><Link to="/login" className="text-yellow-400 hover:underline">Sign in</Link> to see your watch list.</p>
    </div>
  );

  const items = data?.watchlist || [];

  const toggleStatus = async (item) => {
    const next = item.status === 'WATCHED' ? 'WANT_TO_WATCH' : 'WATCHED';
    try {
      await setWatchStatus({ variables: { itemId: item.id, status: next } });
    } catch (err) {
      alert(err.message);
    }
  };

  const remove = async (item) => {
    try {
      await removeFromWatchlist({ variables: { itemId: item.id } });
    } catch (err) {
      alert(err.message);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-white mb-2">My Watch List</h1>
        {items.length > 0 && <p className="text-zinc-400">{items.length} titles</p>}
      </div>

      {loading && <div className="text-center py-16 text-zinc-500">Loading your watch list...</div>}
      {error && <div className="text-center py-16 text-red-400">Error: {error.message}</div>}

      {!loading && !error && items.length === 0 && (
        <div className="text-center py-16 text-zinc-500">
          Nothing here yet. Open a movie or show and add it to your watch list.
        </div>
      )}

      {!loading && !error && items.length > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {items.map(item => {
            const c = item.content;
            const watched = item.status === 'WATCHED';
            return (
              <div key={item.id} className="bg-zinc-900 border border-zinc-800 rounded-xl overflow-hidden flex flex-col">
                <Link to={itemLink(c)} className="block aspect-[2/3] bg-zinc-800 relative overflow-hidden group">
                  {c.posterUrl ? (
                    <img src={c.posterUrl} alt={c.title}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-zinc-600 text-4xl">🎬</div>
                  )}
                  <span className="absolute top-2 left-2 text-xs px-1.5 py-0.5 rounded bg-black/70 text-zinc-300">
                    {c.__typename === 'TvShow' ? 'TV' : 'Film'}
                  </span>
                  <span className={`absolute top-2 right-2 text-xs px-1.5 py-0.5 rounded font-medium ${
                    watched ? 'bg-teal-500/30 text-teal-300' : 'bg-yellow-500/20 text-yellow-300'}`}>
                    {STATUS_LABEL[item.status]}
                  </span>
                </Link>
                <div className="p-3 flex flex-col gap-2 flex-1">
                  <Link to={itemLink(c)} className="text-white font-medium text-sm leading-snug hover:text-yellow-400 transition-colors">
                    {c.title}
                  </Link>
                  <div className="mt-auto flex gap-2">
                    <button
                      onClick={() => toggleStatus(item)}
                      className="flex-1 bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 text-zinc-200 text-xs px-2 py-1.5 rounded transition-colors"
                    >
                      {watched ? 'Mark unwatched' : 'Mark watched'}
                    </button>
                    <button
                      onClick={() => remove(item)}
                      className="bg-zinc-800 hover:bg-red-900/40 border border-zinc-700 hover:border-red-800 text-zinc-500 hover:text-red-400 text-xs px-2 py-1.5 rounded transition-colors"
                      title="Remove"
                    >
                      ✕
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
