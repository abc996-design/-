package com.bigdata.service.impl;

import com.bigdata.dao.MovieDao;
import com.bigdata.domain.MovieRating;
import com.bigdata.domain.MovieRatingCount;
import com.bigdata.service.MovieService;
import com.bigdata.utils.HbaseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class MovieServiceImpl implements MovieService {

    @Autowired
    private MovieDao movieDao;

    @Override
    public List<MovieRatingCount> getAll() {
        try {
            return HbaseUtil.getTopMoviesByTimeAndCount("hot_movie_rate", 10);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//        return movieDao.getAll();
    }

    @Override
    public List<MovieRating> getMovieRatingById(Integer movieId) {

        try {
            return HbaseUtil.getData("movie_rate_test", String.valueOf(movieId), "rateinfo", "userId");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public List<MovieRating> getMovieRatingByName(String movieName) {

        try {
            return HbaseUtil.getScanSingleColumnValueFilter("movie_rate_test", "movieinfo", "title", movieName);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
