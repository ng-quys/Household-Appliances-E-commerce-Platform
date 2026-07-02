package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.dto.GenreCacheDto;
import com.example.WebBanDoGiaDung.entity.Genre;
import com.example.WebBanDoGiaDung.repository.GenreRepository;
import com.example.WebBanDoGiaDung.service.GenreService;
import com.example.WebBanDoGiaDung.specification.admin.AdminGenreSpecification;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenreServiceImpl extends AbstractCrudService<Genre, Integer> implements GenreService {

    private final GenreRepository repository;

    @Override
    protected JpaRepository<Genre, Integer> getRepository() {
        return repository;
    }

    @Override
    public Optional<Genre> findByGenreName(String genreName) {
        return repository.findByGenreName(genreName);
    }

    @Override
    public boolean existsByGenreNameIgnoreCaseAndIdNot(String genreName, Integer genreId) {
        if (genreName == null || genreName.trim().isBlank()) {
            return false;
        }
        String normalized = genreName.trim();
        return repository.findAll().stream()
                .anyMatch(genre -> genre.getGenreName() != null
                        && genre.getGenreName().trim().equalsIgnoreCase(normalized)
                        && (genreId == null || !genre.getGenreId().equals(genreId)));
    }

    @Override
    @Cacheable(cacheNames = "genreList", key = "'all'")
    public List<GenreCacheDto> findAllGenreSummaries() {
        return repository.findAll().stream()
                .map(GenreCacheDto::fromEntity)
                .toList();
    }

    @Override
    public List<GenreCacheDto> findHeaderGenres() {
        return findAllGenreSummaries();
    }

    @Override
    public Page<Genre> findAdminGenres(String search, Pageable pageable) {
        return repository.findAll(AdminGenreSpecification.filter(search), pageable);
    }

    @Override
    @CacheEvict(cacheNames = "genreList", allEntries = true)
    public Genre save(Genre entity) {
        return repository.save(entity);
    }

    @Override
    @CacheEvict(cacheNames = "genreList", allEntries = true)
    public Genre update(Integer id, Genre entity) {
        return super.update(id, entity);
    }

    @Override
    @CacheEvict(cacheNames = "genreList", allEntries = true)
    public void deleteById(Integer id) {
        repository.deleteById(id);
    }
}
