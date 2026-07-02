package com.example.WebBanDoGiaDung.security.dto;

import com.example.WebBanDoGiaDung.entity.Account;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSessionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer accountId;
    private String email;
    private String name;
    private Integer role;
    private String status;
    private String avatar;

    public static AccountSessionDto fromAccount(Account account) {
        if (account == null) {
            return null;
        }

        return AccountSessionDto.builder()
                .accountId(account.getAccountId())
                .email(account.getEmail())
                .name(account.getName())
                .role(account.getRole())
                .status(account.getStatus())
                .avatar(account.getAvatar())
                .build();
    }
}
