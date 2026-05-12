package com.example.WebBanDoGiaDung.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "Accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Integer accountId;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "Email", nullable = false, length = 100)
    private String email;

    @Column(name = "Requestcode", length = 255)
    private String requestCode;

    @Column(name = "Role", nullable = false)
    private Integer role;

    @Column(name = "Name", nullable = false, length = 50)
    private String name;

    @Column(name = "Phone", nullable = false, length = 10)
    private String phone;

    @Lob
    @Column(name = "Avatar", columnDefinition = "TEXT")
    private String avatar;

    @Column(name = "create_by", length = 100)
    private String createBy;

    @Column(name = "create_at")
    private LocalDateTime createAt;

    @Column(name = "update_by", length = 100)
    private String updateBy;

    @Column(name = "update_at")
    private LocalDateTime updateAt;

    @Column(name = "status", length = 1)
    private String status;

    @OneToMany(mappedBy = "account")
    private List<Feedback> feedbacks = new ArrayList<>();

    @OneToMany(mappedBy = "account")
    private List<ReplyFeedback> replyFeedbacks = new ArrayList<>();

    @OneToMany(mappedBy = "account")
    private List<OrderEntity> orders = new ArrayList<>();

    @OneToMany(mappedBy = "account")
    private List<AccountAddress> accountAddresses = new ArrayList<>();
}
