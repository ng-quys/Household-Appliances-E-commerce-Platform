package com.example.WebBanDoGiaDung.service.impl;

import com.example.WebBanDoGiaDung.entity.Feedback;
import com.example.WebBanDoGiaDung.repository.FeedbackRepository;
import com.example.WebBanDoGiaDung.service.FeedbackService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl extends AbstractCrudService<Feedback, Integer> implements FeedbackService {

    private final FeedbackRepository repository;

    @Override
    protected JpaRepository<Feedback, Integer> getRepository() {
        return repository;
    }

    @Override
    public List<Feedback> findByProductId(Integer productId) {
        return repository.findByProductProductId(productId);
    }

    @Override
    public List<Feedback> findByAccountId(Integer accountId) {
        return repository.findByAccountAccountId(accountId);
    }
}
