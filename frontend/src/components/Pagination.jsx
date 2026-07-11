export default function Pagination({ currentPage, totalPages, onPageChange }) {
  if (totalPages <= 1) return null;

  const pages = [];
  const delta = 2;
  const range = [];

  for (let i = Math.max(1, currentPage - delta); i <= Math.min(totalPages, currentPage + delta); i++) {
    range.push(i);
  }
  if (range[0] > 1) {
    pages.push(1);
    if (range[0] > 2) pages.push('...');
  }
  pages.push(...range);
  if (range[range.length - 1] < totalPages) {
    if (range[range.length - 1] < totalPages - 1) pages.push('...');
    pages.push(totalPages);
  }

  return (
    <div className="flex items-center justify-center gap-1 mt-8">
      <button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 1}
        className="px-3 py-2 rounded bg-zinc-800 text-zinc-300 hover:bg-zinc-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors text-sm"
      >
        ←
      </button>
      {pages.map((page, i) =>
        page === '...' ? (
          <span key={`dot-${i}`} className="px-3 py-2 text-zinc-500 text-sm">...</span>
        ) : (
          <button
            key={page}
            onClick={() => onPageChange(page)}
            className={`px-3 py-2 rounded text-sm font-medium transition-colors ${
              page === currentPage
                ? 'bg-yellow-500 text-black'
                : 'bg-zinc-800 text-zinc-300 hover:bg-zinc-700'
            }`}
          >
            {page}
          </button>
        )
      )}
      <button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage === totalPages}
        className="px-3 py-2 rounded bg-zinc-800 text-zinc-300 hover:bg-zinc-700 disabled:opacity-30 disabled:cursor-not-allowed transition-colors text-sm"
      >
        →
      </button>
    </div>
  );
}
