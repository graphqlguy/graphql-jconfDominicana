import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@apollo/client/react';
import { useCaps } from '../context/CapabilitiesContext';
import { NOOP } from '../graphql/documents';
import ReviewSection from '../components/ReviewSection';
import FeaturePending from '../components/FeaturePending';

export default function TvShowDetailPage() {
  const { id } = useParams();
  const { caps, docs } = useCaps();
  const { data, loading, error } = useQuery(docs.GET_TV_SHOW ?? NOOP, {
    variables: { id },
    skip: !docs.GET_TV_SHOW,
  });
  const [openSeason, setOpenSeason] = useState(1);

  if (!docs.GET_TV_SHOW) return (
    <FeaturePending
      title="No single-show query yet"
      hint="Add a tvShow(id) query to the schema to open TV show pages."
    />
  );

  if (loading) return (
    <div className="max-w-7xl mx-auto px-4 py-16 animate-pulse">
      <div className="flex gap-8">
        <div className="w-56 aspect-[2/3] bg-zinc-800 rounded-xl shrink-0" />
        <div className="flex-1 space-y-4">
          <div className="h-8 bg-zinc-800 rounded w-1/3" />
          <div className="h-4 bg-zinc-800 rounded w-1/4" />
          <div className="h-20 bg-zinc-800 rounded" />
        </div>
      </div>
    </div>
  );

  if (error || !data?.tvShow) return (
    <div className="max-w-7xl mx-auto px-4 py-16 text-center text-zinc-400">
      <p>TV show not found.</p>
      <Link to="/tvshows" className="text-yellow-400 hover:underline mt-4 inline-block">← Back to TV shows</Link>
    </div>
  );

  const show = data.tvShow;
  const yearRange = show.endYear ? `${show.startYear}-${show.endYear}` : `${show.startYear}-present`;

  // Group episodes by season
  const seasonMap = {};
  (show.episodes || []).forEach(ep => {
    if (!seasonMap[ep.seasonNumber]) seasonMap[ep.seasonNumber] = [];
    seasonMap[ep.seasonNumber].push(ep);
  });
  const seasonNumbers = Object.keys(seasonMap).map(Number).sort((a, b) => a - b);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <Link to="/tvshows" className="text-zinc-400 hover:text-white text-sm flex items-center gap-1 mb-6 w-fit">
        ← Back to TV shows
      </Link>

      {/* Hero */}
      <div className="flex flex-col sm:flex-row gap-8 mb-10">
        <div className="shrink-0">
          <div className="w-56 rounded-xl overflow-hidden bg-zinc-800 border border-zinc-700">
            {show.posterUrl ? (
              <img src={show.posterUrl} alt={show.title} className="w-full object-cover"
                onError={e => { e.target.style.display = 'none'; }} />
            ) : (
              <div className="aspect-[2/3] flex items-center justify-center text-zinc-600">
                <svg className="w-20 h-20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1}
                    d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
              </div>
            )}
          </div>
        </div>

        <div className="flex-1">
          <h1 className="text-3xl md:text-4xl font-bold text-white mb-2">{show.title}</h1>
          <div className="flex flex-wrap gap-3 text-zinc-400 text-sm mb-4">
            <span>{yearRange}</span>
            {show.seasons && <span>· {show.seasons} seasons</span>}
            <span>· {show.genre}</span>
          </div>
          {show.rating && (
            <div className="flex items-center gap-2 mb-4">
              <span className="text-yellow-400 text-xl">★</span>
              <span className="text-white text-xl font-bold">{show.rating.toFixed(1)}</span>
              <span className="text-zinc-500 text-sm">/ 10</span>
            </div>
          )}
          {show.plot && (
            <p className="text-zinc-300 leading-relaxed max-w-2xl">{show.plot}</p>
          )}
        </div>
      </div>

      {/* Creators */}
      {show.creators?.length > 0 && (
        <div className="mb-10">
          <h2 className="text-xl font-bold text-white mb-4">Created by</h2>
          <div className="flex flex-wrap gap-4">
            {show.creators.map(creator => (
              <Link key={creator.id} to={`/person/${creator.id}`}
                className="flex items-center gap-3 bg-zinc-900 border border-zinc-800 rounded-xl p-3 hover:border-zinc-600 transition-colors">
                <div className="w-12 h-12 rounded-full overflow-hidden bg-zinc-800 shrink-0">
                  {creator.photoUrl ? (
                    <img src={creator.photoUrl} alt={creator.name} className="w-full h-full object-cover"
                      onError={e => { e.target.style.display = 'none'; }} />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-zinc-600">
                      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                          d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                      </svg>
                    </div>
                  )}
                </div>
                <div>
                  <div className="text-white text-sm font-medium">{creator.name}</div>
                  {creator.country && <div className="text-zinc-500 text-xs">{creator.country.emoji} {creator.country.name}</div>}
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}

      {/* Episodes by season */}
      {seasonNumbers.length > 0 && (
        <div className="mb-10">
          <h2 className="text-xl font-bold text-white mb-4">
            Episodes <span className="text-zinc-500 font-normal text-base">({show.episodes.length} total)</span>
          </h2>
          <div className="space-y-2">
            {seasonNumbers.map(season => {
              const episodes = seasonMap[season];
              const isOpen = openSeason === season;
              return (
                <div key={season} className="border border-zinc-800 rounded-xl overflow-hidden">
                  {/* Season header */}
                  <button
                    onClick={() => setOpenSeason(isOpen ? null : season)}
                    className="w-full flex items-center justify-between px-5 py-4 bg-zinc-900 hover:bg-zinc-800 transition-colors text-left"
                  >
                    <div className="flex items-center gap-3">
                      <span className="text-white font-semibold">Season {season}</span>
                      <span className="text-zinc-500 text-sm">{episodes.length} episodes</span>
                      {episodes[0]?.airYear && (
                        <span className="text-zinc-600 text-sm">· {episodes[0].airYear}</span>
                      )}
                    </div>
                    <svg
                      className={`w-5 h-5 text-zinc-400 transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`}
                      fill="none" stroke="currentColor" viewBox="0 0 24 24"
                    >
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </button>

                  {/* Episode list */}
                  {isOpen && (
                    <div className="divide-y divide-zinc-800/60">
                      {episodes.map(ep => (
                        <div key={ep.id} className="flex items-start gap-4 px-5 py-4 bg-zinc-950 hover:bg-zinc-900/60 transition-colors">
                          <div className="shrink-0 w-10 text-center">
                            <span className="text-zinc-500 text-sm font-mono">
                              {String(ep.episodeNumber).padStart(2, '0')}
                            </span>
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="text-white font-medium text-sm">{ep.title}</div>
                            {ep.overview && (
                              <div className="text-zinc-500 text-xs mt-1 leading-relaxed line-clamp-2">{ep.overview}</div>
                            )}
                          </div>
                          <div className="shrink-0 text-right space-y-1">
                            {ep.airYear && <div className="text-zinc-600 text-xs">{ep.airYear}</div>}
                            {ep.runtime && <div className="text-zinc-600 text-xs">{ep.runtime}m</div>}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {caps.field('TvShow', 'reviews') && (
        <ReviewSection
          reviews={show.reviews || []}
          tvShowId={show.id}
          refetchQuery={{ query: docs.GET_TV_SHOW, variables: { id } }}
        />
      )}

      {/* Cast */}
      {show.cast?.length > 0 && (
        <div>
          <h2 className="text-xl font-bold text-white mb-4">
            Cast <span className="text-zinc-500 font-normal text-base">({show.cast.length})</span>
          </h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
            {show.cast.map(c => (
              <Link key={c.id} to={`/person/${c.person.id}`}
                className="bg-zinc-900 border border-zinc-800 rounded-xl overflow-hidden hover:border-zinc-600 transition-colors group">
                <div className="aspect-square bg-zinc-800 flex items-center justify-center overflow-hidden">
                  {c.person.photoUrl ? (
                    <img src={c.person.photoUrl} alt={c.person.name}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                      onError={e => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'flex'; }}
                    />
                  ) : null}
                  <div className={`w-full h-full flex items-center justify-center text-zinc-600 ${c.person.photoUrl ? 'hidden' : 'flex'}`}>
                    <svg className="w-12 h-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1}
                        d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                  </div>
                </div>
                <div className="p-3">
                  <div className="text-white font-medium text-sm leading-snug">{c.person.name}</div>
                  {c.characterName && <div className="text-zinc-500 text-xs mt-1 italic">{c.characterName}</div>}
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
