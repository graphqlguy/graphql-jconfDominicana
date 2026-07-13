import { useMutation, useQuery } from '@apollo/client/react';
import { useAuth } from '../context/AuthContext';
import { useCaps } from '../context/CapabilitiesContext';
import { NOOP } from '../graphql/documents';

// Watch-list control for a movie or TV show detail page. Reflects whether the
// title is already on the signed-in user's list and lets them change or remove it;
// otherwise offers to add it. Renders nothing when the feature is absent or the
// user is not logged in.
export default function WatchlistButton({ movieId, tvShowId }) {
  const { isLoggedIn } = useAuth();
  const { docs } = useCaps();

  const typename = movieId ? 'Movie' : 'TvShow';
  const contentId = String(movieId || tvShowId);

  const { data } = useQuery(docs.GET_WATCHLIST ?? NOOP, {
    skip: !docs.GET_WATCHLIST || !isLoggedIn,
  });

  const refetch = { refetchQueries: docs.GET_WATCHLIST ? [{ query: docs.GET_WATCHLIST }] : [] };
  const [addToWatchlist, { loading: adding }] = useMutation(docs.ADD_TO_WATCHLIST ?? NOOP, refetch);
  const [setWatchStatus, { loading: setting }] = useMutation(docs.SET_WATCH_STATUS ?? NOOP, refetch);
  const [removeFromWatchlist, { loading: removing }] = useMutation(docs.REMOVE_FROM_WATCHLIST ?? NOOP, refetch);

  if (!docs.ADD_TO_WATCHLIST || !isLoggedIn) return null;

  const busy = adding || setting || removing;
  const existing = (data?.watchlist || []).find(
    it => it.content.__typename === typename && it.content.id === contentId
  );

  const run = async (fn, variables) => {
    try {
      await fn({ variables });
    } catch (err) {
      alert(err.message);
    }
  };

  const btn = 'px-3 py-1.5 rounded-lg text-sm transition-colors disabled:opacity-50';

  if (existing) {
    const watched = existing.status === 'WATCHED';
    return (
      <div className="flex flex-wrap items-center gap-2">
        <span className="inline-flex items-center gap-1.5 text-teal-400 text-sm font-medium">
          ✓ On your watch list · {watched ? 'Watched' : 'Want to watch'}
        </span>
        <button
          disabled={busy}
          onClick={() => run(setWatchStatus, { itemId: existing.id, status: watched ? 'WANT_TO_WATCH' : 'WATCHED' })}
          className={`bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 text-zinc-200 ${btn}`}
        >
          {watched ? 'Mark want to watch' : 'Mark watched'}
        </button>
        <button
          disabled={busy}
          onClick={() => run(removeFromWatchlist, { itemId: existing.id })}
          className={`bg-zinc-800 hover:bg-red-900/40 border border-zinc-700 hover:border-red-800 text-zinc-400 hover:text-red-400 ${btn}`}
        >
          Remove
        </button>
      </div>
    );
  }

  const add = (status) => run(addToWatchlist, {
    subject: movieId ? { movieId } : { tvShowId },
    status,
  });

  return (
    <div className="flex flex-wrap gap-2">
      <button
        disabled={busy}
        onClick={() => add('WANT_TO_WATCH')}
        className={`bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 text-white ${btn}`}
      >
        + Want to watch
      </button>
      <button
        disabled={busy}
        onClick={() => add('WATCHED')}
        className={`bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 text-white ${btn}`}
      >
        ✓ Watched
      </button>
    </div>
  );
}
