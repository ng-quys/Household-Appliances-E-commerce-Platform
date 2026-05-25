package com.example.WebBanDoGiaDung.security;

import com.example.WebBanDoGiaDung.entity.Account;
import com.example.WebBanDoGiaDung.service.AccountService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final AccountService accountService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        Account account = extractAccount(authentication);

        if (account == null) {
            getRedirectStrategy().sendRedirect(request, response, "/login?error=account_not_found");
            return;
        }

        try {
            String jwtToken = jwtService.generateToken(account);

            Cookie cookie = new Cookie("jwt", jwtToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(false);
            cookie.setPath("/");
            cookie.setMaxAge(7 * 24 * 60 * 60);
            response.addCookie(cookie);

            System.out.println("🎉 Google Login SUCCESS: " + account.getEmail());

            String redirectUrl = account.getRole() != null && account.getRole() == 0
                    ? "/admin/home"
                    : "/home?login=success";

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            e.printStackTrace();
            getRedirectStrategy().sendRedirect(request, response, "/login?error=jwt_error");
        }
    }

    private Account extractAccount(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof AccountPrincipal custom) {
            return custom.getAccount();
        }

        if (principal instanceof OAuth2User oAuth2User) {
            String email = (String) oAuth2User.getAttribute("email");
            if (email != null) {
                return accountService.findByEmail(email).orElse(null);
            }
        }

        String email = authentication.getName();
        if (email != null && !email.isBlank()) {
            return accountService.findByEmail(email).orElse(null);
        }

        return null;
    }
}