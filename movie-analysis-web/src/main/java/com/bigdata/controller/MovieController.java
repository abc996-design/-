package com.bigdata.controller;

import com.bigdata.domain.MovieRating;
import com.bigdata.domain.MovieRatingCount;
import com.bigdata.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movies")
public class MovieController {

    @Autowired
    private MovieService movieService;

    /**
     * 实时统计的热门评分电影
     * @return
     */
    @GetMapping("/hot")
    public Result getAll() {
        List<MovieRatingCount> rateCountList = movieService.getAll();
        Integer code = rateCountList != null ? Code.GET_OK : Code.GET_ERR;
        String msg = rateCountList != null ? "" : "数据查询失败，请重试！";
        return new Result(code,rateCountList,msg);
    }

    /**
     * 通过电影ID，查看评分
     * @param id
     * @return
     */
    @GetMapping("/id/{id}")
    public Result getMovieRatingById(@PathVariable Integer id) {
        List<MovieRating> rating = movieService.getMovieRatingById(id);
        Integer code = rating != null ? Code.GET_OK : Code.GET_ERR;
        String msg = rating != null ? "" : "数据查询失败，请重试！";
        return new Result(code,rating,msg);
    }

    /**
     * 通过名称查询电影评分
     * @param name
     * @return
     */
    @GetMapping("/name/{name}")
    public Result getMovieRatingByName(@PathVariable String name) {
        List<MovieRating> rating = movieService.getMovieRatingByName(name);
        Integer code = rating != null ? Code.GET_OK : Code.GET_ERR;
        String msg = rating != null ? "" : "数据查询失败，请重试！";
        return new Result(code,rating,msg);
    }

}
