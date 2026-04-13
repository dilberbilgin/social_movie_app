package com.socialmovieclub.core.constant;

public class CacheConstants {
    public static final String FEED_CACHE = "feed";
    public static final String MOVIE_DETAILS = "movieDetails";
    public static final String TRENDING_MOVIES = "trendingMovies";

    // Redis Sorted Set Key for Weekly Ranking
    public static final String WEEKLY_COMMENT_RANKING = "rank:comments:weekly";

    public static final String WATCH_PROVIDERS = "watchProviders";

    //TODO : İleride eklenecekler için: public static final String MOVIE_CACHE = "movies";
}
