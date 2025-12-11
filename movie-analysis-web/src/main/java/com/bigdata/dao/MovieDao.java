package com.bigdata.dao;

import com.bigdata.domain.MovieRatingCount;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface MovieDao {

    @Select("select * from hot_movie_rates")
    public List<MovieRatingCount> getAll();

//    public List<MovieRating> getMovieRatingById(Integer movieId);
//
//    public List<MovieRating> getMovieRatingByName(String movieName);


}
