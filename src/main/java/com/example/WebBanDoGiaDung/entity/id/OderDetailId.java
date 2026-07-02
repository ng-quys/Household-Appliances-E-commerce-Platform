package com.example.WebBanDoGiaDung.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class OderDetailId implements Serializable {
    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "genre_id")
    private Integer genreId;

    @Column(name = "order_id")
    private Integer orderId;
}
