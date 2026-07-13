import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@apollo/client/react';
import { useCaps } from '../context/CapabilitiesContext';
import { NOOP } from '../graphql/documents';
import { useAuth } from '../context/AuthContext';
import ReviewSection from '../components/ReviewSection';
import WatchlistButton from '../components/WatchlistButton';
import FeaturePending from '../components/FeaturePending';

const GENRE_COLORS = {
  ACTION: 'text-red-400', COMEDY: 'text-yellow-400',
  CRIME: 'text-orange-400', DRAMA: 'text-teal-400',
  FANTASY: 'text-violet-400', HORROR: 'text-red-300',
  MYSTERY: 'text-indigo-400', ROMANCE: 'text-rose-400', SCIFI: 'text-cyan-400',
  THRILLER: 'text-amber-400', WAR: 'text-stone-400', WESTERN: 'text-yellow-600',
};

export default function MovieDetailPage() {
  const { id } = useParams();
  const { isAdmin } = useAuth();
  const { caps, docs } = useCaps();
  const navigate = useNavigate();

  const { data, loading, error } = useQuery(docs.GET_MOVIE ?? NOOP, {
    variables: { id },
    skip: !docs.GET_MOVIE,
  });
  const [deleteMovie] = useMutation(docs.DELETE_MOVIE ?? NOOP);

  // Before the security class exists there is no login, so everyone may admin.
  const canAdmin = isAdmin || !docs.LOGIN;

  const handleDelete = async () => {
    if (!confirm('Delete this movie?')) return;
    try {
      await deleteMovie({
        variables: { id },
        refetchQueries: docs.GET_MOVIES ? [{ query: docs.GET_MOVIES }] : [],
        update(cache) {
          cache.evict({ id: cache.identify({ __typename: 'Movie', id }) });
          cache.gc();
        },
      });
      navigate('/');
    } catch (err) {
      alert('Failed to delete: ' + err.message);
    }
  };

  if (!docs.GET_MOVIE) return (
    <FeaturePending
      title="No single-movie query yet"
      hint="Add a movie(id) query to the schema to open movie detail pages."
    />
  );

  if (loading) return (
    <div className="max-w-7xl mx-auto px-4 py-16 animate-pulse">
      <div className="flex gap-8">
        <div className="w-72 h-[432px] bg-zinc-800 rounded-xl shrink-0" />
        <div className="flex-1 space-y-4">
          <div className="h-10 bg-zinc-800 rounded w-2/3" />
          <div className="h-4 bg-zinc-800 rounded w-1/4" />
          <div className="h-32 bg-zinc-800 rounded" />
        </div>
      </div>
    </div>
  );

  if (error || !data?.movie) return (
    <div className="max-w-7xl mx-auto px-4 py-16 text-center text-zinc-400">
      <p>Movie not found.</p>
      <Link to="/" className="text-yellow-400 hover:underline mt-4 inline-block">← Back to movies</Link>
    </div>
  );

  const movie = data.movie;
  const genreColor = GENRE_COLORS[movie.genre] || 'text-zinc-400';

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <Link to="/" className="text-zinc-400 hover:text-white text-sm flex items-center gap-1 mb-6 w-fit">
        ← Back to movies
      </Link>

      <div className="flex flex-col md:flex-row gap-8">
        {/* Poster */}
        <div className="shrink-0">
          <div className="w-72 rounded-xl overflow-hidden bg-zinc-800 border border-zinc-700">
            {movie.posterUrl ? (
              <img src={movie.posterUrl} alt={movie.title} className="w-full object-cover" />
            ) : (
              <div className="w-72 h-[432px] flex items-center justify-center text-zinc-600">
                <svg className="w-24 h-24" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M7 4v16M17 4v16M3 8h4m10 0h4M3 12h18M3 16h4m10 0h4M4 20h16a1 1 0 001-1V5a1 1 0 00-1-1H4a1 1 0 00-1 1v14a1 1 0 001 1z" />
                </svg>
              </div>
            )}
          </div>
        </div>

        {/* Details */}
        <div className="flex-1">
          <div className="flex items-start justify-between gap-4">
            <div>
              <h1 className="text-3xl md:text-4xl font-bold text-white">{movie.title}</h1>
              <div className="flex flex-wrap items-center gap-3 mt-2">
                <span className="text-zinc-400">{movie.releaseYear}</span>
                <span className={`font-semibold ${genreColor}`}>{movie.genre}</span>
                {movie.runtime && <span className="text-zinc-400">{movie.runtime} min</span>}
              </div>
            </div>
            {canAdmin && (
              <div className="flex gap-2 shrink-0">
                {docs.UPDATE_MOVIE && (
                  <Link
                    to={`/admin?edit=${movie.id}`}
                    className="bg-zinc-700 hover:bg-zinc-600 text-white px-4 py-2 rounded text-sm transition-colors"
                  >
                    Edit
                  </Link>
                )}
                {docs.DELETE_MOVIE && (
                  <button
                    onClick={handleDelete}
                    className="bg-red-900/40 hover:bg-red-800/50 text-red-400 px-4 py-2 rounded text-sm transition-colors"
                  >
                    Delete
                  </button>
                )}
              </div>
            )}
          </div>

          {/* Rating */}
          {movie.rating && (
            <div className="flex items-center gap-2 mt-4">
              <div className="flex items-center gap-1 bg-zinc-900 border border-zinc-700 rounded-lg px-4 py-2">
                <span className="text-yellow-400 text-2xl font-bold">{movie.rating.toFixed(1)}</span>
                <span className="text-zinc-500 text-sm">/10</span>
                <span className="text-yellow-400 ml-1">★</span>
              </div>
            </div>
          )}

          {/* Watch list */}
          <div className="mt-4">
            <WatchlistButton movieId={movie.id} />
          </div>

          {/* Plot */}
          {movie.plot && (
            <div className="mt-6">
              <h2 className="text-zinc-400 text-sm uppercase tracking-wider font-semibold mb-2">Plot</h2>
              <p className="text-zinc-300 leading-relaxed">{movie.plot}</p>
            </div>
          )}

          {/* Directors */}
          {movie.directors?.length > 0 && (
            <div className="mt-6">
              <h2 className="text-zinc-400 text-sm uppercase tracking-wider font-semibold mb-3">
                {movie.directors.length === 1 ? 'Director' : 'Directors'}
              </h2>
              <div className="flex flex-wrap gap-2">
                {movie.directors.map(d => (
                  <Link key={d.id} to={`/person/${d.id}`}
                    className="bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 rounded-lg px-3 py-2 text-sm text-white transition-colors">
                    <div className="font-medium">{d.name}</div>
                    {d.country && <div className="text-zinc-500 text-xs">{d.country.emoji} {d.country.name}</div>}
                  </Link>
                ))}
              </div>
            </div>
          )}

          {/* Cast */}
          {movie.cast?.length > 0 && (
            <div className="mt-6">
              <h2 className="text-zinc-400 text-sm uppercase tracking-wider font-semibold mb-3">Cast</h2>
              <div className="flex flex-wrap gap-2">
                {movie.cast.map(c => (
                  <Link key={c.id} to={`/person/${c.person.id}`}
                    className="bg-zinc-800 hover:bg-zinc-700 border border-zinc-700 rounded-lg px-3 py-2 text-sm text-white transition-colors">
                    <div className="font-medium">{c.person.name}</div>
                    {c.characterName && <div className="text-zinc-500 text-xs italic">{c.characterName}</div>}
                  </Link>
                ))}
              </div>
            </div>
          )}

          {movie.tmdbId && (
            <div className="mt-6">
              <a
                href={`https://www.themoviedb.org/movie/${movie.tmdbId}`}
                target="_blank"
                rel="noopener noreferrer"
                className="text-zinc-500 hover:text-zinc-300 text-sm flex items-center gap-1 w-fit"
              >
                View on TMDB →
              </a>
            </div>
          )}
        </div>
      </div>

      {caps.field('Movie', 'reviews') && (
        <ReviewSection
          reviews={movie.reviews || []}
          movieId={movie.id}
          refetchQuery={{ query: docs.GET_MOVIE, variables: { id } }}
        />
      )}
    </div>
  );
}
