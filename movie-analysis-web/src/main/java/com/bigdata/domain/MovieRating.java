package com.bigdata.domain;

public class MovieRating {

    String movieId;

    String movieName;

    String movieGenres;

    String maxRating;

    String avgRating;


    String minRating;

    String timestamp;

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public String getMovieGenres() {
        return movieGenres;
    }

    public void setMovieGenres(String movieGenres) {
        this.movieGenres = movieGenres;
    }

    public String getMaxRating() {
        return maxRating;
    }

    public void setMaxRating(String maxRating) {
        this.maxRating = maxRating;
    }

    public String getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(String avgRating) {
        this.avgRating = avgRating;
    }

    public String getMinRating() {
        return minRating;
    }

    public void setMinRating(String minRating) {
        this.minRating = minRating;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "MovieRating{" +
                "movieId='" + movieId + '\'' +
                ", movieName='" + movieName + '\'' +
                ", movieGenres='" + movieGenres + '\'' +
                ", maxRating='" + maxRating + '\'' +
                ", avgRating='" + avgRating + '\'' +
                ", minRating='" + minRating + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
