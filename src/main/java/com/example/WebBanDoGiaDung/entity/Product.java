package com.example.WebBanDoGiaDung.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "Product")
public class Product extends Auditable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "genre_id", nullable = false, insertable = false, updatable = false)
    private Integer genreId;

    @Column(name = "brand_id", nullable = false, insertable = false, updatable = false)
    private Integer brandId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "view", nullable = false)
    private Long view;

    @Column(name = "buyturn", nullable = false)
    private Long buyturn;

    @Column(name = "quantity", nullable = false, length = 10)
    private String quantity;

    @Column(name = "status", length = 1)
    private String status;

    @Lob
    @Column(name = "specifications", columnDefinition = "LONGTEXT")
    private String specifications;

    @Lob
    @Column(name = "image", columnDefinition = "LONGTEXT")
    private String image;

    @Lob
    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", referencedColumnName = "brand_id")
    private Brand brand;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id", referencedColumnName = "genre_id")
    private Genre genre;

    @OneToMany(mappedBy = "product")
    private List<OderDetail> oderDetails = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    private List<ProductImage> productImages = new ArrayList<>();
}
