package example.example.grading_engine.security;

import example.example.grading_engine.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final JwtAuthenticationFilter jwtFilter;
    private final CustomOAuth2UserService oauthUserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter,
            CustomOAuth2UserService oauthUserService,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler
    ) {
        this.jwtFilter = jwtFilter;
        this.oauthUserService = oauthUserService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler() {

        AccessDeniedHandlerImpl handler =
                new AccessDeniedHandlerImpl();

        handler.setErrorPage("/accessDenied");

        return handler;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config =
                new CorsConfiguration();

        config.setAllowedOrigins(
                List.of(frontendUrl)
        );

        config.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        config.setAllowedHeaders(
                List.of("*")
        );

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                config
        );

        return source;
    }

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            UserRepository userRepository
    ) throws Exception {

        http

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        /*
                         * Public authentication endpoints
                         */
                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()

                        /*
                         * Swagger
                         */
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        /*
                         * Role-based APIs
                         */
                        .requestMatchers(
                                "/api/faculty/**"
                        ).hasRole("FACULTY")

                        .requestMatchers(
                                "/api/hod/**"
                        ).hasRole("HOD")

                        .requestMatchers(
                                "/api/student/**"
                        ).hasRole("STUDENT")

                        /*
                         * Everything else requires authentication
                         */
                        .anyRequest().authenticated()
                )

                /*
                 * IMPORTANT:
                 *
                 * If an API request is unauthenticated,
                 * return 401 instead of redirecting to Google.
                 */
                .exceptionHandling(ex -> ex

                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(
                                        HttpStatus.UNAUTHORIZED
                                ),
                                request ->
                                        request.getRequestURI()
                                                .startsWith("/api/")
                        )

                        .accessDeniedHandler(
                                accessDeniedHandler()
                        )
                )

                /*
                 * Google OAuth login remains unchanged.
                 */
                .oauth2Login(oauth -> oauth

                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(
                                        oauthUserService
                                )
                        )

                        .successHandler(
                                oAuth2LoginSuccessHandler
                        )
                )

                /*
                 * JWT authentication
                 */
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}