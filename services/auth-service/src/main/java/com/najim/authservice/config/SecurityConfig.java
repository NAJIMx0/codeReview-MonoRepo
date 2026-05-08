package com.najim.authservice.config;

import com.najim.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final OAuth2AuthorizedClientService authorizedClientService;
    public final ClientRegistrationRepository clientRegistrationRepository;
    public final AuthService authService;
    @Bean
    public SecurityFilterChain FilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequest->authorizeRequest
                        .requestMatchers("/api/auth/logout").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        .anyRequest().authenticated()
                ).exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.getWriter().write("Unauthorized");
                        })
                )
                .oauth2Login(oauth->oauth
                        .defaultSuccessUrl("/api/auth/success",true)
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(authorizationRequestResolver())
                        )
                ).logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .addLogoutHandler((request, response, authentication) -> {
                            if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                                authService.revokeGithubToken(oauthToken);  // ← revoke first
                                authorizedClientService.removeAuthorizedClient(
                                        oauthToken.getAuthorizedClientRegistrationId(),
                                        oauthToken.getName()
                                );
                            }
                        })
                        .logoutSuccessHandler((req, res, auth) -> {
                            // Delete the cookie server-side — this actually works
                            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("JSESSIONID", null);
                            cookie.setMaxAge(0);
                            cookie.setPath("/");
                            cookie.setHttpOnly(true);
                            res.addCookie(cookie);

                            res.sendRedirect("http://localhost:5173");
                        })
                );
        return http.build();
    }

    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver() {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        "/oauth2/authorization"
                );

        // Force GitHub to always show the login screen
        resolver.setAuthorizationRequestCustomizer(customizer ->
                customizer.additionalParameters(params ->
                        {
                            params.put("allow_signup", "false");
                            params.remove("prompt");
                        }
                )

        );

        return resolver;
    }

}
