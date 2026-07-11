import { useState } from 'react';
import { useQuery, useLazyQuery } from '@apollo/client/react';
import { Link } from 'react-router-dom';
import { useCaps } from '../context/CapabilitiesContext';
import { NOOP } from '../graphql/documents';
import MovieCard from '../components/MovieCard';
import Pagination from '../components/Pagination';
import FeaturePending from '../components/FeaturePending';

const GENRES = ['ACTION','CRIME','DRAMA','FANTASY',
  'HORROR','MYSTERY','ROMANCE','SCIFI','THRILLER','WAR','WESTERN'];

const SORT_OPTIONS = [
  { label: 'Newest First', field: 'RELEASE_YEAR', order: 'DESC' },
  { label: 'Oldest First', field: 'RELEASE_YEAR', order: 'ASC' },
  { label: 'Highest Rated', field: 'RATING', order: 'DESC' },
  { label: 'Title A-Z', field: 'TITLE', order: 'ASC' },
];

const PAGE_SIZE = 20;

export default function HomePage() {
  const { caps, docs } = useCaps();
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [selectedGenre, setSelectedGenre] = useState('');
  const [sortIdx, setSortIdx] = useState(0);
  const [minRating, setMinRating] = useState('');

  const sort = SORT_OPTIONS[sortIdx];

  // Which movies-query features exist right now?
  const canFilter = caps.arg('query', 'movies', 'filter');
  const canSort = caps.arg('query', 'movies', 'sort');
  const canPaginate = caps.arg('query', 'movies', 'page');

  const filter = {
    ...(selectedGenre && { genre: selectedGenre }),
    ...(minRating && { minRating: parseFloat(minRating) }),
  };

  // Only send variables the schema declares
  const variables = {
    ...(canFilter && { filter: Object.keys(filter).length > 0 ? filter : null }),
    ...(canPaginate && { page: page - 1, size: PAGE_SIZE }),
    ...(canSort && { sort: { field: sort.field, order: sort.order } }),
  };

  // Movie grid query (when not searching)
  const { data, loading, error } = useQuery(docs.GET_MOVIES ?? NOOP, {
    variables,
    skip: !!search || !docs.GET_MOVIES,
  });

  // Global search query (movies + TV shows + people)
  const [runSearch, { data: searchData, loading: searchLoading }] = useLazyQuery(docs.GLOBAL_SEARCH ?? NOOP);

  const handleSearch = (e) => {
    e.preventDefault();
    if (!searchInput.trim()) {
      setSearch('');
      return;
    }
    setSearch(searchInput.trim());
    runSearch({ variables: { query: searchInput.trim() } });
  };

  const clearSearch = () => {
    setSearch('');
    setSearchInput('');
    setPage(1);
  };

  const handleGenreChange = (genre) => {
    setSelectedGenre(genre === selectedGenre ? '' : genre);
    setPage(1);
  };

  // The movies query is a page object once pagination exists, a plain list before
  const rawMovies = data?.movies;
  const movies = Array.isArray(rawMovies) ? rawMovies : rawMovies?.content || [];
  const totalPages = rawMovies?.totalPages || 0;
  const totalElements = rawMovies?.totalElements || 0;

  // The union search returns movies and people in one list, split here by
  // __typename; the fallback fields cover schemas without the union.
  const searchResults = searchData?.search || [];
  const searchMovies = searchResults
    .filter(r => r.__typename === 'Movie')
    .concat(searchData?.searchMovies || []);
  const searchPeople = searchResults
    .filter(r => r.__typename === 'Person')
    .concat(searchData?.searchPeople || []);
  const hasSearchResults = searchMovies.length > 0 || searchPeople.length > 0;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Hero */}
      <div className="text-center mb-10">
        <h1 className="text-4xl md:text-5xl font-bold text-white mb-3">
          Discover <span className="text-yellow-400">Great Movies</span>
        </h1>
        <p className="text-zinc-400 text-lg">Search movies and people</p>
      </div>

      {/* Search (only once a search query exists in the schema) */}
      {docs.GLOBAL_SEARCH && (
        <form onSubmit={handleSearch} className="flex gap-2 mb-6 max-w-xl mx-auto">
          <input
            type="text"
            value={searchInput}
            onChange={e => setSearchInput(e.target.value)}
            placeholder="Search movies and people..."
            className="flex-1 bg-zinc-800 border border-zinc-700 rounded-lg px-4 py-2.5 text-white placeholder-zinc-500 focus:outline-none focus:border-yellow-500 transition-colors"
          />
          <button
            type="submit"
            className="bg-yellow-500 hover:bg-yellow-400 text-black font-semibold px-6 py-2.5 rounded-lg transition-colors"
          >
            Search
          </button>
          {search && (
            <button type="button" onClick={clearSearch}
              className="bg-zinc-700 hover:bg-zinc-600 text-white px-4 py-2.5 rounded-lg transition-colors">
              ✕
            </button>
          )}
        </form>
      )}

      {/* Search Results Mode */}
      {search && (
        <>
          {searchLoading && (
            <div className="text-center py-16 text-zinc-400">Searching...</div>
          )}

          {!searchLoading && !hasSearchResults && (
            <div className="text-center py-16 text-zinc-500">
              <p className="text-2xl mb-2">🔍</p>
              <p>No results found for "{search}"</p>
            </div>
          )}

          {!searchLoading && hasSearchResults && (
            <div className="space-y-10">
              {/* Movies */}
              {searchMovies.length > 0 && (
                <section>
                  <h2 className="text-xl font-bold text-white mb-4">
                    Movies <span className="text-zinc-500 font-normal text-base">({searchMovies.length})</span>
                  </h2>
                  <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
                    {searchMovies.map(movie => (
                      <Link key={movie.id} to={`/movie/${movie.id}`}
                        className="bg-zinc-900 rounded-xl overflow-hidden border border-zinc-800 hover:border-zinc-600 transition-colors group">
                        <div className="aspect-[2/3] bg-zinc-800 relative overflow-hidden">
                          {movie.posterUrl ? (
                            <img src={movie.posterUrl} alt={movie.title}
                              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                              onError={e => { e.target.style.display = 'none'; }} />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center text-zinc-600 text-4xl">🎬</div>
                          )}
                          {movie.rating && (
                            <div className="absolute top-2 right-2 bg-black/70 text-yellow-400 text-xs font-bold px-2 py-1 rounded">
                              ★ {movie.rating.toFixed(1)}
                            </div>
                          )}
                        </div>
                        <div className="p-3">
                          <h3 className="text-white text-sm font-medium leading-tight line-clamp-2">{movie.title}</h3>
                          <p className="text-zinc-500 text-xs mt-1">{movie.releaseYear} · {movie.genre}</p>
                        </div>
                      </Link>
                    ))}
                  </div>
                </section>
              )}

              {/* People */}
              {searchPeople.length > 0 && (
                <section>
                  <h2 className="text-xl font-bold text-white mb-4">
                    People <span className="text-zinc-500 font-normal text-base">({searchPeople.length})</span>
                  </h2>
                  <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
                    {searchPeople.map(person => (
                      <Link key={person.id} to={`/person/${person.id}`}
                        className="bg-zinc-900 rounded-xl overflow-hidden border border-zinc-800 hover:border-zinc-600 transition-colors group">
                        <div className="aspect-square bg-zinc-800 overflow-hidden">
                          {person.photoUrl ? (
                            <img src={person.photoUrl} alt={person.name}
                              className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                              onError={e => { e.target.style.display = 'none'; }} />
                          ) : (
                            <div className="w-full h-full flex items-center justify-center text-zinc-600 text-4xl">👤</div>
                          )}
                        </div>
                        <div className="p-3">
                          <h3 className="text-white text-sm font-medium leading-tight">{person.name}</h3>
                          {person.country && <p className="text-zinc-500 text-xs mt-1">{person.country.emoji} {person.country.name}</p>}
                        </div>
                      </Link>
                    ))}
                  </div>
                </section>
              )}
            </div>
          )}
        </>
      )}

      {/* Browse Mode (no search) */}
      {!search && !docs.GET_MOVIES && (
        <FeaturePending
          title="No movies query yet"
          hint="The schema has no way to list movies so far. Add a query that returns movies (and the Movie type) to light this page up."
        />
      )}

      {!search && docs.GET_MOVIES && (
        <>
          {/* Filters (appear once the movies query accepts them) */}
          {canFilter && (
            <div className="flex flex-wrap gap-2 mb-4 items-center">
              <span className="text-zinc-500 text-sm">Genre:</span>
              {GENRES.map(g => (
                <button
                  key={g}
                  onClick={() => handleGenreChange(g)}
                  className={`text-xs px-3 py-1.5 rounded-full transition-colors font-medium ${
                    selectedGenre === g
                      ? 'bg-yellow-500 text-black'
                      : 'bg-zinc-800 text-zinc-400 hover:bg-zinc-700'
                  }`}
                >
                  {g}
                </button>
              ))}
            </div>
          )}

          {(canFilter || canSort) && (
            <div className="flex flex-wrap gap-4 mb-6 items-center">
              {canFilter && (
                <div className="flex items-center gap-2">
                  <label className="text-zinc-500 text-sm">Min rating:</label>
                  <select
                    value={minRating}
                    onChange={e => { setMinRating(e.target.value); setPage(1); }}
                    className="bg-zinc-800 border border-zinc-700 text-zinc-300 text-sm rounded px-2 py-1 focus:outline-none focus:border-yellow-500"
                  >
                    <option value="">Any</option>
                    {[5,6,7,7.5,8,8.5,9].map(r => <option key={r} value={r}>{r}+</option>)}
                  </select>
                </div>
              )}
              {canSort && (
                <div className="flex items-center gap-2">
                  <label className="text-zinc-500 text-sm">Sort:</label>
                  <select
                    value={sortIdx}
                    onChange={e => { setSortIdx(Number(e.target.value)); setPage(1); }}
                    className="bg-zinc-800 border border-zinc-700 text-zinc-300 text-sm rounded px-2 py-1 focus:outline-none focus:border-yellow-500"
                  >
                    {SORT_OPTIONS.map((opt, i) => <option key={i} value={i}>{opt.label}</option>)}
                  </select>
                </div>
              )}
              {totalElements > 0 && (
                <span className="text-zinc-500 text-sm ml-auto">{totalElements} movies</span>
              )}
            </div>
          )}

          {/* Results */}
          {loading && (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
              {Array.from({ length: PAGE_SIZE }).map((_, i) => (
                <div key={i} className="bg-zinc-900 rounded-xl overflow-hidden border border-zinc-800 animate-pulse">
                  <div className="aspect-[2/3] bg-zinc-800" />
                  <div className="p-3 space-y-2">
                    <div className="h-3 bg-zinc-800 rounded w-3/4" />
                    <div className="h-3 bg-zinc-800 rounded w-1/2" />
                  </div>
                </div>
              ))}
            </div>
          )}

          {error && (
            <div className="text-center py-16 text-zinc-400">
              <p className="text-red-400">Error loading movies. Is the backend running?</p>
              <p className="text-sm mt-2">{error.message}</p>
            </div>
          )}

          {!loading && !error && movies.length === 0 && (
            <div className="text-center py-16 text-zinc-500">
              <p className="text-2xl mb-2">🎬</p>
              <p>No movies found. Try a different filter.</p>
            </div>
          )}

          {!loading && movies.length > 0 && (
            <>
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
                {movies.map(movie => <MovieCard key={movie.id} movie={movie} />)}
              </div>
              {canPaginate && (
                <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
              )}
            </>
          )}
        </>
      )}
    </div>
  );
}
