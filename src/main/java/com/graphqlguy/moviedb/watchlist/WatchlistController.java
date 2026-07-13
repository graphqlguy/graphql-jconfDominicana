package com.graphqlguy.moviedb.watchlist;

import com.graphqlguy.moviedb.shared.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    List<WatchlistItem> watchlist(Principal principal) {
        return watchlistService.findForUser(principal.getName());
    }

    // The interface field: a WatchlistItem points at either a Movie or a TvShow, and
    // both implement Content, so the resolver just returns whichever is set. Spring
    // picks the GraphQL type from the concrete class name.
    @SchemaMapping(typeName = "WatchlistItem")
    Content content(WatchlistItem item) {
        return item.getMovie() != null ? item.getMovie() : item.getTvShow();
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    WatchlistItem addToWatchlist(@Argument WatchlistSubjectInput subject,
                                 @Argument WatchStatus status,
                                 Principal principal) {
        return watchlistService.addToWatchlist(subject, status, principal.getName());
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    WatchlistItem setWatchStatus(@Argument Long itemId, @Argument WatchStatus status, Principal principal) {
        return watchlistService.setStatus(itemId, status, principal.getName());
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    boolean removeFromWatchlist(@Argument Long itemId, Principal principal) {
        return watchlistService.removeFromWatchlist(itemId, principal.getName());
    }
}
