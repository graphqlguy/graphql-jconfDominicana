import { gql } from '@apollo/client/core';

// Safe placeholder for hooks that must always receive a document; every use
// pairs it with skip so it never actually executes.
export const NOOP = gql`query Noop { __typename }`;

// Builds every operation the app can currently send, based on the introspected
// capabilities. An operation is null when its root field does not exist yet;
// optional selections inside an operation appear only once their field exists.
export function buildDocuments(caps) {
  const has = (type, field) => caps.field(type, field);
  const when = (cond, text) => (cond ? text : '');

  // ---- shared fragments -------------------------------------------------

  const movieCore = [
    'id',
    'title',
    when(has('Movie', 'releaseYear'), 'releaseYear'),
    when(has('Movie', 'genre'), 'genre'),
    when(has('Movie', 'rating'), 'rating'),
    when(has('Movie', 'posterUrl'), 'posterUrl'),
  ].filter(Boolean).join(' ');

  const personCountry = when(has('Person', 'country'), 'country { name emoji }');

  const personRef = [
    'id',
    'name',
    when(has('Person', 'birthYear'), 'birthYear'),
    personCountry,
    when(has('Person', 'photoUrl'), 'photoUrl'),
  ].filter(Boolean).join(' ');

  const reviewFields = 'id score comment createdAt user { id username }';

  const movieListFields = `
    ${movieCore}
    ${when(has('Movie', 'runtime'), 'runtime')}
    ${when(has('Movie', 'directors'), 'directors { id name }')}
    ${when(has('Movie', 'cast'), 'cast { id characterName person { id name } }')}
    ${when(has('Movie', 'communityRating'), 'communityRating { voteAverage voteCount }')}
    ${when(has('Movie', 'reviewCount'), 'reviewCount')}
  `;

  // ---- movies -------------------------------------------------------------

  let GET_MOVIES = null;
  const moviesPaginated = caps.query('movies') && has('MoviePage', 'content');
  if (moviesPaginated) {
    const args = [
      ['filter', '$filter: MovieFilter', 'filter: $filter'],
      ['page', '$page: Int', 'page: $page'],
      ['size', '$size: Int', 'size: $size'],
      ['sort', '$sort: MovieSort', 'sort: $sort'],
    ].filter(([name]) => caps.arg('query', 'movies', name));
    const defs = args.map(a => a[1]).join(', ');
    const uses = args.map(a => a[2]).join(', ');
    GET_MOVIES = gql`
      query GetMovies${defs ? `(${defs})` : ''} {
        movies${uses ? `(${uses})` : ''} {
          content { ${movieListFields} }
          ${when(has('MoviePage', 'totalElements'), 'totalElements')}
          ${when(has('MoviePage', 'totalPages'), 'totalPages')}
          ${when(has('MoviePage', 'currentPage'), 'currentPage')}
          ${when(has('MoviePage', 'size'), 'size')}
          ${when(has('MoviePage', 'hasNext'), 'hasNext')}
          ${when(has('MoviePage', 'hasPrevious'), 'hasPrevious')}
        }
      }
    `;
  } else if (caps.query('movies')) {
    GET_MOVIES = gql`query GetMovies { movies { ${movieListFields} } }`;
  } else if (caps.query('moviesAll')) {
    GET_MOVIES = gql`query GetMovies { movies: moviesAll { ${movieListFields} } }`;
  }

  const GET_MOVIE = caps.query('movie') ? gql`
    query GetMovie($id: ID!) {
      movie(id: $id) {
        ${movieCore}
        ${when(has('Movie', 'runtime'), 'runtime')}
        ${when(has('Movie', 'plot'), 'plot')}
        ${when(has('Movie', 'tmdbId'), 'tmdbId')}
        ${when(has('Movie', 'directors'), `directors { ${personRef} ${when(has('Person', 'biography'), 'biography')} }`)}
        ${when(has('Movie', 'cast'), `cast { id characterName person { ${personRef} ${when(has('Person', 'biography'), 'biography')} } }`)}
        ${when(has('Movie', 'reviews'), `reviews { ${reviewFields} }`)}
      }
    }
  ` : null;

  // ---- search -------------------------------------------------------------

  const searchPeopleBlock = when(caps.query('searchPeople'),
    `searchPeople(name: $query) { ${personRef} }`);

  let GLOBAL_SEARCH = null;
  if (caps.query('search')) {
    // The search query is a union; movies and people arrive in one list,
    // distinguished by __typename.
    GLOBAL_SEARCH = gql`
      query GlobalSearch($query: String!) {
        search(query: $query) {
          ... on Movie { __typename ${movieCore} }
          ... on Person { __typename ${personRef} }
        }
      }
    `;
  } else if (caps.query('searchMovies') || caps.query('searchPeople')) {
    // Before the union exists, fall back to the separate typed searches.
    GLOBAL_SEARCH = gql`
      query GlobalSearch($query: String!) {
        ${when(caps.query('searchMovies'), `searchMovies(title: $query) { ${movieCore} }`)}
        ${searchPeopleBlock}
      }
    `;
  }

  // ---- people -------------------------------------------------------------

  const GET_PEOPLE = caps.query('people') ? gql`
    query GetPeople($page: Int, $size: Int) {
      people(page: $page, size: $size) {
        content { ${personRef} }
        ${when(has('PersonPage', 'totalElements'), 'totalElements')}
        ${when(has('PersonPage', 'totalPages'), 'totalPages')}
        ${when(has('PersonPage', 'currentPage'), 'currentPage')}
        ${when(has('PersonPage', 'size'), 'size')}
      }
    }
  ` : null;

  const GET_PERSON = caps.query('person') ? gql`
    query GetPerson($id: ID!) {
      person(id: $id) {
        ${personRef}
        ${when(has('Person', 'biography'), 'biography')}
        ${when(has('Person', 'directedMovies'), `directedMovies { ${movieCore} }`)}
        ${when(has('Person', 'movieCastCredits'), `movieCastCredits { id characterName movie { ${movieCore} } }`)}
        ${when(has('Person', 'createdShows'), 'createdShows { id title startYear endYear genre posterUrl rating }')}
        ${when(has('Person', 'tvShowCastCredits'), 'tvShowCastCredits { id characterName tvShow { id title startYear genre posterUrl rating } }')}
      }
    }
  ` : null;

  // ---- TV shows -----------------------------------------------------------

  const GET_TV_SHOWS = caps.query('tvShows') ? gql`
    query GetTvShows($page: Int, $size: Int) {
      tvShows(page: $page, size: $size) {
        content {
          id title genre rating posterUrl startYear endYear
          ${when(has('TvShow', 'seasons'), 'seasons')}
          ${when(has('TvShow', 'creators'), 'creators { id name }')}
          ${when(has('TvShow', 'cast'), 'cast { id characterName person { id name } }')}
        }
        ${when(has('TvShowPage', 'totalElements'), 'totalElements')}
        ${when(has('TvShowPage', 'totalPages'), 'totalPages')}
        ${when(has('TvShowPage', 'currentPage'), 'currentPage')}
        ${when(has('TvShowPage', 'size'), 'size')}
      }
    }
  ` : null;

  const GET_TV_SHOW = caps.query('tvShow') ? gql`
    query GetTvShow($id: ID!) {
      tvShow(id: $id) {
        id title genre rating posterUrl startYear endYear
        ${when(has('TvShow', 'seasons'), 'seasons')}
        ${when(has('TvShow', 'plot'), 'plot')}
        ${when(has('TvShow', 'creators'), `creators { ${personRef} ${when(has('Person', 'biography'), 'biography')} }`)}
        ${when(has('TvShow', 'cast'), `cast { id characterName person { ${personRef} } }`)}
        ${when(has('TvShow', 'episodes'), 'episodes { id seasonNumber episodeNumber title overview runtime airYear }')}
        ${when(has('TvShow', 'reviews'), `reviews { ${reviewFields} }`)}
      }
    }
  ` : null;

  // ---- TMDB ---------------------------------------------------------------

  const TMDB_SEARCH = caps.query('tmdbSearch') ? gql`
    query TmdbSearch($title: String!) {
      tmdbSearch(title: $title) {
        tmdbId title releaseYear overview posterUrl rating
      }
    }
  ` : null;

  // ---- mutations ----------------------------------------------------------

  const authResponse = 'token user { id username email role }';
  const LOGIN = caps.mutation('login') ? gql`
    mutation Login($input: LoginInput!) { login(input: $input) { ${authResponse} } }
  ` : null;
  const REGISTER = caps.mutation('register') ? gql`
    mutation Register($input: RegisterInput!) { register(input: $input) { ${authResponse} } }
  ` : null;

  const CREATE_MOVIE = caps.mutation('createMovie') ? gql`
    mutation CreateMovie($input: CreateMovieInput!) {
      createMovie(input: $input) {
        ${movieCore}
        ${when(has('Movie', 'runtime'), 'runtime')}
        ${when(has('Movie', 'plot'), 'plot')}
        ${when(has('Movie', 'tmdbId'), 'tmdbId')}
      }
    }
  ` : null;

  const UPDATE_MOVIE = caps.mutation('updateMovie') ? gql`
    mutation UpdateMovie($input: UpdateMovieInput!) {
      updateMovie(input: $input) {
        ${movieCore}
        ${when(has('Movie', 'runtime'), 'runtime')}
        ${when(has('Movie', 'plot'), 'plot')}
        ${when(has('Movie', 'tmdbId'), 'tmdbId')}
      }
    }
  ` : null;

  const DELETE_MOVIE = caps.mutation('deleteMovie') ? gql`
    mutation DeleteMovie($id: ID!) { deleteMovie(id: $id) { success message } }
  ` : null;

  const CREATE_PERSON = caps.mutation('createPerson') ? gql`
    mutation CreatePerson($input: CreatePersonInput!) {
      createPerson(input: $input) { id name birthYear ${when(has('Person', 'countryCode'), 'countryCode')} }
    }
  ` : null;

  const UPDATE_PERSON = caps.mutation('updatePerson') ? gql`
    mutation UpdatePerson($input: UpdatePersonInput!) {
      updatePerson(input: $input) { ${personRef} ${when(has('Person', 'biography'), 'biography')} }
    }
  ` : null;

  const DELETE_PERSON = caps.mutation('deletePerson') ? gql`
    mutation DeletePerson($id: ID!, $force: Boolean!) {
      deletePerson(id: $id, force: $force) { success error deletedId }
    }
  ` : null;

  const CREATE_REVIEW = caps.mutation('createReview') ? gql`
    mutation CreateReview($input: CreateReviewInput!) {
      createReview(input: $input) { ${reviewFields} }
    }
  ` : null;

  const DELETE_REVIEW = caps.mutation('deleteReview') ? gql`
    mutation DeleteReview($id: ID!) { deleteReview(id: $id) { success deletedId } }
  ` : null;

  // ---- subscriptions ------------------------------------------------------

  const REVIEW_ADDED = caps.subscription('reviewAdded') ? gql`
    subscription ReviewAdded($movieId: ID) {
      reviewAdded(movieId: $movieId) {
        review { ${reviewFields} }
        movieId
        tvShowId
        title
      }
    }
  ` : null;

  return {
    GET_MOVIES, GET_MOVIE, GLOBAL_SEARCH,
    GET_PEOPLE, GET_PERSON,
    GET_TV_SHOWS, GET_TV_SHOW,
    TMDB_SEARCH,
    LOGIN, REGISTER,
    CREATE_MOVIE, UPDATE_MOVIE, DELETE_MOVIE,
    CREATE_PERSON, UPDATE_PERSON, DELETE_PERSON, CREATE_REVIEW, DELETE_REVIEW,
    REVIEW_ADDED,
  };
}
