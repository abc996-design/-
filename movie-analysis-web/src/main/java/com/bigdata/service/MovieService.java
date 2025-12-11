package com.bigdata.service;

import com.bigdata.domain.MovieRating;
import com.bigdata.domain.MovieRatingCount;

import java.util.List;

public interface MovieService {

    public List<MovieRatingCount> getAll();

    public List<MovieRating> getMovieRatingById(Integer movieId);

    public List<MovieRating> getMovieRatingByName(String movieName);

}
