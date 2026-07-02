package com.example.WebBanDoGiaDung.advice;

import com.example.WebBanDoGiaDung.dto.GenreCacheDto;
import com.example.WebBanDoGiaDung.dto.ProductCacheDto;
import com.example.WebBanDoGiaDung.service.CartService;
import com.example.WebBanDoGiaDung.service.GenreService;
import com.example.WebBanDoGiaDung.service.ProductService;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttribute {

    private static final int HEADER_PREVIEW_LIMIT = 8;

    private final CartService cartService;
    private final GenreService genreService;
    private final ProductService productService;

    @ModelAttribute("cartCount")
    public int cartCount(HttpSession session) {
        return cartService.getCartQuantity(session);
    }

    @ModelAttribute("headerGenres")
    public List<GenreCacheDto> headerGenres() {
        try {
            return genreService.findHeaderGenres();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    @ModelAttribute("headerGenreProducts")
    public Map<Integer, List<ProductCacheDto>> headerGenreProducts() {
        List<GenreCacheDto> genres = headerGenres();
        if (genres.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, List<ProductCacheDto>> previewMap = new LinkedHashMap<>();
        for (GenreCacheDto genre : genres) {
            if (genre == null || genre.getGenreId() == null) {
                continue;
            }
            try {
                previewMap.put(
                        genre.getGenreId(),
                        productService.findHeaderPreviewProductsByGenreId(genre.getGenreId(), HEADER_PREVIEW_LIMIT)
                );
            } catch (Exception ignored) {
                previewMap.put(genre.getGenreId(), Collections.emptyList());
            }
        }

        return previewMap;
    }
}
