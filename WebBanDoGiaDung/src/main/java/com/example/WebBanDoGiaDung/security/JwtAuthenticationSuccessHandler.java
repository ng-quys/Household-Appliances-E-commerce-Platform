package com.example.WebBanDoGiaDung.security;

import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.service.AccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final AccountService accountService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        AccountPrincipal userDetails = (AccountPrincipal) authentication.getPrincipal();
        Integer accountId = userDetails.getAccountId();
        Account account = accountId != null ? accountService.findById(accountId).orElse(null) : null;

        if (account == null) {
            getRedirectStrategy().sendRedirect(request, response, "/login?error=account_not_found");
            return;
        }

        String accessToken = jwtService.generateToken(account);

        Cookie jwtCookie = new Cookie("jwt", accessToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(jwtCookie);

        String redirectUrl = account.getRole() != null && account.getRole() == 0
                ? "/admin/home"
                : "/home?login=success";

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
