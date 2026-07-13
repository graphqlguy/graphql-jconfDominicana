import { useState, useEffect } from 'react';
import { useMutation, useSubscription } from '@apollo/client/react';
import { useAuth } from '../context/AuthContext';
import { useCaps } from '../context/CapabilitiesContext';
import { NOOP } from '../graphql/documents';

function StarPicker({ value, onChange }) {
  const [hovered, setHovered] = useState(0);
  return (
    <div className="flex gap-1">
      {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map(n => (
        <button
          key={n}
          type="button"
          onClick={() => onChange(n)}
          onMouseEnter={() => setHovered(n)}
          onMouseLeave={() => setHovered(0)}
          className={`text-xl transition-colors ${
            n <= (hovered || value) ? 'text-yellow-400' : 'text-zinc-700'
          }`}
        >
          ★
        </button>
      ))}
      {value > 0 && (
        <span className="ml-2 text-zinc-400 text-sm self-center">{value}/10</span>
      )}
    </div>
  );
}

function ReviewCard({ review, currentUserId, isAdmin, onDelete }) {
  return (
    <div className="bg-zinc-900 border border-zinc-800 rounded-xl p-4">
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-full bg-zinc-700 flex items-center justify-center shrink-0">
            <span className="text-zinc-300 text-xs font-bold uppercase">
              {review.user.username[0]}
            </span>
          </div>
          <div>
            <span className="text-white text-sm font-medium">{review.user.username}</span>
            {review.user.email && (
              <span className="ml-2 text-xs text-zinc-500" title="Visible to admins and the user themselves">
                {review.user.email}
              </span>
            )}
            <div className="text-zinc-500 text-xs">{new Date(review.createdAt).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })}</div>
          </div>
        </div>
        <div className="flex items-center gap-3 shrink-0">
          <div className="flex items-center gap-1 bg-zinc-800 rounded-lg px-2 py-1">
            <span className="text-yellow-400 text-sm">★</span>
            <span className="text-white text-sm font-bold">{review.score}</span>
            <span className="text-zinc-500 text-xs">/10</span>
          </div>
          {onDelete && (currentUserId === review.user.id || isAdmin) && (
            <button
              onClick={() => onDelete(review.id)}
              className="text-zinc-600 hover:text-red-400 transition-colors text-xs"
              title="Delete review"
            >
              ✕
            </button>
          )}
        </div>
      </div>
      {review.comment && (
        <p className="text-zinc-300 text-sm mt-3 leading-relaxed">{review.comment}</p>
      )}
    </div>
  );
}

export default function ReviewSection({ reviews = [], movieId, tvShowId, refetchQuery }) {
  const { isLoggedIn, user, isAdmin } = useAuth();
  const { docs } = useCaps();
  const [score, setScore] = useState(0);
  const [comment, setComment] = useState('');
  const [submitError, setSubmitError] = useState('');

  const refetchOptions = { refetchQueries: [refetchQuery] };

  const [createReview, { loading: adding }] = useMutation(docs.CREATE_REVIEW ?? NOOP, refetchOptions);
  const [deleteReview] = useMutation(docs.DELETE_REVIEW ?? NOOP, refetchOptions);

  // Before the security class exists there is no login, so reviewing is open
  const authBuilt = !!docs.LOGIN;
  const mayReview = !!docs.CREATE_REVIEW && (isLoggedIn || !authBuilt);

  // Live notifications over WebSocket when anyone posts a review
  const { data: subData } = useSubscription(docs.REVIEW_ADDED ?? NOOP, {
    variables: movieId ? { movieId } : {},
    skip: !docs.REVIEW_ADDED,
  });
  const incoming = subData?.reviewAdded;
  const [dismissedReviewId, setDismissedReviewId] = useState(null);
  useEffect(() => {
    if (!incoming) return;
    const timer = setTimeout(() => setDismissedReviewId(incoming.review.id), 8000);
    return () => clearTimeout(timer);
  }, [incoming]);
  // Show the toast until it self-dismisses; never for the viewer's own review
  const liveNotification = incoming
    && incoming.review.id !== dismissedReviewId
    && incoming.review.user.username !== user?.username
    ? incoming : null;

  const avgScore = reviews.length
    ? (reviews.reduce((sum, r) => sum + r.score, 0) / reviews.length).toFixed(1)
    : null;

  const alreadyReviewed = reviews.some(r => r.user.id === user?.id);

  const handleSubmit = async e => {
    e.preventDefault();
    setSubmitError('');
    if (score === 0) { setSubmitError('Please select a score.'); return; }
    try {
      const subject = movieId ? { movieId } : { tvShowId };
      await createReview({ variables: { input: { subject, score, comment: comment.trim() || null } } });
      setScore(0);
      setComment('');
    } catch (err) {
      setSubmitError(err.message);
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('Delete this review?')) return;
    try {
      await deleteReview({ variables: { id } });
    } catch (err) {
      alert(err.message);
    }
  };

  return (
    <div className="mt-10">
      <div className="flex items-baseline gap-4 mb-6">
        <h2 className="text-xl font-bold text-white">
          Reviews <span className="text-zinc-500 font-normal text-base">({reviews.length})</span>
        </h2>
        {avgScore && (
          <span className="text-zinc-400 text-sm">
            <span className="text-yellow-400">★</span> {avgScore} avg
          </span>
        )}
      </div>

      {/* Live review notification */}
      {liveNotification && (
        <div className="bg-teal-900/30 border border-teal-700 rounded-xl p-4 mb-6 text-teal-300 text-sm animate-pulse">
          <strong>{liveNotification.review.user.username}</strong> just reviewed{' '}
          <strong>{liveNotification.title}</strong> - rated it{' '}
          <span className="text-yellow-400">★ {liveNotification.review.score}/10</span>
        </div>
      )}

      {/* Add review form - logged-in users only (or anyone, before auth exists) */}
      {mayReview && !alreadyReviewed && (
        <form onSubmit={handleSubmit} className="bg-zinc-900 border border-zinc-800 rounded-xl p-5 mb-6">
          <h3 className="text-white font-semibold mb-4">Write a Review</h3>
          <div className="mb-4">
            <label className="text-zinc-400 text-sm block mb-2">Your score</label>
            <StarPicker value={score} onChange={setScore} />
          </div>
          <div className="mb-4">
            <label className="text-zinc-400 text-sm block mb-2">Comment <span className="text-zinc-600">(optional)</span></label>
            <textarea
              value={comment}
              onChange={e => setComment(e.target.value)}
              rows={3}
              placeholder="Share your thoughts..."
              className="w-full bg-zinc-800 border border-zinc-700 rounded-lg px-3 py-2 text-white text-sm placeholder-zinc-600 focus:outline-none focus:border-yellow-500 resize-none"
            />
          </div>
          {submitError && <p className="text-red-400 text-sm mb-3">{submitError}</p>}
          <button
            type="submit"
            disabled={adding}
            className="bg-yellow-500 hover:bg-yellow-400 disabled:opacity-50 text-black font-semibold px-5 py-2 rounded-lg text-sm transition-colors"
          >
            {adding ? 'Submitting…' : 'Submit Review'}
          </button>
        </form>
      )}

      {mayReview && alreadyReviewed && (
        <div className="bg-zinc-900/50 border border-zinc-800 rounded-xl p-4 mb-6 text-zinc-400 text-sm">
          You have already reviewed this {movieId ? 'movie' : 'show'}.
        </div>
      )}

      {docs.CREATE_REVIEW && authBuilt && !isLoggedIn && (
        <div className="bg-zinc-900/50 border border-zinc-800 rounded-xl p-4 mb-6 text-zinc-400 text-sm">
          <a href="/login" className="text-yellow-400 hover:underline">Sign in</a> to leave a review.
        </div>
      )}

      {/* Review list */}
      {reviews.length === 0 ? (
        <p className="text-zinc-600 text-sm">No reviews yet. Be the first!</p>
      ) : (
        <div className="space-y-4">
          {reviews.map(review => (
            <ReviewCard
              key={review.id}
              review={review}
              currentUserId={user?.id}
              isAdmin={isAdmin || !authBuilt}
              onDelete={docs.DELETE_REVIEW ? handleDelete : null}
            />
          ))}
        </div>
      )}
    </div>
  );
}
