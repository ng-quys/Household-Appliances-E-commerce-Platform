package com.example.WebBanDoGiaDung.repository;

import com.example.WebBanDoGiaDung.entity.ReplyFeedback;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReplyFeedbackRepository extends JpaRepository<ReplyFeedback, Integer> {
    List<ReplyFeedback> findByFeedbackFeedbackId(Integer feedbackId);
}
