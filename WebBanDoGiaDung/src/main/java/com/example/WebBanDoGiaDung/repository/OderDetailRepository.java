package com.example.WebBanDoGiaDung.repository;

import com.example.WebBanDoGiaDung.entity.OderDetail;
import com.example.WebBanDoGiaDung.entity.id.OderDetailId;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OderDetailRepository extends JpaRepository<OderDetail, OderDetailId> {

    @EntityGraph(attributePaths = {"product"})
    List<OderDetail> findByOrderOrderId(Integer orderId);

    List<OderDetail> findByProductProductId(Integer productId);

    @Query("""
        SELECT 
            YEAR(o.orderDate),
            MONTH(o.orderDate),
            SUM(od.quantity * od.price)
        FROM OderDetail od
        JOIN od.order o
        WHERE o.status = 'COMPLETED'
           OR o.status = '4'
        GROUP BY YEAR(o.orderDate), MONTH(o.orderDate)
        ORDER BY YEAR(o.orderDate), MONTH(o.orderDate)
    """)
    List<Object[]> getMonthlyRevenueStatistics();

    @Query("""
        SELECT 
            od.product.productId,
            SUM(od.quantity)
        FROM OderDetail od
        JOIN od.order o
        WHERE o.orderDate >= :fromDate
          AND (
                o.status = 'COMPLETED'
                OR o.status = '4'
          )
        GROUP BY od.product.productId
    """)
    List<Object[]> getSoldQuantityByProductFromDate(@Param("fromDate") LocalDateTime fromDate);
}