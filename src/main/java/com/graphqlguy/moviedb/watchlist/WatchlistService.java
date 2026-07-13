package com.graphqlguy.moviedb.watchlist;

import com.graphqlguy.moviedb.config.LatencySimulator;
import com.graphqlguy.moviedb.exception.EntityNotFoundException;
import com.graphqlguy.moviedb.exception.InvalidInputException;
import com.graphqlguy.moviedb.movie.Movie;
import com.graphqlguy.moviedb.movie.MovieRepository;
import com.graphqlguy.moviedb.tvshow.TvShow;
import com.graphqlguy.moviedb.tvshow.TvShowRepository;
import com.graphqlguy.moviedb.user.AppUser;
import com.graphqlguy.moviedb.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// This service is starter scaffolding, present on every branch, so it must not
// depend on Spring Security (which the workshop only adds in the security class).
// The signed-in username is passed in by the controller, which is where the
// "must be authenticated" rule is enforced. Ownership is enforced here by scoping
// every lookup to that user: an item that is not theirs simply is not found.
@Service
@Transactional(readOnly = true)
public class WatchlistService {

    private final WatchlistItemRepository watchlistRepository;
    private final MovieRepository movieRepository;
    private final TvShowRepository tvShowRepository;
    private final UserRepository userRepository;
    private final LatencySimulator latencySimulator;

    public WatchlistService(final WatchlistItemRepository watchlistRepository, final MovieRepository movieRepository,
                            final TvShowRepository tvShowRepository, final UserRepository userRepository,
                            final LatencySimulator latencySimulator) {
        this.watchlistRepository = watchlistRepository;
        this.movieRepository = movieRepository;
        this.tvShowRepository = tvShowRepository;
        this.userRepository = userRepository;
        this.latencySimulator = latencySimulator;
    }

    public List<WatchlistItem> findForUser(String username) {
        latencySimulator.pause();
        AppUser user = requireUser(username);
        return watchlistRepository.findWithContentByUserId(user.getId());
    }

    @Transactional
    public WatchlistItem addToWatchlist(WatchlistSubjectInput subject, WatchStatus status, String username) {
        latencySimulator.pause();
        AppUser user = requireUser(username);

        WatchlistItem.WatchlistItemBuilder item = WatchlistItem.builder()
                .user(user)
                .status(status != null ? status : WatchStatus.WANT_TO_WATCH);

        // The @oneOf directive guarantees exactly one of movieId/tvShowId is set.
        if (subject.movieId() != null) {
            Long movieId = parseId(subject.movieId(), "movieId");
            Movie movie = movieRepository.findById(movieId)
                    .orElseThrow(() -> new EntityNotFoundException("Movie", movieId));
            if (watchlistRepository.existsByUserIdAndMovieId(user.getId(), movieId)) {
                throw new InvalidInputException("movieId", "This movie is already in your watch list");
            }
            item.movie(movie);
        } else {
            Long tvShowId = parseId(subject.tvShowId(), "tvShowId");
            TvShow tvShow = tvShowRepository.findById(tvShowId)
                    .orElseThrow(() -> new EntityNotFoundException("TvShow", tvShowId));
            if (watchlistRepository.existsByUserIdAndTvShowId(user.getId(), tvShowId)) {
                throw new InvalidInputException("tvShowId", "This show is already in your watch list");
            }
            item.tvShow(tvShow);
        }

        try {
            return watchlistRepository.save(item.build());
        } catch (DataIntegrityViolationException e) {
            // Two concurrent adds can both pass the existsBy check above; the unique
            // constraint on watchlist_items catches the loser here.
            throw new InvalidInputException("subject", "This title is already in your watch list");
        }
    }

    @Transactional
    public WatchlistItem setStatus(Long itemId, WatchStatus status, String username) {
        latencySimulator.pause();
        WatchlistItem item = requireOwnedItem(itemId, username);
        item.setStatus(status);
        return watchlistRepository.save(item);
    }

    @Transactional
    public boolean removeFromWatchlist(Long itemId, String username) {
        latencySimulator.pause();
        WatchlistItem item = requireOwnedItem(itemId, username);
        watchlistRepository.delete(item);
        return true;
    }

    private WatchlistItem requireOwnedItem(Long itemId, String username) {
        AppUser user = requireUser(username);
        // Fetch with content so a mutation returning this item can resolve its
        // Movie/TvShow after the transaction closes. Scoping to the user id both
        // enforces ownership and avoids revealing that another user's item exists.
        WatchlistItem item = watchlistRepository.findWithContentById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("WatchlistItem", itemId));
        if (!item.getUser().getId().equals(user.getId())) {
            throw new EntityNotFoundException("WatchlistItem", itemId);
        }
        return item;
    }

    private AppUser requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user has no matching record: " + username));
    }

    private static Long parseId(String rawId, String field) {
        try {
            return Long.parseLong(rawId);
        } catch (NumberFormatException e) {
            // The GraphQL ID scalar accepts any string, so garbage like "abc" reaches
            // us here; classify it as bad input rather than an unexpected 500.
            throw new InvalidInputException(field, "must be a numeric ID");
        }
    }
}
