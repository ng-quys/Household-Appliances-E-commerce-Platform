package com.example.WebBanDoGiaDung.repository;

import com.example.WebBanDoGiaDung.entity.Feedback;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    List<Feedback> findByProductProductId(Integer productId);

    List<Feedback> findByAccountAccountId(Integer accountId);
}
