package com.example.WebBanDoGiaDung.config;

import com.example.WebBanDoGiaDung.security.*;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationSuccessHandler successHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtOAuth2SuccessHandler jwtOAuth2SuccessHandler;
    private final PasswordEncoder passwordEncoder;

    public WebSecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          @Qualifier("customUserDetailsService") UserDetailsService userDetailsService,
                          JwtAuthenticationSuccessHandler successHandler,
                          CustomOAuth2UserService customOAuth2UserService,
                          JwtOAuth2SuccessHandler jwtOAuth2SuccessHandler,
                             PasswordEncoder passwordEncoder) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.successHandler = successHandler;
        this.customOAuth2UserService = customOAuth2UserService;
        this.jwtOAuth2SuccessHandler = jwtOAuth2SuccessHandler;
        this.passwordEncoder = passwordEncoder;

    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/api/auth/**",
                                "/css/**", "/js/**", "/images/**", "/static/**",
                                "/", "/home", "/home/**", "/products", "/products/**",
                                "/oauth2/authorization/**", "/login/oauth2/code/**").permitAll()

                        .requestMatchers("/user/**", "/profile/**").hasAnyRole("USER", "STAFF", "ADMIN")
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(jwtOAuth2SuccessHandler)
                        .failureUrl("/login?error=oauth")
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .deleteCookies("jwt", "JSESSIONID")
                        .addLogoutHandler((request, response, authentication) -> {
                            // XÓA COOKIE JWT TRIỆT ĐỂ
                            Cookie[] cookies = request.getCookies();
                            if (cookies != null) {
                                for (Cookie cookie : cookies) {
                                    if ("jwt".equals(cookie.getName())) {
                                        Cookie clearCookie = new Cookie("jwt", null);
                                        clearCookie.setPath("/");
                                        clearCookie.setMaxAge(0);
                                        clearCookie.setHttpOnly(true);
                                        clearCookie.setSecure(false);        // đổi thành true khi dùng HTTPS
                                        response.addCookie(clearCookie);
                                    }
                                }
                            }

                            // Xóa thêm một lần nữa với các path khác nhau
                            Cookie rootCookie = new Cookie("jwt", null);
                            rootCookie.setPath("/");
                            rootCookie.setMaxAge(0);
                            rootCookie.setHttpOnly(true);
                            response.addCookie(rootCookie);
                        })
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                )
                // ✅ SỬA Ở ĐÂY
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        authProvider.setHideUserNotFoundExceptions(false);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }


}