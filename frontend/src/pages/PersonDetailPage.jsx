import { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@apollo/client/react';
import { useCaps } from '../context/CapabilitiesContext';
import { useAuth } from '../context/AuthContext';
import { NOOP } from '../graphql/documents';
import MovieCard from '../components/MovieCard';
import FeaturePending from '../components/FeaturePending';

export default function PersonDetailPage() {
  const { id } = useParams();
  const { docs } = useCaps();
  const { isAdmin } = useAuth();
  const navigate = useNavigate();
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({ name: '', birthYear: '' });
  const [saveError, setSaveError] = useState('');

  const { data, loading, error } = useQuery(docs.GET_PERSON ?? NOOP, {
    variables: { id },
    skip: !docs.GET_PERSON,
  });
  const [updatePerson, { loading: saving }] = useMutation(docs.UPDATE_PERSON ?? NOOP, {
    refetchQueries: docs.GET_PERSON ? [{ query: docs.GET_PERSON, variables: { id } }] : [],
  });
  const [deletePerson] = useMutation(docs.DELETE_PERSON ?? NOOP);

  // Before the security class exists there is no login, so everyone may admin.
  const canAdmin = isAdmin || !docs.LOGIN;

  const startEditing = (person) => {
    setForm({ name: person.name, birthYear: person.birthYear ?? '' });
    setSaveError('');
    setEditing(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaveError('');
    try {
      await updatePerson({
        variables: {
          input: {
            id,
            name: form.name,
            birthYear: form.birthYear === '' ? null : parseInt(form.birthYear),
          },
        },
      });
      setEditing(false);
    } catch (err) {
      setSaveError(err.message);
    }
  };

  const handleDelete = async () => {
    if (!confirm('Delete this person?')) return;
    // Same cleanup as deleting a movie: refetch the list and evict the cached
    // entity, so the people page never shows the deleted person from cache.
    const cleanup = {
      // by operation name: refetches the people page's active query, whatever its variables
      refetchQueries: ['GetPeople'],
      update(cache) {
        cache.evict({ id: cache.identify({ __typename: 'Person', id }) });
        cache.gc();
      },
    };
    try {
      const { data: res } = await deletePerson({ variables: { id, force: false }, ...cleanup });
      let result = res.deletePerson;
      if (!result.success && result.error) {
        const linked = result.error === 'LINKED_TO_MOVIE' ? 'movies' : 'TV shows';
        if (!confirm(`This person is linked to ${linked}. Delete anyway and unlink all credits?`)) return;
        const { data: forced } = await deletePerson({ variables: { id, force: true }, ...cleanup });
        result = forced.deletePerson;
      }
      if (result.success) navigate('/people');
    } catch (err) {
      alert('Failed to delete: ' + err.message);
    }
  };

  if (!docs.GET_PERSON) return (
    <FeaturePending
      title="No single-person query yet"
      hint="Add a person(id) query to the schema to open person profiles."
    />
  );

  if (loading) return (
    <div className="max-w-7xl mx-auto px-4 py-16 animate-pulse">
      <div className="flex gap-8">
        <div className="w-48 h-48 bg-zinc-800 rounded-xl shrink-0" />
        <div className="flex-1 space-y-4">
          <div className="h-8 bg-zinc-800 rounded w-1/3" />
          <div className="h-4 bg-zinc-800 rounded w-1/4" />
          <div className="h-20 bg-zinc-800 rounded" />
        </div>
      </div>
    </div>
  );

  if (error || !data?.person) return (
    <div className="max-w-7xl mx-auto px-4 py-16 text-center text-zinc-400">
      <p>Person not found.</p>
      <Link to="/people" className="text-yellow-400 hover:underline mt-4 inline-block">← Back to people</Link>
    </div>
  );

  const person = data.person;
  const directedMovies = person.directedMovies || [];
  const castCredits = person.movieCastCredits || [];
  const createdShows = person.createdShows || [];
  const tvCastCredits = person.tvShowCastCredits || [];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <Link to="/people" className="text-zinc-400 hover:text-white text-sm flex items-center gap-1 mb-6 w-fit">
        ← Back to people
      </Link>

      <div className="flex flex-col sm:flex-row gap-8 mb-10">
        <div className="shrink-0">
          <div className="w-48 h-48 rounded-xl overflow-hidden bg-zinc-800 border border-zinc-700 flex items-center justify-center">
            {person.photoUrl ? (
              <img src={person.photoUrl} alt={person.name} className="w-full h-full object-cover"
                onError={e => { e.target.style.display = 'none'; }} />
            ) : (
              <svg className="w-20 h-20 text-zinc-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1}
                  d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
            )}
          </div>
        </div>

        <div className="flex-1">
          <div className="flex items-start justify-between gap-4">
            <h1 className="text-3xl md:text-4xl font-bold text-white mb-2">{person.name}</h1>
            {canAdmin && (docs.UPDATE_PERSON || docs.DELETE_PERSON) && (
              <div className="flex gap-2 shrink-0">
                {docs.UPDATE_PERSON && (
                  <button
                    onClick={() => (editing ? setEditing(false) : startEditing(person))}
                    className="bg-zinc-700 hover:bg-zinc-600 text-white px-4 py-2 rounded text-sm transition-colors"
                  >
                    {editing ? 'Cancel' : 'Edit'}
                  </button>
                )}
                {docs.DELETE_PERSON && (
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
          <div className="flex flex-wrap gap-3 text-zinc-400 text-sm mb-4">
            {person.birthYear && <span>Born {person.birthYear}</span>}
            {person.country && <span>· {person.country.emoji} {person.country.name}</span>}
          </div>
          {person.biography && (
            <p className="text-zinc-300 leading-relaxed max-w-2xl">{person.biography}</p>
          )}

          {editing && (
            <form onSubmit={handleSave} className="bg-zinc-900 border border-zinc-800 rounded-xl p-5 mt-4 max-w-md space-y-4">
              <div>
                <label className="block text-zinc-400 text-sm mb-1.5">Name *</label>
                <input
                  type="text"
                  value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  required
                  className="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:border-yellow-500"
                />
              </div>
              <div>
                <label className="block text-zinc-400 text-sm mb-1.5">Birth year</label>
                <input
                  type="number"
                  value={form.birthYear}
                  onChange={e => setForm(f => ({ ...f, birthYear: e.target.value }))}
                  min="1850"
                  className="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-4 py-2.5 text-white focus:outline-none focus:border-yellow-500"
                />
              </div>
              {saveError && <p className="text-red-400 text-sm">{saveError}</p>}
              <button
                type="submit"
                disabled={saving}
                className="bg-yellow-500 hover:bg-yellow-400 disabled:opacity-50 text-black font-semibold px-5 py-2 rounded-lg text-sm transition-colors"
              >
                {saving ? 'Saving...' : 'Save'}
              </button>
            </form>
          )}
        </div>
      </div>

      {directedMovies.length > 0 && (
        <div className="mb-10">
          <h2 className="text-xl font-bold text-white mb-4">
            Directed <span className="text-zinc-500 font-normal text-base">({directedMovies.length} films)</span>
          </h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
            {directedMovies.slice().sort((a, b) => b.releaseYear - a.releaseYear).map(movie => (
              <MovieCard key={movie.id} movie={movie} />
            ))}
          </div>
        </div>
      )}

      {castCredits.length > 0 && (
        <div className="mb-10">
          <h2 className="text-xl font-bold text-white mb-4">
            Acting <span className="text-zinc-500 font-normal text-base">({castCredits.length} films)</span>
          </h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
            {castCredits.slice().sort((a, b) => b.movie.releaseYear - a.movie.releaseYear).map(credit => (
              <div key={credit.id} className="relative">
                <MovieCard movie={credit.movie} />
                {credit.characterName && (
                  <div className="mt-1 px-1 text-xs text-zinc-500 italic truncate">{credit.characterName}</div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {createdShows.length > 0 && (
        <div className="mb-10">
          <h2 className="text-xl font-bold text-white mb-4">
            Created Shows <span className="text-zinc-500 font-normal text-base">({createdShows.length})</span>
          </h2>
          <div className="flex flex-wrap gap-3">
            {createdShows.map(show => (
              <Link key={show.id} to={`/tvshow/${show.id}`}
                className="bg-zinc-900 border border-zinc-800 hover:border-zinc-600 rounded-lg px-4 py-2 transition-colors">
                <div className="text-white text-sm font-medium">{show.title}</div>
                <div className="text-zinc-500 text-xs">{show.startYear}{show.endYear ? `-${show.endYear}` : '-present'}</div>
              </Link>
            ))}
          </div>
        </div>
      )}

      {tvCastCredits.length > 0 && (
        <div className="mb-10">
          <h2 className="text-xl font-bold text-white mb-4">
            TV Roles <span className="text-zinc-500 font-normal text-base">({tvCastCredits.length})</span>
          </h2>
          <div className="flex flex-wrap gap-3">
            {tvCastCredits.map(credit => (
              <Link key={credit.id} to={`/tvshow/${credit.tvShow.id}`}
                className="bg-zinc-900 border border-zinc-800 hover:border-zinc-600 rounded-lg px-4 py-2 transition-colors">
                <div className="text-white text-sm font-medium">{credit.tvShow.title}</div>
                {credit.characterName && (
                  <div className="text-zinc-500 text-xs italic">{credit.characterName}</div>
                )}
              </Link>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
