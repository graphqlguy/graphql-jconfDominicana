import { Link } from 'react-router-dom';

const GENRE_COLORS = {
  ACTION: 'bg-red-500/20 text-red-400',
  COMEDY: 'bg-yellow-500/20 text-yellow-400',
  CRIME: 'bg-orange-500/20 text-orange-400',
  DRAMA: 'bg-teal-500/20 text-teal-400',
  FANTASY: 'bg-violet-500/20 text-violet-400',
  HORROR: 'bg-red-900/40 text-red-300',
  MYSTERY: 'bg-indigo-500/20 text-indigo-400',
  ROMANCE: 'bg-rose-500/20 text-rose-400',
  SCIFI: 'bg-cyan-500/20 text-cyan-400',
  THRILLER: 'bg-amber-500/20 text-amber-400',
  WAR: 'bg-stone-500/20 text-stone-400',
  WESTERN: 'bg-yellow-700/20 text-yellow-600',
};

export default function MovieCard({ movie }) {
  const genreColor = GENRE_COLORS[movie.genre] || 'bg-zinc-500/20 text-zinc-400';

  return (
    <Link to={`/movie/${movie.id}`} className="group block bg-zinc-900 rounded-xl overflow-hidden border border-zinc-800 hover:border-zinc-600 transition-all duration-200 hover:shadow-xl hover:shadow-black/40 hover:-translate-y-0.5">
      <div className="relative aspect-[2/3] overflow-hidden bg-zinc-800">
        {movie.posterUrl ? (
          <img
            src={movie.posterUrl}
            alt={movie.title}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            loading="lazy"
            onError={(e) => {
              e.target.onerror = null;
              e.target.style.display = 'none';
              e.target.nextSibling.style.display = 'flex';
            }}
          />
        ) : null}
        <div className="w-full h-full flex items-center justify-center text-zinc-600"
          style={{ display: movie.posterUrl ? 'none' : 'flex' }}>
          <svg className="w-16 h-16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M7 4v16M17 4v16M3 8h4m10 0h4M3 12h18M3 16h4m10 0h4M4 20h16a1 1 0 001-1V5a1 1 0 00-1-1H4a1 1 0 00-1 1v14a1 1 0 001 1z" />
          </svg>
        </div>
        {movie.rating && (
          <div className="absolute top-2 right-2 bg-black/70 text-yellow-400 text-xs font-bold px-2 py-1 rounded flex items-center gap-1">
            ★ {movie.rating.toFixed(1)}
          </div>
        )}
        {movie.communityRating && (
          <div className="absolute bottom-2 right-2 bg-black/70 text-teal-400 text-xs font-bold px-2 py-1 rounded flex items-center gap-1"
               title="TMDB Community Rating">
            TMDB {movie.communityRating.voteAverage.toFixed(1)}
            <span className="text-teal-500/70 font-normal">
              ({movie.communityRating.voteCount >= 1000
                ? (movie.communityRating.voteCount / 1000).toFixed(1) + 'K'
                : movie.communityRating.voteCount})
            </span>
          </div>
        )}
      </div>
      <div className="p-3">
        <h3 className="text-white font-semibold text-sm leading-tight line-clamp-2 group-hover:text-yellow-400 transition-colors">
          {movie.title}
        </h3>
        <div className="flex items-center justify-between mt-2">
          <span className="text-zinc-500 text-xs">{movie.releaseYear}</span>
          <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${genreColor}`}>
            {movie.genre}
          </span>
        </div>
        <div className="flex items-center justify-between mt-1">
          {movie.directors?.length > 0 && (
            <p className="text-zinc-500 text-xs truncate flex-1">
              {movie.directors.map(d => d.name).join(', ')}
            </p>
          )}
          {movie.reviewCount > 0 && (
            <span className="text-zinc-500 text-xs ml-2 shrink-0" title={`${movie.reviewCount} reviews`}>
              💬 {movie.reviewCount}
            </span>
          )}
        </div>
      </div>
    </Link>
  );
}
