package com.example.WebBanDoGiaDung.controller.api;

import com.example.WebBanDoGiaDung.service.GenreService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/genreapi")
public class GenreApiController {

    private final GenreService genreService;

    public GenreApiController(GenreService genreService) {
        this.genreService = genreService;
    }

    @GetMapping
    public Object getGenres() {
        return genreService.findAll().stream()
                .map(g -> Map.of("genre_id", g.getGenreId(), "genre_name", g.getGenreName()))
                .toList();
    }
}
