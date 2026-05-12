package com.example.WebBanDoGiaDung.repository;

import com.example.WebBanDoGiaDung.entity.OderDetail;
import com.example.WebBanDoGiaDung.entity.id.OderDetailId;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OderDetailRepository extends JpaRepository<OderDetail, OderDetailId> {
    @EntityGraph(attributePaths = {"product"})
    List<OderDetail> findByOrderOrderId(Integer orderId);

    List<OderDetail> findByProductProductId(Integer productId);
}
