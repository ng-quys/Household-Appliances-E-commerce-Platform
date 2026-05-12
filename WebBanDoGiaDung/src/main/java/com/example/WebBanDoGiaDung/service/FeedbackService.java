package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.Feedback;
import java.util.List;

public interface FeedbackService extends CrudService<Feedback, Integer> {
    List<Feedback> findByProductId(Integer productId);

    List<Feedback> findByAccountId(Integer accountId);
}
