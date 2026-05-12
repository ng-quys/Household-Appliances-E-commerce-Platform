package com.example.WebBanDoGiaDung.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "ReplyFeedback")
public class ReplyFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rep_feedback_id")
    private Integer repFeedbackId;

    @Column(name = "feedback_id", nullable = false, insertable = false, updatable = false)
    private Integer feedbackId;

    @Column(name = "account_id", nullable = false, insertable = false, updatable = false)
    private Integer accountId;

    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "stastus", length = 1)
    private String status;

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", referencedColumnName = "feedback_id")
    private Feedback feedback;
}
