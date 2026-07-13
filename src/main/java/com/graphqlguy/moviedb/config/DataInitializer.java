package com.graphqlguy.moviedb.config;

import com.graphqlguy.moviedb.movie.Movie;
import com.graphqlguy.moviedb.movie.MovieCast;
import com.graphqlguy.moviedb.movie.MovieCastRepository;
import com.graphqlguy.moviedb.movie.MovieRepository;
import com.graphqlguy.moviedb.person.Person;
import com.graphqlguy.moviedb.person.PersonRepository;
import com.graphqlguy.moviedb.review.Review;
import com.graphqlguy.moviedb.review.ReviewRepository;
import com.graphqlguy.moviedb.watchlist.WatchStatus;
import com.graphqlguy.moviedb.watchlist.WatchlistItem;
import com.graphqlguy.moviedb.watchlist.WatchlistItemRepository;
import com.graphqlguy.moviedb.tvshow.TvShow;
import com.graphqlguy.moviedb.tvshow.TvShowCast;
import com.graphqlguy.moviedb.tvshow.TvShowCastRepository;
import com.graphqlguy.moviedb.tvshow.TvShowRepository;
import com.graphqlguy.moviedb.tvshow.Episode;
import com.graphqlguy.moviedb.tvshow.EpisodeRepository;
import com.graphqlguy.moviedb.user.AppUser;
import com.graphqlguy.moviedb.user.Role;
import com.graphqlguy.moviedb.user.UserRepository;
import com.graphqlguy.moviedb.shared.Genre;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final MovieRepository movieRepo;
    private final PersonRepository personRepo;
    private final MovieCastRepository movieCastRepo;
    private final TvShowRepository tvShowRepo;
    private final TvShowCastRepository tvShowCastRepo;
    private final EpisodeRepository episodeRepo;
    private final UserRepository userRepo;
    private final ReviewRepository reviewRepo;
    private final WatchlistItemRepository watchlistRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(final MovieRepository movieRepo, final PersonRepository personRepo,
                           final MovieCastRepository movieCastRepo, final TvShowRepository tvShowRepo,
                           final TvShowCastRepository tvShowCastRepo, final EpisodeRepository episodeRepo,
                           final UserRepository userRepo, final ReviewRepository reviewRepo,
                           final WatchlistItemRepository watchlistRepo,
                           final PasswordEncoder passwordEncoder) {
        this.movieRepo = movieRepo;
        this.personRepo = personRepo;
        this.movieCastRepo = movieCastRepo;
        this.tvShowRepo = tvShowRepo;
        this.tvShowCastRepo = tvShowCastRepo;
        this.episodeRepo = episodeRepo;
        this.userRepo = userRepo;
        this.reviewRepo = reviewRepo;
        this.watchlistRepo = watchlistRepo;
        this.passwordEncoder = passwordEncoder;
    }

    private static final String BASE = "https://image.tmdb.org/t/p/w500/";

    private record CastEntry(Person person, String characterName) {}


    @Bean
    CommandLineRunner initData() {
        return args -> {
            if (movieRepo.count() == 0) {
                log.info("Seeding database with movies, actors, directors, and TV shows...");

            // ── Users ─────────────────────────────────────────────────────
            AppUser admin = userRepo.save(AppUser.builder().username("admin").email("admin@moviedb.com")
                .password(passwordEncoder.encode("admin123")).role(Role.ADMIN).build());
            AppUser user = userRepo.save(AppUser.builder().username("user").email("user@moviedb.com")
                .password(passwordEncoder.encode("user123")).role(Role.USER).build());

            // ── Persons (directors) ───────────────────────────────────────
            Person christopherNolan = personRepo.save(Person.builder().name("Christopher Nolan").birthYear(1970).countryCode("GB")
                    .biography("Master of mind-bending large-scale narratives.").photoUrl(BASE + "xuAIuYSmsUzKlUMBFGVZaWsY3DZ.jpg").build());
            Person davidFincher = personRepo.save(Person.builder().name("David Fincher").birthYear(1962).countryCode("US")
                    .biography("Master of dark psychological thrillers.").photoUrl(BASE + "tpEczFclQZeKAiCeKZZ0adRvtfz.jpg").build());
            Person martinScorsese = personRepo.save(Person.builder().name("Martin Scorsese").birthYear(1942).countryCode("US")
                    .biography("Legendary filmmaker of crime epics and character studies.").photoUrl(BASE + "9U9Y5GQuWX3EZy39B8nkk4NY01S.jpg").build());
            Person francisFordCoppola = personRepo.save(Person.builder().name("Francis Ford Coppola").birthYear(1939).countryCode("US")
                    .biography("Director of The Godfather trilogy.").photoUrl(BASE + "IwGgkmW6IoJ9vuNF0T9CU3FYUX.jpg").build());
            Person stanleyKubrick = personRepo.save(Person.builder().name("Stanley Kubrick").birthYear(1928).countryCode("US")
                    .biography("Perfectionist visionary known for genre-defining films.").photoUrl(BASE + "yFT0VyIelI9aegZrsAwOG5iVP4v.jpg").build());
            Person ridleyScott = personRepo.save(Person.builder().name("Ridley Scott").birthYear(1937).countryCode("GB")
                    .biography("Versatile director of sci-fi, epics and thrillers.").photoUrl(BASE + "zABJmN9opmqD4orWl3KSdCaSo7Q.jpg").build());
            Person denisVilleneuve = personRepo.save(Person.builder().name("Denis Villeneuve").birthYear(1967).countryCode("CA")
                    .biography("Modern master of cerebral science fiction.").photoUrl(BASE + "zdDx9Xs93UIrJFWYApYR28J8M6b.jpg").build());
            Person jamesCameron = personRepo.save(Person.builder().name("James Cameron").birthYear(1954).countryCode("CA")
                    .biography("Technical pioneer known for blockbusters.").photoUrl(BASE + "9NAZnTjBQ9WcXAQEzZpKy4vdQto.jpg").build());
            Person peterJackson = personRepo.save(Person.builder().name("Peter Jackson").birthYear(1961).countryCode("NZ")
                    .biography("Director of the Lord of the Rings trilogy.").photoUrl(BASE + "bNc908d59Ba8VDNr4eCcm4G1cR.jpg").build());
            Person frankDarabont = personRepo.save(Person.builder().name("Frank Darabont").birthYear(1959).countryCode("US")
                    .biography("Director of two of Stephen King's greatest adaptations.").photoUrl(BASE + "oQvVLXw8Sh7gDww3g1jr0UY0FFj.jpg").build());
            Person robertZemeckis = personRepo.save(Person.builder().name("Robert Zemeckis").birthYear(1951).countryCode("US")
                    .biography("Known for innovative VFX and heartfelt stories.").photoUrl(BASE + "lPYDQ5LYNJ12rJZENtyASmVZ1Ql.jpg").build());
            Person johnMcTiernan = personRepo.save(Person.builder().name("John McTiernan").birthYear(1951).countryCode("US")
                    .biography("Action director known for Die Hard.").photoUrl(BASE + "yVfDkVbgQHD1A7JSV8Z47EjB1mU.jpg").build());
            Person sidneyLumet = personRepo.save(Person.builder().name("Sidney Lumet").birthYear(1924).countryCode("US")
                    .biography("Director of intense character dramas.").photoUrl(BASE + "hjj3V2DkPJ46zo5uz9bsZQzAk6R.jpg").build());
            Person josephKosinski = personRepo.save(Person.builder().name("Joseph Kosinski").birthYear(1974).countryCode("US")
                    .biography("Director of Top Gun: Maverick.").photoUrl(BASE + "oWLUXWY0j8TYzwnf2wETYWO181S.jpg").build());
            Person anthonyRusso = personRepo.save(Person.builder().name("Anthony Russo").birthYear(1970).countryCode("US")
                    .biography("Co-director of Avengers: Endgame.").photoUrl(BASE + "xbINBnWn28YygYWUJ1aSAw0xPRv.jpg").build());
            Person joeRusso = personRepo.save(Person.builder().name("Joe Russo").birthYear(1971).countryCode("US")
                    .biography("Co-director of Avengers: Endgame.").photoUrl(BASE + "o0OXjFzL10jCy89iAs7UzzSbyoK.jpg").build());
            Person lanaWachowski = personRepo.save(Person.builder().name("Lana Wachowski").birthYear(1965).countryCode("US")
                    .biography("Co-creator of the Matrix franchise.").photoUrl(BASE + "5KuRHnoH8UkSCFHMKf4YjKOvzOM.jpg").build());
            Person lillyWachowski = personRepo.save(Person.builder().name("Lilly Wachowski").birthYear(1967).countryCode("US")
                    .biography("Co-creator of the Matrix franchise.").photoUrl(BASE + "rCScAjSpeKA19BLNR07MqNNeeTT.jpg").build());
            Person sergioLeone = personRepo.save(Person.builder().name("Sergio Leone").birthYear(1929).countryCode("IT")
                    .biography("Master of the Spaghetti Western genre.").photoUrl(BASE + "2576qoW8l9Z1nKGM10ar60aIwUu.jpg").build());
            Person georgeLucas = personRepo.save(Person.builder().name("George Lucas").birthYear(1944).countryCode("US")
                    .biography("Creator of Star Wars and Indiana Jones.").photoUrl(BASE + "mDLDvsx8PaZoEThkBdyaG1JxPdf.jpg").build());
            Person irvinKershner = personRepo.save(Person.builder().name("Irvin Kershner").birthYear(1923).countryCode("US")
                    .biography("Director of The Empire Strikes Back.").photoUrl(BASE + "imtFUtcASoh2e1Emtt62UuFkIWA.jpg").build());
            Person richardMarquand = personRepo.save(Person.builder().name("Richard Marquand").birthYear(1937).countryCode("GB")
                    .biography("Director of Return of the Jedi.").photoUrl(BASE + "eEalDQpLsXJqejPDQ3MWGe95UHT.jpg").build());
            Person jJAbrams = personRepo.save(Person.builder().name("J.J. Abrams").birthYear(1966).countryCode("US")
                    .biography("Director of Star Wars: The Force Awakens and The Rise of Skywalker.").photoUrl(BASE + "k4IWd2RV5kY1kAL2VgKQwFvnCLP.jpg").build());
            Person rianJohnson = personRepo.save(Person.builder().name("Rian Johnson").birthYear(1973).countryCode("US")
                    .biography("Director of The Last Jedi and Knives Out.").photoUrl(BASE + "ggwlJvCn0laNGjcvwGchuwC00hQ.jpg").build());
            Person jonathanMostow = personRepo.save(Person.builder().name("Jonathan Mostow").birthYear(1961).countryCode("US")
                    .biography("Director of Terminator 3: Rise of the Machines.").photoUrl(BASE + "yRMYvjGLIf0aOUKVLnU6jSpR1oQ.jpg").build());
            Person mcg = personRepo.save(Person.builder().name("McG").birthYear(1968).countryCode("US")
                    .biography("Director of Terminator Salvation.").photoUrl(BASE + "sEcoHVCqc2IrJkxgixGHrDytsyd.jpg").build());
            Person alanTaylor = personRepo.save(Person.builder().name("Alan Taylor").birthYear(1959).countryCode("US")
                    .biography("Director of Terminator Genisys.").photoUrl(BASE + "sXC2wNRo7lshghNnNaPdWQ9sqKe.jpg").build());
            Person timMiller = personRepo.save(Person.builder().name("Tim Miller").birthYear(1964).countryCode("US")
                    .biography("Director of Deadpool and Terminator: Dark Fate.").photoUrl(BASE + "dCyBYwhO76j5wA96HPb6k5xk2Le.jpg").build());
            Person rennyHarlin = personRepo.save(Person.builder().name("Renny Harlin").birthYear(1959).countryCode("FI")
                    .biography("Action director known for Die Hard 2.").photoUrl(BASE + "IhXiDrZBrsLZpB5K5BmzUGkp4G.jpg").build());
            Person lenWiseman = personRepo.save(Person.builder().name("Len Wiseman").birthYear(1973).countryCode("US")
                    .biography("Director of Live Free or Die Hard.").photoUrl(BASE + "aXsTPBb6dQ2T3PxLqy2ijf2nxrG.jpg").build());
            Person johnMoore = personRepo.save(Person.builder().name("John Moore").birthYear(1970).countryCode("IE")
                    .biography("Director of A Good Day to Die Hard.").photoUrl(BASE + "n3czTYjeuJHaTLvMHllnT35uhnF.jpg").build());
            Person jonathanDemme = personRepo.save(Person.builder().name("Jonathan Demme").birthYear(1944).countryCode("US")
                    .biography("Director of The Silence of the Lambs.").photoUrl(BASE + "fb3TfFITlOC0BN3kNpUXj1FL0LN.jpg").build());
            Person michaelCurtiz = personRepo.save(Person.builder().name("Michael Curtiz").birthYear(1886).countryCode("HU")
                    .biography("Director of Casablanca.").photoUrl(BASE + "AnxPuEsdjPTJ6uIaHY0KdgBeu7t.jpg").build());
            Person alfredHitchcock = personRepo.save(Person.builder().name("Alfred Hitchcock").birthYear(1899).countryCode("GB")
                    .biography("Master of suspense and psychological thrillers.").photoUrl(BASE + "108fiNM6poRieMg7RIqLJRxdAwG.jpg").build());
            Person johnKrasinski = personRepo.save(Person.builder().name("John Krasinski").birthYear(1979).countryCode("US")
                    .biography("Actor-director known for A Quiet Place.").photoUrl(BASE + "6YauDiiTBwRGC1xnwspPmNvPWUu.jpg").build());
            Person davidCrane = personRepo.save(Person.builder().name("David Crane").birthYear(1957).countryCode("US")
                    .biography("Co-creator and showrunner of Friends.").photoUrl(BASE + "1NYo5ZYCSqoxQ5sqXLMDm3cqvKp.jpg").build());
            Person martaKauffman = personRepo.save(Person.builder().name("Marta Kauffman").birthYear(1956).countryCode("US")
                    .biography("Co-creator and showrunner of Friends.").photoUrl(BASE + "AsX4ZOoQP5oQVLiA51zdRiTNKTm.jpg").build());
            Person larryDavid = personRepo.save(Person.builder().name("Larry David").birthYear(1947).countryCode("US")
                    .biography("Co-creator and showrunner of Seinfeld.").photoUrl(BASE + "ojPx93eaDcanOVi4AH14uAFwXhn.jpg").build());
            Person davidBenioff = personRepo.save(Person.builder().name("David Benioff").birthYear(1970).countryCode("US")
                    .biography("Co-creator of Game of Thrones.").photoUrl(BASE + "xvNN5huL0X8yJ7h3IZfGG4O2zBD.jpg").build());
            Person dBWeiss = personRepo.save(Person.builder().name("D.B. Weiss").birthYear(1971).countryCode("US")
                    .biography("Co-creator of Game of Thrones.").photoUrl(BASE + "6Wt006TIQoDSSnl0YaKihfn3w7K.jpg").build());

            // ── Persons (actors) ──────────────────────────────────────────
            Person tomHanks = personRepo.save(Person.builder().name("Tom Hanks").birthYear(1956).countryCode("US")
                    .biography("Versatile actor known for dramatic and comedic roles.").photoUrl(BASE + "eKF1sGJRrZJbfBG1KirPt1cfNd3.jpg").build());
            Person leonardoDiCaprio = personRepo.save(Person.builder().name("Leonardo DiCaprio").birthYear(1974).countryCode("US")
                    .biography("Acclaimed actor known for intense performances.").photoUrl(BASE + "qZs7xVpe2gGXfGaS5NRkhjPOedW.jpg").build());
            Person morganFreeman = personRepo.save(Person.builder().name("Morgan Freeman").birthYear(1937).countryCode("US")
                    .biography("Iconic actor with a distinctive voice.").photoUrl(BASE + "jPsLqiYGSofU4s6BjrxnefMfabb.jpg").build());
            Person robertDeNiro = personRepo.save(Person.builder().name("Robert De Niro").birthYear(1943).countryCode("US")
                    .biography("Legendary method actor.").photoUrl(BASE + "cT8htcckIuyI1Lqwt1CvD02ynTh.jpg").build());
            Person alPacino = personRepo.save(Person.builder().name("Al Pacino").birthYear(1940).countryCode("US")
                    .biography("Iconic for passionate and intense portrayals.").photoUrl(BASE + "m8HAAjq1T75JypKk0v1FFQn4ysZ.jpg").build());
            Person bradPitt = personRepo.save(Person.builder().name("Brad Pitt").birthYear(1963).countryCode("US")
                    .biography("Award-winning actor and producer.").photoUrl(BASE + "cckcYc2v0yh1tc9QjRelptcOBko.jpg").build());
            Person christianBale = personRepo.save(Person.builder().name("Christian Bale").birthYear(1974).countryCode("GB")
                    .biography("Known for extreme physical transformations.").photoUrl(BASE + "7Pxez9J8fuPd2Mn9kex13YALrCQ.jpg").build());
            Person matthewMcConaughey = personRepo.save(Person.builder().name("Matthew McConaughey").birthYear(1969).countryCode("US")
                    .biography("Oscar-winning actor.").photoUrl(BASE + "lCySuYjhXix3FzQdS4oceDDrXKI.jpg").build());
            Person nataliePortman = personRepo.save(Person.builder().name("Natalie Portman").birthYear(1981).countryCode("IL")
                    .biography("Oscar-winning actress.").photoUrl(BASE + "edPU5HxncLWa1YkgRPNkSd68ONG.jpg").build());
            Person keanuReeves = personRepo.save(Person.builder().name("Keanu Reeves").birthYear(1964).countryCode("CA")
                    .biography("Action star known for physicality.").photoUrl(BASE + "8RZLOyYGsoRe9p44q3xin9QkMHv.jpg").build());
            Person samuelLJackson = personRepo.save(Person.builder().name("Samuel L. Jackson").birthYear(1948).countryCode("US")
                    .biography("One of the highest-grossing actors of all time.").photoUrl(BASE + "AiAYAqwpM5xmiFrAIeQvUXDCVvo.jpg").build());
            Person marlonBrando = personRepo.save(Person.builder().name("Marlon Brando").birthYear(1924).countryCode("US")
                    .biography("Revolutionary actor who transformed American cinema.").photoUrl(BASE + "iyO183LVAJ0I4ZkNibINPjfAjCP.jpg").build());
            Person liamNeeson = personRepo.save(Person.builder().name("Liam Neeson").birthYear(1952).countryCode("IE")
                    .biography("Dramatic actor turned action star.").photoUrl(BASE + "g0iIEyt9ILiKTG0g8K69US5VtLy.jpg").build());
            Person tomCruise = personRepo.save(Person.builder().name("Tom Cruise").birthYear(1962).countryCode("US")
                    .biography("Megastar known for doing his own stunts.").photoUrl(BASE + "maf8PhSvDCdEwjEMbYfGpojR5RP.jpg").build());
            Person sigourneyWeaver = personRepo.save(Person.builder().name("Sigourney Weaver").birthYear(1949).countryCode("US")
                    .biography("Pioneer of strong female roles in sci-fi.").photoUrl(BASE + "wTSnfktNBLd6kwQxgvkqYw6vEon.jpg").build());
            Person jackNicholson = personRepo.save(Person.builder().name("Jack Nicholson").birthYear(1937).countryCode("US")
                    .biography("Three-time Oscar winner.").photoUrl(BASE + "hBHcQIEa6P48HQAlLZkh0eKSSkG.jpg").build());
            Person cateBlanchett = personRepo.save(Person.builder().name("Cate Blanchett").birthYear(1969).countryCode("AU")
                    .biography("Two-time Oscar winner known for range.").photoUrl(BASE + "9ZhDs8qPwIYLDQDk9YUCo6bs5Li.jpg").build());
            Person harrisonFord = personRepo.save(Person.builder().name("Harrison Ford").birthYear(1942).countryCode("US")
                    .biography("Star of Star Wars and Indiana Jones.").photoUrl(BASE + "zVnHagUvXkR2StdOtquEwsiwSVt.jpg").build());
            Person bruceWillis = personRepo.save(Person.builder().name("Bruce Willis").birthYear(1955).countryCode("US")
                    .biography("Action hero known for Die Hard.").photoUrl(BASE + "w3aXr1e7gQCn8MSp1vW4sXHn99P.jpg").build());
            Person elijahWood = personRepo.save(Person.builder().name("Elijah Wood").birthYear(1981).countryCode("US")
                    .biography("Known for the Lord of the Rings trilogy.").photoUrl(BASE + "ayARmqAe9Aab1zg6FjJG0u9MEBo.jpg").build());
            Person ianMcKellen = personRepo.save(Person.builder().name("Ian McKellen").birthYear(1939).countryCode("GB")
                    .biography("Acclaimed stage and screen actor.").photoUrl(BASE + "coWjgMEYJjk2OrNddlXCBm8EIr3.jpg").build());
            Person chadwickBoseman = personRepo.save(Person.builder().name("Chadwick Boseman").birthYear(1976).countryCode("US")
                    .biography("Star of Black Panther.").photoUrl(BASE + "1lz1wLOuPFSRIratMz0SxD3tkJ.jpg").build());
            Person timotheeChalamet = personRepo.save(Person.builder().name("Timothée Chalamet").birthYear(1995).countryCode("US")
                    .biography("Rising star of Dune and Call Me by Your Name.").photoUrl(BASE + "dFxpwRpmzpVfP1zjluH68DeQhyj.jpg").build());
            Person scarlettJohansson = personRepo.save(Person.builder().name("Scarlett Johansson").birthYear(1984).countryCode("US")
                    .biography("Star of the Avengers franchise.").photoUrl(BASE + "mjReG6rR7NPMEIWb1T4YWtV11ty.jpg").build());
            Person danielCraig = personRepo.save(Person.builder().name("Daniel Craig").birthYear(1968).countryCode("GB")
                    .biography("Known for his role as James Bond.").photoUrl(BASE + "iFerDZUmC5Fu26i4qI8xnUVEHc7.jpg").build());
            Person jessicaChastain = personRepo.save(Person.builder().name("Jessica Chastain").birthYear(1977).countryCode("US")
                    .biography("Oscar-winning actress known for dramatic roles.").photoUrl(BASE + "lodMzLKSdrPcBry6TdoDsMN3Vge.jpg").build());
            Person kevinSpacey = personRepo.save(Person.builder().name("Kevin Spacey").birthYear(1959).countryCode("US")
                    .biography("Two-time Oscar winner.").photoUrl(BASE + "nPrUZDEbGQe6jwpVbHKJCXsMd7r.jpg").build());
            Person clintEastwood = personRepo.save(Person.builder().name("Clint Eastwood").birthYear(1930).countryCode("US")
                    .biography("Iconic actor and director known for Westerns and thrillers.").photoUrl(BASE + "8TwdCfeOZH7ucRlfLZ6wObxa7cO.jpg").build());
            Person ewanMcGregor = personRepo.save(Person.builder().name("Ewan McGregor").birthYear(1971).countryCode("GB")
                    .biography("Known for the Star Wars prequel trilogy and Trainspotting.").photoUrl(BASE + "tw6lVBh0DvAUkCd1jsU98yD1usk.jpg").build());
            Person daisyRidley = personRepo.save(Person.builder().name("Daisy Ridley").birthYear(1992).countryCode("GB")
                    .biography("Star of the Star Wars sequel trilogy as Rey.").photoUrl(BASE + "iVboQmgPC3tYFjezBjrVECJRS8n.jpg").build());
            Person lindaHamilton = personRepo.save(Person.builder().name("Linda Hamilton").birthYear(1956).countryCode("US")
                    .biography("Known for her role as Sarah Connor in the Terminator franchise.").photoUrl(BASE + "7FNn9Z5xkRS9EFbGL2tpmpph9xV.jpg").build());
            Person timRobbins = personRepo.save(Person.builder().name("Tim Robbins").birthYear(1958).countryCode("US")
                    .biography("Known for The Shawshank Redemption and Mystic River.").photoUrl(BASE + "q9Q3c7HsZEEqrwNIY9yDgsJ74uE.jpg").build());
            Person henryFonda = personRepo.save(Person.builder().name("Henry Fonda").birthYear(1905).countryCode("US")
                    .biography("Legendary actor known for 12 Angry Men and The Grapes of Wrath.").photoUrl(BASE + "1G8TxAQnndnY5CLqVApBQ8RUT4A.jpg").build());
            Person anthonyHopkins = personRepo.save(Person.builder().name("Anthony Hopkins").birthYear(1937).countryCode("GB")
                    .biography("Oscar winner known for The Silence of the Lambs.").photoUrl(BASE + "dYVQTK1dPrQl1mugeLEWSSmA6Im.jpg").build());
            Person jodieFoster = personRepo.save(Person.builder().name("Jodie Foster").birthYear(1962).countryCode("US")
                    .biography("Oscar-winning actress known for The Silence of the Lambs.").photoUrl(BASE + "v6ezjezzDo6xP2wlONO5ZzBciwl.jpg").build());
            Person emilyBlunt = personRepo.save(Person.builder().name("Emily Blunt").birthYear(1983).countryCode("GB")
                    .biography("Known for A Quiet Place and Edge of Tomorrow.").photoUrl(BASE + "5nCSG5TL1bP1geD8aaBfaLnLLCD.jpg").build());
            Person anthonyPerkins = personRepo.save(Person.builder().name("Anthony Perkins").birthYear(1932).countryCode("US")
                    .biography("Best known for his role as Norman Bates in Psycho.").photoUrl(BASE + "rrLTDDFo23kSdvj19qMaUEVI9BQ.jpg").build());
            Person janetLeigh = personRepo.save(Person.builder().name("Janet Leigh").birthYear(1927).countryCode("US")
                    .biography("Known for her iconic role in Psycho.").photoUrl(BASE + "fe7QwANelGt0M1PLKj9qTJF9FZu.jpg").build());
            Person humphreyBogart = personRepo.save(Person.builder().name("Humphrey Bogart").birthYear(1899).countryCode("US")
                    .biography("Classic Hollywood icon known for Casablanca and The Maltese Falcon.").photoUrl(BASE + "4pk2VbOb2td7iBZyir6Ji46HH4N.jpg").build());
            Person ingridBergman = personRepo.save(Person.builder().name("Ingrid Bergman").birthYear(1915).countryCode("SE")
                    .biography("Legendary actress known for Casablanca and Notorious.").photoUrl(BASE + "lzXRh16qe4HHeBN6tMyw0DHvaMn.jpg").build());
            Person jenniferAniston = personRepo.save(Person.builder().name("Jennifer Aniston").birthYear(1969).countryCode("US")
                    .biography("Known for playing Rachel Green in Friends.").photoUrl(BASE + "vq7KKJE4gsb8WQEUkvMB2zUcsOO.jpg").build());
            Person courteneyCox = personRepo.save(Person.builder().name("Courteney Cox").birthYear(1964).countryCode("US")
                    .biography("Known for playing Monica Geller in Friends.").photoUrl(BASE + "cSOORhCRPJiwKghozXVXrOBi3Tp.jpg").build());
            Person lisaKudrow = personRepo.save(Person.builder().name("Lisa Kudrow").birthYear(1963).countryCode("US")
                    .biography("Known for playing Phoebe Buffay in Friends.").photoUrl(BASE + "ziatnwJRiBJIcc8jlk6xoClhfOy.jpg").build());
            Person mattLeBlanc = personRepo.save(Person.builder().name("Matt LeBlanc").birthYear(1967).countryCode("US")
                    .biography("Known for playing Joey Tribbiani in Friends.").photoUrl(BASE + "4oGrLuAVBHPqRbbaQH6p85bEDwu.jpg").build());
            Person matthewPerry = personRepo.save(Person.builder().name("Matthew Perry").birthYear(1969).countryCode("CA")
                    .biography("Known for playing Chandler Bing in Friends.").photoUrl(BASE + "ecDzkLWPV1z0x31I1GTjNmLxAHk.jpg").build());
            Person davidSchwimmer = personRepo.save(Person.builder().name("David Schwimmer").birthYear(1966).countryCode("US")
                    .biography("Known for playing Ross Geller in Friends.").photoUrl(BASE + "cNwpRXSN5mxlT7Gee3JayYHae1b.jpg").build());
            Person jerrySeinfeld = personRepo.save(Person.builder().name("Jerry Seinfeld").birthYear(1954).countryCode("US")
                    .biography("Stand-up comedian and co-creator/star of Seinfeld.").photoUrl(BASE + "nZdVry7lnUkE24PnXakok9okvL4.jpg").build());
            Person juliaLouisDreyfus = personRepo.save(Person.builder().name("Julia Louis-Dreyfus").birthYear(1961).countryCode("US")
                    .biography("Emmy-winning actress known for Seinfeld and Veep.").photoUrl(BASE + "2QUEYVhrKbOkRKeFEUnc5sJby6a.jpg").build());
            Person jasonAlexander = personRepo.save(Person.builder().name("Jason Alexander").birthYear(1959).countryCode("US")
                    .biography("Known for playing George Costanza in Seinfeld.").photoUrl(BASE + "qRezZ2yhM2bmBERt7jVcxo8RVSA.jpg").build());
            Person michaelRichards = personRepo.save(Person.builder().name("Michael Richards").birthYear(1949).countryCode("US")
                    .biography("Known for playing Cosmo Kramer in Seinfeld.").photoUrl(BASE + "bAYwmBdPJAiFkossWq56pEDmUh4.jpg").build());
            Person emiliaClarke = personRepo.save(Person.builder().name("Emilia Clarke").birthYear(1986).countryCode("GB")
                    .biography("Known for playing Daenerys Targaryen in Game of Thrones.").photoUrl(BASE + "6Sjz9teWjrMY9lF2o9FCo4XmoRh.jpg").build());
            Person kitHarington = personRepo.save(Person.builder().name("Kit Harington").birthYear(1986).countryCode("GB")
                    .biography("Known for playing Jon Snow in Game of Thrones.").photoUrl(BASE + "iGXlJbExWwZmo9sUDsYuzf4Sv4y.jpg").build());
            Person peterDinklage = personRepo.save(Person.builder().name("Peter Dinklage").birthYear(1969).countryCode("US")
                    .biography("Emmy-winning actor known for Tyrion Lannister in Game of Thrones.").photoUrl(BASE + "9CAd7wr8QZyIN0E7nm8v1B6WkGn.jpg").build());
            Person markHamill = personRepo.save(Person.builder().name("Mark Hamill").birthYear(1951).countryCode("US")
                    .biography("Iconic as Luke Skywalker in the Star Wars franchise.").photoUrl(BASE + "zMQ93JTLW8KxusKhOlHFZhih3YQ.jpg").build());
            Person carrieFisher = personRepo.save(Person.builder().name("Carrie Fisher").birthYear(1956).countryCode("US")
                    .biography("Beloved as Princess Leia in the Star Wars franchise.").photoUrl(BASE + "of4yHmryKPy92eeskUQ7MRmjC3l.jpg").build());
            Person adamDriver = personRepo.save(Person.builder().name("Adam Driver").birthYear(1983).countryCode("US")
                    .biography("Known for his role as Kylo Ren in the Star Wars sequel trilogy.").photoUrl(BASE + "fsbGQ1eZFgdsG1XnKlhNSvHsiGo.jpg").build());
            Person carrieAnneMoss = personRepo.save(Person.builder().name("Carrie-Anne Moss").birthYear(1967).countryCode("CA")
                    .biography("Known for her role as Trinity in The Matrix franchise.").photoUrl(BASE + "9msSN9TnF6Ne5cyBwrFZjrjwYbR.jpg").build());
            Person zendaya = personRepo.save(Person.builder().name("Zendaya").birthYear(1996).countryCode("US")
                    .biography("Multi-talented actress known for Dune and Euphoria.").photoUrl(BASE + "3WdOloHpjtjL96uVOhFRRCcYSwq.jpg").build());
            Person tomHardy = personRepo.save(Person.builder().name("Tom Hardy").birthYear(1977).countryCode("GB")
                    .biography("Versatile actor known for intense and physical roles.").photoUrl(BASE + "d81K0RH8UX7tZj49tZaQhZ9ewH.jpg").build());
            Person rayLiotta = personRepo.save(Person.builder().name("Ray Liotta").birthYear(1954).countryCode("US")
                    .biography("Known for his breakthrough role in Goodfellas.").photoUrl(BASE + "rhaCUi04uEXDFvuPM5Drj1AprE6.jpg").build());
            Person alanRickman = personRepo.save(Person.builder().name("Alan Rickman").birthYear(1946).countryCode("GB")
                    .biography("Known for playing iconic villains and complex characters.").photoUrl(BASE + "bVZRMlpjTAO2pJK6v90buFgVbSW.jpg").build());
            Person anaDeArmas = personRepo.save(Person.builder().name("Ana de Armas").birthYear(1988).countryCode("CU")
                    .biography("Rising star known for Knives Out and No Time to Die.").photoUrl(BASE + "3vxvsmYLTf4jnr163SUlBIw51ee.jpg").build());
            Person robinWright = personRepo.save(Person.builder().name("Robin Wright").birthYear(1966).countryCode("US")
                    .biography("Known for Forrest Gump and House of Cards.").photoUrl(BASE + "1p2aSnSkYi0maqqdpzQ73KZSDPO.jpg").build());
            Person shelleyDuvall = personRepo.save(Person.builder().name("Shelley Duvall").birthYear(1949).countryCode("US")
                    .biography("Known for her iconic role in The Shining.").photoUrl(BASE + "gf44Hr3HJuWK7ZMHQKzDNBe0ylI.jpg").build());
            Person eliWallach = personRepo.save(Person.builder().name("Eli Wallach").birthYear(1915).countryCode("US")
                    .biography("Legendary character actor known for The Good, the Bad and the Ugly.").photoUrl(BASE + "egLe8r2PwbTx9ocwS1Zu2vsYC9v.jpg").build());
            Person leeJCobb = personRepo.save(Person.builder().name("Lee J. Cobb").birthYear(1911).countryCode("US")
                    .biography("Powerful character actor known for 12 Angry Men.").photoUrl(BASE + "yxMxBvM0PZwu7YXQamG0kFwt9DZ.jpg").build());
            Person milesTeller = personRepo.save(Person.builder().name("Miles Teller").birthYear(1987).countryCode("US")
                    .biography("Known for Whiplash and Top Gun: Maverick.").photoUrl(BASE + "aciu7YM8fD0BzrrA6cJ5wDKZIA6.jpg").build());
            Person claireDanes = personRepo.save(Person.builder().name("Claire Danes").birthYear(1979).countryCode("US")
                    .biography("Known for Homeland and Terminator 3.").photoUrl(BASE + "vTKKniIwbXWMmnuMmGKHYS41Vif.jpg").build());
            Person samWorthington = personRepo.save(Person.builder().name("Sam Worthington").birthYear(1976).countryCode("AU")
                    .biography("Known for Avatar and Terminator Salvation.").photoUrl(BASE + "mflBcox36s9ZPbsZPVOuhf6axaJ.jpg").build());
            Person andyGarcia = personRepo.save(Person.builder().name("Andy Garcia").birthYear(1956).countryCode("CU")
                    .biography("Known for The Godfather Part III and Ocean's Eleven.").photoUrl(BASE + "9EivXoBlczZcFBet96WOoFbDsfF.jpg").build());
            Person williamSadler = personRepo.save(Person.builder().name("William Sadler").birthYear(1950).countryCode("US")
                    .biography("Known for Die Hard 2 and The Shawshank Redemption.").photoUrl(BASE + "xC9sijoDnjS3oDZ5eszcGKHKAOp.jpg").build());
            Person justinLong = personRepo.save(Person.builder().name("Justin Long").birthYear(1978).countryCode("US")
                    .biography("Known for his role in Live Free or Die Hard.").photoUrl(BASE + "7TGXeHw4o86IBm6xknQotpludXK.jpg").build());
            Person jaiCourtney = personRepo.save(Person.builder().name("Jai Courtney").birthYear(1986).countryCode("AU")
                    .biography("Known for Die Hard and Terminator Genisys.").photoUrl(BASE + "6vEaNwbOKov6yzQx15CdtrqfK3L.jpg").build());
            Person tomSkerritt = personRepo.save(Person.builder().name("Tom Skerritt").birthYear(1933).countryCode("US")
                    .biography("Known for Alien and Top Gun.").photoUrl(BASE + "oWFCyBLm1lsbsbT5Nmx3SPMaqFZ.jpg").build());
            Person arnoldSchwarzenegger = personRepo.save(Person.builder().name("Arnold Schwarzenegger").birthYear(1947).countryCode("AT")
                    .biography("Action legend and former California governor.").photoUrl(BASE + "dgCABuZp2HBehCT84O4WBp7KIoe.jpg").build());
            Person aneurinBarnard = personRepo.save(Person.builder().name("Aneurin Barnard").birthYear(1987).countryCode("GB")
                    .biography("Welsh actor known for Dunkirk and War & Peace.").photoUrl(BASE + "aiuFLvyaxmuglNi3nfQ3oL9qW97.jpg").build());

            // ── Movies ────────────────────────────────────────────────────

            // DRAMA
            Movie shawshank = saveMovie(Movie.builder().title("The Shawshank Redemption").releaseYear(1994).genre(Genre.DRAMA).rating(9.3).runtime(142)
                    .plot("Two imprisoned men bond over years, finding solace and redemption through common decency.")
                    .posterUrl(BASE + "9cqNxx0GxF0bflZmeSMuL5tnGzr.jpg").tmdbId(278).build(),
                List.of(frankDarabont),
                List.of(new CastEntry(timRobbins, "Andy Dufresne"), new CastEntry(morganFreeman, "Ellis 'Red' Redding")));
            Movie godfather = saveMovie(Movie.builder().title("The Godfather").releaseYear(1972).genre(Genre.CRIME).rating(9.2).runtime(175)
                    .plot("The patriarch of a crime dynasty transfers control to his reluctant son.")
                    .posterUrl(BASE + "3bhkrj58Vtu7enYsRolD1fZdja1.jpg").tmdbId(238).build(),
                List.of(francisFordCoppola),
                List.of(new CastEntry(marlonBrando, "Vito Corleone"), new CastEntry(alPacino, "Michael Corleone")));
            saveMovie(Movie.builder().title("The Godfather Part II").releaseYear(1974).genre(Genre.CRIME).rating(9.0).runtime(202)
                    .plot("The early life of Vito Corleone is told while Michael expands and tightens his grip on the family crime syndicate.")
                    .posterUrl(BASE + "hek3koDUyRQk7FIhPXsa6mT2Zc3.jpg").tmdbId(240).build(),
                List.of(francisFordCoppola),
                List.of(new CastEntry(alPacino, "Michael Corleone"), new CastEntry(robertDeNiro, "Young Vito Corleone")));
            saveMovie(Movie.builder().title("The Godfather Part III").releaseYear(1990).genre(Genre.CRIME).rating(7.6).runtime(162)
                    .plot("An aging Michael Corleone seeks to legitimize his family's crime empire while personal tragedy strikes.")
                    .posterUrl(BASE + "lm3pQ2QoQ16pextRsmnUbG2onES.jpg").tmdbId(242).build(),
                List.of(francisFordCoppola),
                List.of(new CastEntry(alPacino, "Michael Corleone"), new CastEntry(andyGarcia, "Vincent Corleone")));
            saveMovie(Movie.builder().title("Forrest Gump").releaseYear(1994).genre(Genre.DRAMA).rating(8.8).runtime(142)
                    .plot("Historical events of the 20th century unfold through the eyes of a simple Alabama man.")
                    .posterUrl(BASE + "saHP97rTPS5eLmrLQEcANmKrsFl.jpg").tmdbId(13).build(),
                List.of(robertZemeckis),
                List.of(new CastEntry(tomHanks, "Forrest Gump"), new CastEntry(robinWright, "Jenny Curran")));
            saveMovie(Movie.builder().title("12 Angry Men").releaseYear(1957).genre(Genre.DRAMA).rating(9.0).runtime(96)
                    .plot("A jury holdout forces his colleagues to reconsider evidence in a murder trial.")
                    .posterUrl(BASE + "2QXLVh32JKaWTjFJU3n8aIxRK9P.jpg").tmdbId(389).build(),
                List.of(sidneyLumet),
                List.of(new CastEntry(henryFonda, "Juror 8"), new CastEntry(leeJCobb, "Juror 3")));

            // ACTION
            saveMovie(Movie.builder().title("Die Hard").releaseYear(1988).genre(Genre.ACTION).rating(8.2).runtime(132)
                    .plot("An NYPD officer tries to save his wife and others taken hostage by terrorists during a Christmas party.")
                    .posterUrl(BASE + "7Bjd8kfmDSOzpmhySpEhkUyK2oH.jpg").tmdbId(562).build(),
                List.of(johnMcTiernan),
                List.of(new CastEntry(bruceWillis, "John McClane"), new CastEntry(alanRickman, "Hans Gruber")));
            saveMovie(Movie.builder().title("Die Hard 2").releaseYear(1990).genre(Genre.ACTION).rating(7.1).runtime(124)
                    .plot("John McClane battles renegade military officers who have seized control of Dulles Airport.")
                    .posterUrl(BASE + "ybki0UWO3OPhaM6MSniuKC7sy1R.jpg").tmdbId(1573).build(),
                List.of(rennyHarlin),
                List.of(new CastEntry(bruceWillis, "John McClane"), new CastEntry(williamSadler, "Col. Stuart")));
            saveMovie(Movie.builder().title("Die Hard with a Vengeance").releaseYear(1995).genre(Genre.ACTION).rating(7.7).runtime(128)
                    .plot("John McClane and a reluctant civilian are taken on a wild chase through New York City by a mysterious bomber.")
                    .posterUrl(BASE + "buqmCdFQEWwEpL3agGgg2GVjN2d.jpg").tmdbId(1572).build(),
                List.of(johnMcTiernan),
                List.of(new CastEntry(bruceWillis, "John McClane"), new CastEntry(samuelLJackson, "Zeus Carver")));
            saveMovie(Movie.builder().title("Live Free or Die Hard").releaseYear(2007).genre(Genre.ACTION).rating(7.2).runtime(128)
                    .plot("John McClane takes on an Internet-based terrorist organization that is shutting down the USA.")
                    .posterUrl(BASE + "31TT47YjBl7a7uvJ3ff1nrirXhP.jpg").tmdbId(1571).build(),
                List.of(lenWiseman),
                List.of(new CastEntry(bruceWillis, "John McClane"), new CastEntry(justinLong, "Matt Farrell")));
            saveMovie(Movie.builder().title("A Good Day to Die Hard").releaseYear(2013).genre(Genre.ACTION).rating(5.3).runtime(97)
                    .plot("John McClane travels to Russia to help his estranged son, unaware that Jack is a CIA operative.")
                    .posterUrl(BASE + "qJ0csDXAVFMsNn0cRcjy6W6PxAK.jpg").tmdbId(47964).build(),
                List.of(johnMoore),
                List.of(new CastEntry(bruceWillis, "John McClane"), new CastEntry(jaiCourtney, "Jack McClane")));
            saveMovie(Movie.builder().title("Top Gun: Maverick").releaseYear(2022).genre(Genre.ACTION).rating(8.3).runtime(130)
                    .plot("After thirty years, Maverick is still pushing the envelope as a top naval aviator.")
                    .posterUrl(BASE + "62HCnUTziyWcpDaBO2i1DX17ljH.jpg").tmdbId(361743).build(),
                List.of(josephKosinski),
                List.of(new CastEntry(tomCruise, "Pete 'Maverick' Mitchell"), new CastEntry(milesTeller, "Bradley 'Rooster' Bradshaw")));
            saveMovie(Movie.builder().title("Avengers: Endgame").releaseYear(2019).genre(Genre.ACTION).rating(8.4).runtime(181)
                    .plot("The Avengers assemble to undo Thanos's actions and restore order to the universe.")
                    .posterUrl(BASE + "ulzhLuWrPK07P1YkdWQLZnQh1JL.jpg").tmdbId(299534).build(),
                List.of(anthonyRusso, joeRusso),
                List.of(new CastEntry(scarlettJohansson, "Natasha Romanoff"), new CastEntry(chadwickBoseman, "T'Challa")));

            // SCI-FI
            saveMovie(Movie.builder().title("The Terminator").releaseYear(1984).genre(Genre.SCIFI).rating(8.1).runtime(107)
                    .plot("A cyborg is sent from the future to kill Sarah Connor, whose son will lead humanity against machines.")
                    .posterUrl(BASE + "qvktm0BHcnmDpul4Hz01GIazWPr.jpg").tmdbId(218).build(),
                List.of(jamesCameron),
                List.of(new CastEntry(arnoldSchwarzenegger, "T-800"), new CastEntry(lindaHamilton, "Sarah Connor")));
            saveMovie(Movie.builder().title("Terminator 2: Judgment Day").releaseYear(1991).genre(Genre.SCIFI).rating(8.6).runtime(137)
                    .plot("A reprogrammed Terminator returns to protect John Connor from a more advanced and deadly T-1000.")
                    .posterUrl(BASE + "jFTVD4XoWQTcg7wdyJKa8PEds5q.jpg").tmdbId(280).build(),
                List.of(jamesCameron),
                List.of(new CastEntry(arnoldSchwarzenegger, "T-800"), new CastEntry(lindaHamilton, "Sarah Connor")));
            saveMovie(Movie.builder().title("Terminator 3: Rise of the Machines").releaseYear(2003).genre(Genre.SCIFI).rating(6.3).runtime(109)
                    .plot("A new Terminator is sent back in time to eliminate the future leaders of the Resistance.")
                    .posterUrl(BASE + "nvsoLAclNfpyJSp73TiGKwZoqJW.jpg").tmdbId(296).build(),
                List.of(jonathanMostow),
                List.of(new CastEntry(arnoldSchwarzenegger, "T-850"), new CastEntry(claireDanes, "Kate Brewster")));
            saveMovie(Movie.builder().title("Terminator Salvation").releaseYear(2009).genre(Genre.SCIFI).rating(6.5).runtime(115)
                    .plot("In a post-apocalyptic world, John Connor leads the resistance against Skynet's machine army.")
                    .posterUrl(BASE + "gw6JhlekZgtKUFlDTezq3j5JEPK.jpg").tmdbId(534).build(),
                List.of(mcg),
                List.of(new CastEntry(christianBale, "John Connor"), new CastEntry(samWorthington, "Marcus Wright")));
            saveMovie(Movie.builder().title("Terminator Genisys").releaseYear(2015).genre(Genre.SCIFI).rating(6.3).runtime(126)
                    .plot("When Kyle Reese is sent back to 1984, an unexpected turn of events creates a fractured timeline.")
                    .posterUrl(BASE + "oZRVDpNtmHk8M1VYy1aeOWUXgbC.jpg").tmdbId(87101).build(),
                List.of(alanTaylor),
                List.of(new CastEntry(arnoldSchwarzenegger, "Guardian"), new CastEntry(emiliaClarke, "Sarah Connor")));
            saveMovie(Movie.builder().title("Terminator: Dark Fate").releaseYear(2019).genre(Genre.SCIFI).rating(6.2).runtime(128)
                    .plot("Sarah Connor and a hybrid Terminator must protect a young woman from an advanced Terminator sent from the future.")
                    .posterUrl(BASE + "vqzNJRH4YyquRiWxCCOH0aXggHI.jpg").tmdbId(290859).build(),
                List.of(timMiller),
                List.of(new CastEntry(arnoldSchwarzenegger, "Carl"), new CastEntry(lindaHamilton, "Sarah Connor")));
            saveMovie(Movie.builder().title("Star Wars: Episode IV - A New Hope").releaseYear(1977).genre(Genre.SCIFI).rating(8.6).runtime(121)
                    .plot("Luke Skywalker joins forces with a Jedi Knight, a rogue pilot, a Wookiee, and two droids to free the galaxy.")
                    .posterUrl(BASE + "6FfCtAuVAW8XJjZ7eWeLibRLWTw.jpg").tmdbId(11).build(),
                List.of(georgeLucas),
                List.of(new CastEntry(harrisonFord, "Han Solo"), new CastEntry(markHamill, "Luke Skywalker"), new CastEntry(carrieFisher, "Leia Organa")));
            saveMovie(Movie.builder().title("Star Wars: Episode V - The Empire Strikes Back").releaseYear(1980).genre(Genre.SCIFI).rating(8.7).runtime(124)
                    .plot("After the Rebels are overpowered by the Empire, Luke begins Jedi training with Yoda while his friends are pursued.")
                    .posterUrl(BASE + "nNAeTmF4CtdSgMDplXTDPOpYzsX.jpg").tmdbId(1891).build(),
                List.of(irvinKershner),
                List.of(new CastEntry(harrisonFord, "Han Solo"), new CastEntry(markHamill, "Luke Skywalker"), new CastEntry(carrieFisher, "Leia Organa")));
            saveMovie(Movie.builder().title("Star Wars: Episode VI - Return of the Jedi").releaseYear(1983).genre(Genre.SCIFI).rating(8.3).runtime(131)
                    .plot("Luke Skywalker attempts to bring his father back from the dark side while the Rebels attack the Empire.")
                    .posterUrl(BASE + "jQYlydvHm3kUix1f8prMucrplhm.jpg").tmdbId(1892).build(),
                List.of(richardMarquand),
                List.of(new CastEntry(harrisonFord, "Han Solo"), new CastEntry(markHamill, "Luke Skywalker"), new CastEntry(carrieFisher, "Leia Organa")));
            saveMovie(Movie.builder().title("Star Wars: Episode I - The Phantom Menace").releaseYear(1999).genre(Genre.SCIFI).rating(6.5).runtime(136)
                    .plot("Two Jedi Knights escape a hostile blockade to find allies and battle a growing darkness.")
                    .posterUrl(BASE + "6wkfovpn7Eq8dYNKaG5PY3q2oq6.jpg").tmdbId(1893).build(),
                List.of(georgeLucas),
                List.of(new CastEntry(liamNeeson, "Qui-Gon Jinn"), new CastEntry(ewanMcGregor, "Obi-Wan Kenobi"), new CastEntry(nataliePortman, "Padmé Amidala")));
            saveMovie(Movie.builder().title("Star Wars: Episode II - Attack of the Clones").releaseYear(2002).genre(Genre.SCIFI).rating(6.6).runtime(142)
                    .plot("Anakin Skywalker shares a forbidden romance with Padmé while Obi-Wan investigates a mysterious clone army.")
                    .posterUrl(BASE + "oZNPzxqM2s5DyVWab09NTQScDQt.jpg").tmdbId(1894).build(),
                List.of(georgeLucas),
                List.of(new CastEntry(ewanMcGregor, "Obi-Wan Kenobi"), new CastEntry(nataliePortman, "Padmé Amidala")));
            saveMovie(Movie.builder().title("Star Wars: Episode III - Revenge of the Sith").releaseYear(2005).genre(Genre.SCIFI).rating(7.5).runtime(140)
                    .plot("As the Clone Wars near its end, Anakin Skywalker is seduced to the dark side of the Force.")
                    .posterUrl(BASE + "xfSAoBEm9MNBjmlNcDYLvLSMlnq.jpg").tmdbId(1895).build(),
                List.of(georgeLucas),
                List.of(new CastEntry(ewanMcGregor, "Obi-Wan Kenobi"), new CastEntry(nataliePortman, "Padmé Amidala")));
            saveMovie(Movie.builder().title("Star Wars: Episode VII - The Force Awakens").releaseYear(2015).genre(Genre.SCIFI).rating(7.9).runtime(138)
                    .plot("A new threat rises as a group of heroes emerges to face the dark side of the Force.")
                    .posterUrl(BASE + "wqnLdwVXoBjKibFRR5U3y0aDUhs.jpg").tmdbId(140607).build(),
                List.of(jJAbrams),
                List.of(new CastEntry(harrisonFord, "Han Solo"), new CastEntry(daisyRidley, "Rey"), new CastEntry(carrieFisher, "Leia Organa"), new CastEntry(adamDriver, "Kylo Ren")));
            saveMovie(Movie.builder().title("Star Wars: Episode VIII - The Last Jedi").releaseYear(2017).genre(Genre.SCIFI).rating(7.0).runtime(152)
                    .plot("Rey develops her newly discovered abilities with Luke Skywalker while the Resistance faces the First Order.")
                    .posterUrl(BASE + "kOVEVeg59E0wsnXmF9nrh6OmWII.jpg").tmdbId(181808).build(),
                List.of(rianJohnson),
                List.of(new CastEntry(daisyRidley, "Rey"), new CastEntry(markHamill, "Luke Skywalker"), new CastEntry(adamDriver, "Kylo Ren")));
            saveMovie(Movie.builder().title("Star Wars: Episode IX - The Rise of Skywalker").releaseYear(2019).genre(Genre.SCIFI).rating(6.5).runtime(142)
                    .plot("The surviving Resistance faces the First Order as the Skywalker saga comes to its end.")
                    .posterUrl(BASE + "db32LaOibwEliAmSL2jjDF6oDdj.jpg").tmdbId(181812).build(),
                List.of(jJAbrams),
                List.of(new CastEntry(daisyRidley, "Rey"), new CastEntry(adamDriver, "Kylo Ren")));
            Movie inception = saveMovie(Movie.builder().title("Inception").releaseYear(2010).genre(Genre.SCIFI).rating(8.8).runtime(148)
                    .plot("A thief who steals corporate secrets through dream-sharing is given the task of planting an idea.")
                    .posterUrl(BASE + "xlaY2zyzMfkhk0HSC5VUwzoZPU1.jpg").tmdbId(27205).build(),
                List.of(christopherNolan),
                List.of(new CastEntry(leonardoDiCaprio, "Dom Cobb"), new CastEntry(tomHardy, "Eames")));
            saveMovie(Movie.builder().title("Interstellar").releaseYear(2014).genre(Genre.SCIFI).rating(8.6).runtime(169)
                    .plot("A team of explorers travel through a wormhole in space to ensure humanity's survival.")
                    .posterUrl(BASE + "yQvGrMoipbRoddT0ZR8tPoR7NfX.jpg").tmdbId(157336).build(),
                List.of(christopherNolan),
                List.of(new CastEntry(matthewMcConaughey, "Cooper"), new CastEntry(jessicaChastain, "Murph")));
            Movie theMatrix = saveMovie(Movie.builder().title("The Matrix").releaseYear(1999).genre(Genre.SCIFI).rating(8.7).runtime(136)
                    .plot("A computer hacker learns that his world is a simulation and joins a rebellion.")
                    .posterUrl(BASE + "p96dm7sCMn4VYAStA6siNz30G1r.jpg").tmdbId(603).build(),
                List.of(lanaWachowski, lillyWachowski),
                List.of(new CastEntry(keanuReeves, "Neo"), new CastEntry(carrieAnneMoss, "Trinity")));
            saveMovie(Movie.builder().title("The Matrix Reloaded").releaseYear(2003).genre(Genre.SCIFI).rating(7.2).runtime(138)
                    .plot("Neo and his allies race against time before the machines discover the city of Zion.")
                    .posterUrl(BASE + "aA5qHS0FbSXO8PxcxUIHbDrJyuh.jpg").tmdbId(604).build(),
                List.of(lanaWachowski, lillyWachowski),
                List.of(new CastEntry(keanuReeves, "Neo"), new CastEntry(carrieAnneMoss, "Trinity")));
            saveMovie(Movie.builder().title("The Matrix Revolutions").releaseYear(2003).genre(Genre.SCIFI).rating(6.8).runtime(129)
                    .plot("The machine city is humanity's last hope as Neo races to stop the war between machines and man.")
                    .posterUrl(BASE + "bkkS61w94ZVMNVd8KEyyJl2tnY5.jpg").tmdbId(605).build(),
                List.of(lanaWachowski, lillyWachowski),
                List.of(new CastEntry(keanuReeves, "Neo"), new CastEntry(carrieAnneMoss, "Trinity")));
            saveMovie(Movie.builder().title("The Matrix Resurrections").releaseYear(2021).genre(Genre.SCIFI).rating(5.7).runtime(148)
                    .plot("Neo and Trinity must choose to follow the white rabbit once more in a new matrix.")
                    .posterUrl(BASE + "8c4a8kE7PizaGQQnditMmI1xbRp.jpg").tmdbId(624860).build(),
                List.of(lanaWachowski),
                List.of(new CastEntry(keanuReeves, "Neo"), new CastEntry(carrieAnneMoss, "Trinity")));
            saveMovie(Movie.builder().title("Dune").releaseYear(2021).genre(Genre.SCIFI).rating(7.9).runtime(155)
                    .plot("The son of a noble family is entrusted with the protection of a dangerous desert planet.")
                    .posterUrl(BASE + "gDzOcq0pfeCeqMBwKIJlSmQpjkZ.jpg").tmdbId(438631).build(),
                List.of(denisVilleneuve),
                List.of(new CastEntry(timotheeChalamet, "Paul Atreides"), new CastEntry(zendaya, "Chani")));
            saveMovie(Movie.builder().title("Dune: Part Two").releaseYear(2024).genre(Genre.SCIFI).rating(8.5).runtime(167)
                    .plot("Paul Atreides unites with the Fremen on a warpath of revenge against his family's destroyers.")
                    .posterUrl(BASE + "6izwz7rsy95ARzTR3poZ8H6c5pp.jpg").tmdbId(693134).build(),
                List.of(denisVilleneuve),
                List.of(new CastEntry(timotheeChalamet, "Paul Atreides"), new CastEntry(zendaya, "Chani")));

            // THRILLER / CRIME
            saveMovie(Movie.builder().title("Goodfellas").releaseYear(1990).genre(Genre.CRIME).rating(8.7).runtime(145)
                    .plot("Henry Hill's rise and fall in the mob, covering his relationships with partners and his wife.")
                    .posterUrl(BASE + "9OkCLM73MIU2CrKZbqiT8Ln1wY2.jpg").tmdbId(769).build(),
                List.of(martinScorsese),
                List.of(new CastEntry(rayLiotta, "Henry Hill"), new CastEntry(robertDeNiro, "Jimmy Conway")));
            saveMovie(Movie.builder().title("Se7en").releaseYear(1995).genre(Genre.THRILLER).rating(8.6).runtime(127)
                    .plot("Two detectives hunt a serial killer who uses the seven deadly sins as his motives.")
                    .posterUrl(BASE + "191nKfP0ehp3uIvWqgPbFmI4lv9.jpg").tmdbId(807).build(),
                List.of(davidFincher),
                List.of(new CastEntry(bradPitt, "David Mills"), new CastEntry(morganFreeman, "William Somerset"), new CastEntry(kevinSpacey, "John Doe")));
            saveMovie(Movie.builder().title("The Departed").releaseYear(2006).genre(Genre.CRIME).rating(8.5).runtime(151)
                    .plot("An undercover cop and a mole in the police attempt to identify each other inside an Irish gang.")
                    .posterUrl(BASE + "nT97ifVT2J1yMQmeq20Qblg61T.jpg").tmdbId(1422).build(),
                List.of(martinScorsese),
                List.of(new CastEntry(leonardoDiCaprio, "Billy Costigan"), new CastEntry(jackNicholson, "Frank Costello")));
            saveMovie(Movie.builder().title("Knives Out").releaseYear(2019).genre(Genre.MYSTERY).rating(7.9).runtime(130)
                    .plot("A detective investigates the death of the patriarch of an eccentric, combative family.")
                    .posterUrl(BASE + "pThyQovXQrw2m0s9x82twj48Jq4.jpg").tmdbId(546554).build(),
                List.of(rianJohnson),
                List.of(new CastEntry(danielCraig, "Benoit Blanc"), new CastEntry(anaDeArmas, "Marta Cabrera")));
            saveMovie(Movie.builder().title("The Silence of the Lambs").releaseYear(1991).genre(Genre.THRILLER).rating(8.6).runtime(118)
                    .plot("A young FBI cadet must receive the help of an incarcerated cannibal to catch another serial killer.")
                    .posterUrl(BASE + "uS9m8OBk1A8eM9I042bx8XXpqAq.jpg").tmdbId(274).build(),
                List.of(jonathanDemme),
                List.of(new CastEntry(anthonyHopkins, "Hannibal Lecter"), new CastEntry(jodieFoster, "Clarice Starling")));
            saveMovie(Movie.builder().title("Taxi Driver").releaseYear(1976).genre(Genre.CRIME).rating(8.3).runtime(114)
                    .plot("A mentally unstable Vietnam War veteran works as a night-time taxi driver in New York City.")
                    .posterUrl(BASE + "ekstpH614fwDX8DUln1a2Opz0N8.jpg").tmdbId(103).build(),
                List.of(martinScorsese),
                List.of(new CastEntry(robertDeNiro, "Travis Bickle"), new CastEntry(jodieFoster, "Iris")));

            // HORROR
            saveMovie(Movie.builder().title("A Quiet Place").releaseYear(2018).genre(Genre.HORROR).rating(7.5).runtime(90)
                    .plot("A family is forced to live in near silence while hiding from creatures with ultra-sensitive hearing.")
                    .posterUrl(BASE + "nAU74GmpUk7t5iklEp3bufwDq4n.jpg").tmdbId(447332).build(),
                List.of(johnKrasinski),
                List.of(new CastEntry(emilyBlunt, "Evelyn Abbott"), new CastEntry(johnKrasinski, "Lee Abbott")));
            saveMovie(Movie.builder().title("The Shining").releaseYear(1980).genre(Genre.HORROR).rating(8.4).runtime(146)
                    .plot("A family heads to an isolated hotel for winter where a sinister presence drives the father to violence.")
                    .posterUrl(BASE + "uAR0AWqhQL1hQa69UDEbb2rE5Wx.jpg").tmdbId(694).build(),
                List.of(stanleyKubrick),
                List.of(new CastEntry(jackNicholson, "Jack Torrance"), new CastEntry(shelleyDuvall, "Wendy Torrance")));
            saveMovie(Movie.builder().title("Alien").releaseYear(1979).genre(Genre.HORROR).rating(8.5).runtime(117)
                    .plot("After receiving an unknown transmission, a space crew picks up a hostile stowaway.")
                    .posterUrl(BASE + "vfrQk5IPloGg1v9Rzbh2Eg3VGyM.jpg").tmdbId(348).build(),
                List.of(ridleyScott),
                List.of(new CastEntry(sigourneyWeaver, "Ellen Ripley"), new CastEntry(tomSkerritt, "Arthur Dallas")));
            saveMovie(Movie.builder().title("Psycho").releaseYear(1960).genre(Genre.HORROR).rating(8.5).runtime(109)
                    .plot("A secretary on the run embezzles money and ends up at the Bates Motel.")
                    .posterUrl(BASE + "yz4QVqPx3h1hD1DfqqQkCq3rmxW.jpg").tmdbId(539).build(),
                List.of(alfredHitchcock),
                List.of(new CastEntry(anthonyPerkins, "Norman Bates"), new CastEntry(janetLeigh, "Marion Crane")));

            // ROMANCE
            saveMovie(Movie.builder().title("Casablanca").releaseYear(1942).genre(Genre.ROMANCE).rating(8.5).runtime(102)
                    .plot("A cynical cafe owner struggles to decide whether to help his former lover and her husband escape.")
                    .posterUrl(BASE + "lGCEKlJo2CnWydQj7aamY7s1S7Q.jpg").tmdbId(289).build(),
                List.of(michaelCurtiz),
                List.of(new CastEntry(humphreyBogart, "Rick Blaine"), new CastEntry(ingridBergman, "Ilsa Lund")));

            // WAR
            saveMovie(Movie.builder().title("Dunkirk").releaseYear(2017).genre(Genre.WAR).rating(7.9).runtime(106)
                    .plot("Allied soldiers surrounded by the German Army are evacuated during a fierce battle.")
                    .posterUrl(BASE + "b4Oe15CGLL61Ped0RAS9JpqdmCt.jpg").tmdbId(374720).build(),
                List.of(christopherNolan),
                List.of(new CastEntry(aneurinBarnard, "Gibson"), new CastEntry(tomHardy, "Farrier")));

            // FANTASY
            saveMovie(Movie.builder().title("The Lord of the Rings: The Fellowship of the Ring").releaseYear(2001).genre(Genre.FANTASY).rating(8.8).runtime(178)
                    .plot("A Hobbit and eight companions set out to destroy the One Ring and save Middle-earth.")
                    .posterUrl(BASE + "6oom5QYQ2yQTMJIbnvbkBL9cHo6.jpg").tmdbId(120).build(),
                List.of(peterJackson),
                List.of(new CastEntry(elijahWood, "Frodo Baggins"), new CastEntry(ianMcKellen, "Gandalf"), new CastEntry(cateBlanchett, "Galadriel")));
            saveMovie(Movie.builder().title("The Lord of the Rings: The Two Towers").releaseYear(2002).genre(Genre.FANTASY).rating(8.8).runtime(179)
                    .plot("The divided fellowship makes a stand against Sauron while Frodo edges closer to Mordor.")
                    .posterUrl(BASE + "5VTN0pR8gcqV3EPUHHfMGnJYN9L.jpg").tmdbId(121).build(),
                List.of(peterJackson),
                List.of(new CastEntry(elijahWood, "Frodo Baggins"), new CastEntry(ianMcKellen, "Gandalf")));
            saveMovie(Movie.builder().title("The Lord of the Rings: The Return of the King").releaseYear(2003).genre(Genre.FANTASY).rating(9.0).runtime(201)
                    .plot("Gandalf and Aragorn lead the World of Men against Sauron's army to draw his gaze from Frodo.")
                    .posterUrl(BASE + "rCzpDGLbOoPwLjy3OAm5NUPOTrC.jpg").tmdbId(122).build(),
                List.of(peterJackson),
                List.of(new CastEntry(elijahWood, "Frodo Baggins"), new CastEntry(ianMcKellen, "Gandalf"), new CastEntry(cateBlanchett, "Galadriel")));

            // WESTERN
            saveMovie(Movie.builder().title("The Good, the Bad and the Ugly").releaseYear(1966).genre(Genre.WESTERN).rating(8.8).runtime(178)
                    .plot("A bounty hunting scam joins two men in an uneasy alliance to find a fortune in gold.")
                    .posterUrl(BASE + "bX2xnavhMYjWDoZp1VM6VnU1xwe.jpg").tmdbId(429).build(),
                List.of(sergioLeone),
                List.of(new CastEntry(clintEastwood, "Blondie"), new CastEntry(eliWallach, "Tuco")));
            saveMovie(Movie.builder().title("Unforgiven").releaseYear(1992).genre(Genre.WESTERN).rating(8.2).runtime(130)
                    .plot("Retired gunslinger William Munny reluctantly takes on one last job, with the help of his old partner and a young man.")
                    .posterUrl(BASE + "54roTwbX9fltg85zjsmrooXAs12.jpg").tmdbId(33).build(),
                List.of(clintEastwood),
                List.of(new CastEntry(clintEastwood, "William Munny"), new CastEntry(morganFreeman, "Ned Logan")));
            saveMovie(Movie.builder().title("Million Dollar Baby").releaseYear(2004).genre(Genre.DRAMA).rating(8.1).runtime(132)
                    .plot("A determined woman works with a hardened boxing trainer to become a professional fighter.")
                    .posterUrl(BASE + "jcfEqKdWF1zeyvECPqp3mkWLct2.jpg").tmdbId(70).build(),
                List.of(clintEastwood),
                List.of(new CastEntry(clintEastwood, "Frankie Dunn"), new CastEntry(morganFreeman, "Eddie 'Scrap-Iron' Dupris")));
            saveMovie(Movie.builder().title("Gran Torino").releaseYear(2008).genre(Genre.DRAMA).rating(8.1).runtime(116)
                    .plot("A disgruntled Korean War veteran sets out to reform his young neighbor after he tries to steal his prized Ford Gran Torino.")
                    .posterUrl(BASE + "zUybYvxWdAJy5hhYovsXtHSWI1l.jpg").tmdbId(13223).build(),
                List.of(clintEastwood),
                List.of(new CastEntry(clintEastwood, "Walt Kowalski")));

            // ── TV Shows ──────────────────────────────────────────────────
            TvShow friends = saveTvShow(TvShow.builder().title("Friends").genre(Genre.COMEDY).rating(8.9)
                    .plot("Six friends navigate life, love, and careers in New York City.")
                    .posterUrl(BASE + "2koX1xLkpTQM4IZebYvKysFW1Nh.jpg").tmdbId(1668).startYear(1994).endYear(2004).seasons(10).build(),
                List.of(davidCrane, martaKauffman),
                List.of(new CastEntry(jenniferAniston, "Rachel Green"), new CastEntry(courteneyCox, "Monica Geller"),
                        new CastEntry(lisaKudrow, "Phoebe Buffay"), new CastEntry(mattLeBlanc, "Joey Tribbiani"),
                        new CastEntry(matthewPerry, "Chandler Bing"), new CastEntry(davidSchwimmer, "Ross Geller")));
            episodeRepo.saveAll(List.of(
                Episode.builder().tvShow(friends).seasonNumber(1).episodeNumber(1).runtime(22).airYear(1994)
                    .title("The One Where Monica Gets a Roommate").overview("Rachel joins the group after leaving her fiancé at the altar.").build(),
                Episode.builder().tvShow(friends).seasonNumber(1).episodeNumber(7).runtime(22).airYear(1994)
                    .title("The One with the Blackout").overview("A city-wide blackout traps Chandler in an ATM vestibule with a model.").build(),
                Episode.builder().tvShow(friends).seasonNumber(1).episodeNumber(24).runtime(22).airYear(1995)
                    .title("The One Where Rachel Finds Out").overview("Rachel discovers Ross's feelings for her right as he leaves for China.").build(),
                Episode.builder().tvShow(friends).seasonNumber(2).episodeNumber(1).runtime(22).airYear(1995)
                    .title("The One with Ross's New Girlfriend").overview("Ross returns from China with a new girlfriend.").build(),
                Episode.builder().tvShow(friends).seasonNumber(2).episodeNumber(14).runtime(22).airYear(1996)
                    .title("The One with the Prom Video").overview("An old video reveals Ross was ready to be Rachel's prom date.").build(),
                Episode.builder().tvShow(friends).seasonNumber(2).episodeNumber(24).runtime(22).airYear(1996)
                    .title("The One with Barry and Mindy's Wedding").overview("Rachel is a bridesmaid at her ex's wedding.").build(),
                Episode.builder().tvShow(friends).seasonNumber(3).episodeNumber(1).runtime(22).airYear(1996)
                    .title("The One with the Princess Leia Fantasy").overview("Ross shares a secret fantasy with Rachel.").build(),
                Episode.builder().tvShow(friends).seasonNumber(3).episodeNumber(16).runtime(22).airYear(1997)
                    .title("The One the Morning After").overview("Ross and Rachel have a dramatic breakup.").build(),
                Episode.builder().tvShow(friends).seasonNumber(4).episodeNumber(1).runtime(22).airYear(1997)
                    .title("The One with the Jellyfish").overview("The gang reunites after summer apart.").build(),
                Episode.builder().tvShow(friends).seasonNumber(4).episodeNumber(24).runtime(44).airYear(1998)
                    .title("The One with Ross's Wedding").overview("Ross says the wrong name at the altar.").build(),
                Episode.builder().tvShow(friends).seasonNumber(5).episodeNumber(1).runtime(22).airYear(1998)
                    .title("The One After Ross Says Rachel").overview("The aftermath of Ross's wedding disaster.").build(),
                Episode.builder().tvShow(friends).seasonNumber(5).episodeNumber(14).runtime(22).airYear(1999)
                    .title("The One Where Everybody Finds Out").overview("Everyone discovers Chandler and Monica's relationship.").build(),
                Episode.builder().tvShow(friends).seasonNumber(6).episodeNumber(1).runtime(22).airYear(1999)
                    .title("The One After Vegas").overview("The gang returns from Las Vegas.").build(),
                Episode.builder().tvShow(friends).seasonNumber(6).episodeNumber(25).runtime(44).airYear(2000)
                    .title("The One with the Proposal").overview("Chandler proposes to Monica.").build(),
                Episode.builder().tvShow(friends).seasonNumber(7).episodeNumber(1).runtime(22).airYear(2000)
                    .title("The One with Monica's Thunder").overview("Monica and Chandler announce their engagement.").build(),
                Episode.builder().tvShow(friends).seasonNumber(7).episodeNumber(24).runtime(44).airYear(2001)
                    .title("The One with Chandler and Monica's Wedding").overview("Chandler and Monica finally get married.").build(),
                Episode.builder().tvShow(friends).seasonNumber(8).episodeNumber(1).runtime(22).airYear(2001)
                    .title("The One After I Do").overview("The aftermath of the wedding and Rachel's pregnancy reveal.").build(),
                Episode.builder().tvShow(friends).seasonNumber(8).episodeNumber(24).runtime(44).airYear(2002)
                    .title("The One Where Rachel Has a Baby").overview("Rachel gives birth to Emma.").build(),
                Episode.builder().tvShow(friends).seasonNumber(9).episodeNumber(1).runtime(22).airYear(2002)
                    .title("The One Where No One Proposes").overview("Joey's accidental proposal causes confusion.").build(),
                Episode.builder().tvShow(friends).seasonNumber(9).episodeNumber(23).runtime(44).airYear(2003)
                    .title("The One in Barbados").overview("The gang travels to Barbados for a conference.").build(),
                Episode.builder().tvShow(friends).seasonNumber(10).episodeNumber(1).runtime(22).airYear(2003)
                    .title("The One After Joey and Rachel Kiss").overview("The fallout from Joey and Rachel's kiss.").build(),
                Episode.builder().tvShow(friends).seasonNumber(10).episodeNumber(17).runtime(44).airYear(2004)
                    .title("The Last One").overview("Monica and Chandler prepare to move; Ross races to win Rachel back.").build()));

            TvShow seinfeld = saveTvShow(TvShow.builder().title("Seinfeld").genre(Genre.COMEDY).rating(8.8)
                    .plot("A stand-up comedian and his neurotic friends deal with the mundane aspects of everyday life in New York.")
                    .posterUrl(BASE + "aCw8ONfyz3AhngVQa1E2Ss4KSUQ.jpg").tmdbId(1400).startYear(1989).endYear(1998).seasons(9).build(),
                List.of(larryDavid, jerrySeinfeld),
                List.of(new CastEntry(jerrySeinfeld, "Jerry Seinfeld"), new CastEntry(juliaLouisDreyfus, "Elaine Benes"),
                        new CastEntry(jasonAlexander, "George Costanza"), new CastEntry(michaelRichards, "Cosmo Kramer")));
            episodeRepo.saveAll(List.of(
                Episode.builder().tvShow(seinfeld).seasonNumber(1).episodeNumber(1).runtime(23).airYear(1989)
                    .title("The Seinfeld Chronicles").overview("Jerry tries to figure out what a woman's behavior means.").build(),
                Episode.builder().tvShow(seinfeld).seasonNumber(2).episodeNumber(1).runtime(23).airYear(1991)
                    .title("The Ex-Girlfriend").overview("George tries to retrieve a book from his ex-girlfriend.").build(),
                Episode.builder().tvShow(seinfeld).seasonNumber(3).episodeNumber(1).runtime(23).airYear(1991)
                    .title("The Note").overview("The gang tries to get a massage covered by insurance.").build(),
                Episode.builder().tvShow(seinfeld).seasonNumber(3).episodeNumber(23).runtime(23).airYear(1992)
                    .title("The Keys").overview("Kramer moves to Hollywood to pursue acting.").build(),
                Episode.builder().tvShow(seinfeld).seasonNumber(4).episodeNumber(1).runtime(23).airYear(1992)
                    .title("The Trip").overview("Jerry and George fly to L.A. to find Kramer.").build(),
                Episode.builder().tvShow(seinfeld).seasonNumber(4).episodeNumber(11).runtime(23).airYear(1992)
                    .title("The Contest").overview("The gang makes a bet to see who can go the longest without self-gratification.").build(),
                Episode.builder().tvShow(seinfeld).seasonNumber(4).episodeNumber(20).runtime(23).airYear(1993)
                    .title("The Junior Mint").overview("Kramer accidentally drops a Junior Mint into a patient during surgery.").build(),
                Episode.builder().tvShow(seinfeld).seasonNumber(5).episodeNumber(1).runtime(23).airYear(1993)
                    .title("The Mango").overview("Jerry learns Elaine has been faking it during their relationship.").build(),
                Episode.builder().tvShow(seinfeld).seasonNumber(5).episodeNumber(21).runtime(23).airYear(1994)
                    .title("The Opposite").overview("George decides to do the opposite of every instinct he has.").build(),
                Episode.builder().tvShow(seinfeld).seasonNumber(6).episodeNumber(1).runtime(23).airYear(1994)
                    .title("The Chaperone").overview("Jerry dates a Miss America contestant.").build(),
                Episode.builder().tvShow(seinfeld).seasonNumber(6).episodeNumber(12).runtime(23).airYear(1995)
                    .title("The Label Maker").overview("Jerry is given a label maker as a re-gift.").build(),
                Episode.builder().tvShow(seinfeld).seasonNumber(7).episodeNumber(1).runtime(23).airYear(1995)
                    .title("The Engagement").overview("George decides to propose to his ex-girlfriend Susan.").build(),
                Episode.builder().tvShow(seinfeld).seasonNumber(7).episodeNumber(24).runtime(23).airYear(1996)
                    .title("The Invitations").overview("Susan dies from licking toxic wedding invitation envelopes.").build(),
                Episode.builder().tvShow(seinfeld).seasonNumber(8).episodeNumber(1).runtime(23).airYear(1996)
                    .title("The Foundation").overview("The gang deals with Susan's death and a new foundation in her name.").build(),
                Episode.builder().tvShow(seinfeld).seasonNumber(8).episodeNumber(22).runtime(23).airYear(1997)
                    .title("The Summer of George").overview("George plans to make the most of his severance pay.").build(),
                Episode.builder().tvShow(seinfeld).seasonNumber(9).episodeNumber(1).runtime(23).airYear(1997)
                    .title("The Butter Shave").overview("Kramer starts shaving with butter.").build(),
                Episode.builder().tvShow(seinfeld).seasonNumber(9).episodeNumber(23).runtime(75).airYear(1998)
                    .title("The Finale").overview("Jerry, George, Elaine, and Kramer are put on trial for violating a Good Samaritan law.").build()));

            TvShow got = saveTvShow(TvShow.builder().title("Game of Thrones").genre(Genre.FANTASY).rating(9.2)
                    .plot("Nine noble families fight for control over the mythical lands of Westeros while an ancient enemy returns.")
                    .posterUrl(BASE + "1XS1oqL89opfnbLl8WnZY1O1uJx.jpg").tmdbId(1399).startYear(2011).endYear(2019).seasons(8).build(),
                List.of(davidBenioff, dBWeiss),
                List.of(new CastEntry(emiliaClarke, "Daenerys Targaryen"), new CastEntry(kitHarington, "Jon Snow"), new CastEntry(peterDinklage, "Tyrion Lannister")));
            episodeRepo.saveAll(List.of(
                Episode.builder().tvShow(got).seasonNumber(1).episodeNumber(1).runtime(62).airYear(2011)
                    .title("Winter Is Coming").overview("Lord Stark is asked to be the King's Hand and discovers a dark secret.").build(),
                Episode.builder().tvShow(got).seasonNumber(1).episodeNumber(9).runtime(57).airYear(2011)
                    .title("Baelor").overview("Ned Stark faces execution; Robb leads his army south.").build(),
                Episode.builder().tvShow(got).seasonNumber(1).episodeNumber(10).runtime(53).airYear(2011)
                    .title("Fire and Blood").overview("The realm reacts to Ned's execution; Daenerys emerges from the flames.").build(),
                Episode.builder().tvShow(got).seasonNumber(2).episodeNumber(1).runtime(53).airYear(2012)
                    .title("The North Remembers").overview("Joffrey celebrates his birthday; Stannis plans his claim to the throne.").build(),
                Episode.builder().tvShow(got).seasonNumber(2).episodeNumber(9).runtime(59).airYear(2012)
                    .title("Blackwater").overview("Stannis's fleet attacks King's Landing in a massive battle.").build(),
                Episode.builder().tvShow(got).seasonNumber(3).episodeNumber(1).runtime(52).airYear(2013)
                    .title("Valar Dohaeris").overview("Jon is brought before Mance Rayder; Daenerys sails for Slaver's Bay.").build(),
                Episode.builder().tvShow(got).seasonNumber(3).episodeNumber(9).runtime(52).airYear(2013)
                    .title("The Rains of Castamere").overview("The Red Wedding devastates the Stark forces.").build(),
                Episode.builder().tvShow(got).seasonNumber(4).episodeNumber(1).runtime(51).airYear(2014)
                    .title("Two Swords").overview("Tyrion welcomes a new Lannister ally; Jon prepares for war.").build(),
                Episode.builder().tvShow(got).seasonNumber(4).episodeNumber(8).runtime(52).airYear(2014)
                    .title("The Mountain and the Viper").overview("Oberyn Martell fights the Mountain as Tyrion's champion.").build(),
                Episode.builder().tvShow(got).seasonNumber(5).episodeNumber(1).runtime(50).airYear(2015)
                    .title("The Wars to Come").overview("Cersei sees the new world order; Jon weighs his options.").build(),
                Episode.builder().tvShow(got).seasonNumber(5).episodeNumber(8).runtime(57).airYear(2015)
                    .title("Hardhome").overview("Jon Snow fights the White Walkers at Hardhome.").build(),
                Episode.builder().tvShow(got).seasonNumber(6).episodeNumber(1).runtime(50).airYear(2016)
                    .title("The Red Woman").overview("The Night's Watch faces consequences; Sansa and Theon flee.").build(),
                Episode.builder().tvShow(got).seasonNumber(6).episodeNumber(9).runtime(60).airYear(2016)
                    .title("Battle of the Bastards").overview("Jon and Sansa reclaim Winterfell from Ramsay Bolton.").build(),
                Episode.builder().tvShow(got).seasonNumber(7).episodeNumber(1).runtime(59).airYear(2017)
                    .title("Dragonstone").overview("Jon organizes the North's defenses; Daenerys arrives at Dragonstone.").build(),
                Episode.builder().tvShow(got).seasonNumber(7).episodeNumber(7).runtime(79).airYear(2017)
                    .title("The Dragon and the Wolf").overview("A meeting in King's Landing; Jon and Daenerys's relationship deepens.").build(),
                Episode.builder().tvShow(got).seasonNumber(8).episodeNumber(1).runtime(54).airYear(2019)
                    .title("Winterfell").overview("The forces of men prepare for the final battle against the dead.").build(),
                Episode.builder().tvShow(got).seasonNumber(8).episodeNumber(3).runtime(82).airYear(2019)
                    .title("The Long Night").overview("The Battle of Winterfell against the Night King.").build(),
                Episode.builder().tvShow(got).seasonNumber(8).episodeNumber(6).runtime(80).airYear(2019)
                    .title("The Iron Throne").overview("The Starks decide the fate of the Seven Kingdoms.").build()));

            // ── Reviews (max one per user per title) ──────────────────────
            reviewRepo.saveAll(List.of(
                Review.builder().user(admin).movie(shawshank).score(10)
                    .comment("Unbelievable that this movie didn't get a single Oscar.").build(),
                Review.builder().user(user).movie(godfather).score(10)
                    .comment("An offer you can't refuse.").build(),
                Review.builder().user(admin).movie(inception).score(9)
                    .comment("Still arguing about the ending.").build(),
                Review.builder().user(user).movie(theMatrix).score(9)
                    .comment("Aged remarkably well.").build(),
                Review.builder().user(admin).tvShow(friends).score(8)
                    .comment("Comfort TV at its finest.").build(),
                Review.builder().user(user).tvShow(seinfeld).score(9)
                    .comment("The blueprint for every sitcom since.").build(),
                Review.builder().user(admin).tvShow(got).score(7)
                    .comment("Amazing until the final season.").build()));

            // ── Watch lists (max one entry per user per title) ───────────
            watchlistRepo.saveAll(List.of(
                WatchlistItem.builder().user(admin).movie(inception).status(WatchStatus.WATCHED).build(),
                WatchlistItem.builder().user(admin).tvShow(got).status(WatchStatus.WANT_TO_WATCH).build(),
                WatchlistItem.builder().user(user).movie(theMatrix).status(WatchStatus.WATCHED).build(),
                WatchlistItem.builder().user(user).movie(shawshank).status(WatchStatus.WANT_TO_WATCH).build(),
                WatchlistItem.builder().user(user).tvShow(seinfeld).status(WatchStatus.WANT_TO_WATCH).build()));

                log.info("Seeding complete. {} movies, {} TV shows and {} reviews created.",
                    movieRepo.count(), tvShowRepo.count(), reviewRepo.count());
            }
        };
    }


    private Movie saveMovie(Movie movie, List<Person> directors, List<CastEntry> cast) {
        movie.getDirectors().addAll(directors);
        movieRepo.save(movie);
        movieCastRepo.saveAll(cast.stream()
            .map(entry -> MovieCast.builder().movie(movie).person(entry.person()).characterName(entry.characterName()).build())
            .toList());
        return movie;
    }

    private TvShow saveTvShow(TvShow show, List<Person> creators, List<CastEntry> cast) {
        show.getCreators().addAll(creators);
        tvShowRepo.save(show);
        tvShowCastRepo.saveAll(cast.stream()
            .map(entry -> TvShowCast.builder().tvShow(show).person(entry.person()).characterName(entry.characterName()).build())
            .toList());
        return show;
    }

}
