import { useState } from 'react';
import { useQuery } from '@apollo/client/react';
import { Link } from 'react-router-dom';
import { useCaps } from '../context/CapabilitiesContext';
import { NOOP } from '../graphql/documents';
import Pagination from '../components/Pagination';
import FeaturePending from '../components/FeaturePending';

const PAGE_SIZE = 24;

export default function PeoplePage() {
  const { docs } = useCaps();
  const [page, setPage] = useState(1);

  const { data, loading, error } = useQuery(docs.GET_PEOPLE ?? NOOP, {
    variables: { page: page - 1, size: PAGE_SIZE },
    skip: !docs.GET_PEOPLE,
  });

  if (!docs.GET_PEOPLE) return (
    <FeaturePending
      title="No people query yet"
      hint="Add a paginated people query (and the Person type) to the schema to browse people here."
    />
  );

  const people = data?.people?.content || [];
  const totalPages = data?.people?.totalPages || 0;
  const totalElements = data?.people?.totalElements || 0;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-white mb-2">People</h1>
        {totalElements > 0 && (
          <p className="text-zinc-400">{totalElements} people</p>
        )}
      </div>

      {loading && (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
          {Array.from({ length: PAGE_SIZE }).map((_, i) => (
            <div key={i} className="bg-zinc-900 rounded-xl border border-zinc-800 animate-pulse">
              <div className="aspect-square bg-zinc-800 rounded-t-xl" />
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
          Error loading people: {error.message}
        </div>
      )}

      {!loading && !error && (
        <>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
            {people.map(person => (
              <Link key={person.id} to={`/person/${person.id}`}
                className="bg-zinc-900 border border-zinc-800 rounded-xl overflow-hidden hover:border-zinc-600 transition-colors group">
                <div className="aspect-square bg-zinc-800 flex items-center justify-center overflow-hidden">
                  {person.photoUrl ? (
                    <img src={person.photoUrl} alt={person.name}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                      onError={e => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'flex'; }}
                    />
                  ) : null}
                  <div className={`w-full h-full flex items-center justify-center text-zinc-600 ${person.photoUrl ? 'hidden' : 'flex'}`}>
                    <svg className="w-16 h-16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1}
                        d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                  </div>
                </div>
                <div className="p-3">
                  <div className="text-white font-medium text-sm leading-snug">{person.name}</div>
                  {person.country && (
                    <div className="text-zinc-500 text-xs mt-1">{person.country.emoji} {person.country.name}</div>
                  )}
                  {person.birthYear && (
                    <div className="text-zinc-600 text-xs">b. {person.birthYear}</div>
                  )}
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
