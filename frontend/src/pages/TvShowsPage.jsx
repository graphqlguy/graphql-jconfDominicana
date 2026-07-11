import { useState } from 'react';
import { useQuery } from '@apollo/client/react';
import { Link } from 'react-router-dom';
import { useCaps } from '../context/CapabilitiesContext';
import { NOOP } from '../graphql/documents';
import Pagination from '../components/Pagination';
import FeaturePending from '../components/FeaturePending';

const PAGE_SIZE = 20;

const GENRE_COLORS = {
  ACTION: 'bg-red-900/40 text-red-300',
  COMEDY: 'bg-yellow-900/40 text-yellow-300',
  CRIME: 'bg-orange-900/40 text-orange-300',
  DRAMA: 'bg-indigo-900/40 text-indigo-300',
  FANTASY: 'bg-emerald-900/40 text-emerald-300',
  HORROR: 'bg-rose-900/40 text-rose-300',
  MYSTERY: 'bg-violet-900/40 text-violet-300',
  ROMANCE: 'bg-pink-900/40 text-pink-300',
  SCIFI: 'bg-cyan-900/40 text-cyan-300',
  THRILLER: 'bg-amber-900/40 text-amber-300',
  WAR: 'bg-stone-900/40 text-stone-300',
  WESTERN: 'bg-yellow-900/40 text-yellow-300',
};

export default function TvShowsPage() {
  const { docs } = useCaps();
  const [page, setPage] = useState(1);

  const { data, loading, error } = useQuery(docs.GET_TV_SHOWS ?? NOOP, {
    variables: { page: page - 1, size: PAGE_SIZE },
    skip: !docs.GET_TV_SHOWS,
  });

  if (!docs.GET_TV_SHOWS) return (
    <FeaturePending
      title="No TV shows query yet"
      hint="Add the TvShow type and a paginated tvShows query to the schema to browse shows here."
    />
  );

  const shows = data?.tvShows?.content || [];
  const totalPages = data?.tvShows?.totalPages || 0;
  const totalElements = data?.tvShows?.totalElements || 0;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-white mb-2">TV Shows</h1>
        {totalElements > 0 && (
          <p className="text-zinc-400">{totalElements} shows</p>
        )}
      </div>

      {loading && (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {Array.from({ length: PAGE_SIZE }).map((_, i) => (
            <div key={i} className="bg-zinc-900 rounded-xl border border-zinc-800 animate-pulse">
              <div className="aspect-[2/3] bg-zinc-800 rounded-t-xl" />
              <div className="p-3 space-y-2">
                <div className="h-3 bg-zinc-800 rounded w-3/4" />
                <div className="h-3 bg-zinc-800 rounded w-1/2" />
              </div>
            </div>
          ))}
        </div>
      )}

      {error && (
        <div className="text-center py-16 text-red-400">
          Error loading TV shows: {error.message}
        </div>
      )}

      {!loading && !error && (
        <>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
            {shows.map(show => (
              <Link key={show.id} to={`/tvshow/${show.id}`}
                className="bg-zinc-900 border border-zinc-800 rounded-xl overflow-hidden hover:border-zinc-600 transition-colors group">
                <div className="aspect-[2/3] bg-zinc-800 relative overflow-hidden">
                  {show.posterUrl ? (
                    <img src={show.posterUrl} alt={show.title}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-zinc-600">
                      <svg className="w-16 h-16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1}
                          d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                      </svg>
                    </div>
                  )}
                  {show.rating && (
                    <div className="absolute top-2 right-2 bg-black/70 backdrop-blur-sm text-yellow-400 text-xs font-bold px-1.5 py-0.5 rounded flex items-center gap-1">
                      ★ {show.rating.toFixed(1)}
                    </div>
                  )}
                </div>
                <div className="p-3">
                  <div className="text-white font-medium text-sm leading-snug mb-1">{show.title}</div>
                  <div className="flex items-center justify-between">
                    <span className="text-zinc-500 text-xs">
                      {show.startYear}{show.endYear ? `-${show.endYear}` : '-'}
                    </span>
                    {show.seasons && (
                      <span className="text-zinc-600 text-xs">{show.seasons}S</span>
                    )}
                  </div>
                  <div className={`inline-block text-xs px-1.5 py-0.5 rounded mt-1 ${GENRE_COLORS[show.genre] || 'bg-zinc-800 text-zinc-400'}`}>
                    {show.genre}
                  </div>
                </div>
              </Link>
            ))}
          </div>
          <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
