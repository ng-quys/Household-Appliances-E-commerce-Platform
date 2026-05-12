package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.dto.GenreCacheDto;
import com.example.WebBanDoGiaDung.entity.Genre;
import java.util.List;
import java.util.Optional;

public interface GenreService extends CrudService<Genre, Integer> {
    Optional<Genre> findByGenreName(String genreName);

    List<GenreCacheDto> findAllGenreSummaries();
}
