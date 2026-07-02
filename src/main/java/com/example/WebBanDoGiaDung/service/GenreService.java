package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.dto.GenreCacheDto;
import com.example.WebBanDoGiaDung.entity.Genre;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GenreService extends CrudService<Genre, Integer> {
    Optional<Genre> findByGenreName(String genreName);

    boolean existsByGenreNameIgnoreCaseAndIdNot(String genreName, Integer genreId);

    List<GenreCacheDto> findAllGenreSummaries();

    List<GenreCacheDto> findHeaderGenres();

    Page<Genre> findAdminGenres(String search, Pageable pageable);
}
