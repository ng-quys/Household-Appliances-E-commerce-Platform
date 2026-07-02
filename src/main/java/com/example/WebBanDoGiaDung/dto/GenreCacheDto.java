package com.example.WebBanDoGiaDung.dto;

import com.example.WebBanDoGiaDung.entity.Genre;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenreCacheDto implements Serializable {
    private Integer genreId;
    private String genreName;

    public static GenreCacheDto fromEntity(Genre genre) {
        return GenreCacheDto.builder()
                .genreId(genre.getGenreId())
                .genreName(genre.getGenreName())
                .build();
    }
}
