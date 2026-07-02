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
@Table(name = "Brand")
public class Brand extends Auditable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brand_id")
    private Integer brandId;

    @Column(name = "image", length = 200)
    private String image;

    @Column(name = "brand_name", nullable = false, length = 50)
    private String brandName;


    @OneToMany(mappedBy = "brand")
    private List<Product> products = new ArrayList<>();
}
