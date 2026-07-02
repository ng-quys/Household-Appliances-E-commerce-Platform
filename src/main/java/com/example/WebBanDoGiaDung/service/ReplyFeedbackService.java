package com.example.WebBanDoGiaDung.service;

import com.example.WebBanDoGiaDung.entity.ReplyFeedback;
import java.util.List;

public interface ReplyFeedbackService extends CrudService<ReplyFeedback, Integer> {
    List<ReplyFeedback> findByFeedbackId(Integer feedbackId);
}
