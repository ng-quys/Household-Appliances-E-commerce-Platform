package com.example.WebBanDoGiaDung.dto;

import com.example.WebBanDoGiaDung.entity.Product;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCacheDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer productId;
    private Integer genreId;
    private Integer brandId;
    private String genreName;
    private String productName;
    private Double price;
    private Long view;
    private Long buyturn;
    private String quantity;
    private String status;
    private String image;
    private String description;

    public Integer getId() {
        return productId;
    }

    public String getName() {
        return productName;
    }

    public String getImageUrl() {
        return image;
    }

    public String getGenreName() {
        return genreName;
    }

    public String getDiscount() {
        return null;
    }

    public static ProductCacheDto fromEntity(Product product) {
        return ProductCacheDto.builder()
                .productId(product.getProductId())
                .genreId(product.getGenreId())
                .brandId(product.getBrandId())
                .genreName(product.getGenre() != null ? product.getGenre().getGenreName() : null)
                .productName(product.getProductName())
                .price(product.getPrice())
                .view(product.getView())
                .buyturn(product.getBuyturn())
                .quantity(product.getQuantity())
                .status(product.getStatus())
                .image(product.getImage())
                .description(product.getDescription())
                .build();
    }
}
