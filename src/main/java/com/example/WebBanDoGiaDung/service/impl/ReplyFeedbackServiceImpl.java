package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.ReplyFeedback;
import com.example.WebBanDoGiaDung.repository.ReplyFeedbackRepository;
import com.example.WebBanDoGiaDung.service.ReplyFeedbackService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReplyFeedbackServiceImpl extends AbstractCrudService<ReplyFeedback, Integer> implements ReplyFeedbackService {

    private final ReplyFeedbackRepository repository;

    @Override
    protected JpaRepository<ReplyFeedback, Integer> getRepository() {
        return repository;
    }

    @Override
    public List<ReplyFeedback> findByFeedbackId(Integer feedbackId) {
        return repository.findByFeedbackFeedbackId(feedbackId);
    }
}
