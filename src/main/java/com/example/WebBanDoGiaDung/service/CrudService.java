package com.example.WebBanDoGiaDung.service;

import java.util.List;
import java.util.Optional;

public interface CrudService<T, ID> {
    List<T> findAll();

    Optional<T> findById(ID id);

    T save(T entity);

    T update(ID id, T entity);

    void deleteById(ID id);
}
