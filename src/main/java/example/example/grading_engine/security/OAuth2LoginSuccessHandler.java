package example.example.grading_engine.security;
import example.example.grading_engine.model.entity.User;
import example.example.grading_engine.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
public class OAuth2LoginSuccessHandler
        implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    @Value("${app.frontend-url}")
    private String frontendUrl;

    public OAuth2LoginSuccessHandler(
            UserRepository userRepository,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        assert oauthUser != null;
        String email = oauthUser.getAttribute("email");

        User user = userRepository
                .findByEmail(email)
                .orElseThrow();

        String accessToken = jwtUtil.generateAccessToken(
                user.getId().toString(),
                user.getRole()
        );

        String refreshToken = jwtUtil.generateRefreshToken(
                user.getId().toString()
        );

        boolean isLocal = request.getServerName().equals("localhost");

        ResponseCookie accessCookie =
                ResponseCookie.from("ACCESS_TOKEN", accessToken)
                        .httpOnly(true)
                        .secure(!isLocal)
                        .sameSite(isLocal ? "Lax" : "None")
                        .path("/")
                        .maxAge(Duration.ofMinutes(15))
                        .build();

        ResponseCookie refreshCookie =
                ResponseCookie.from("REFRESH_TOKEN", refreshToken)
                        .httpOnly(true)
                        .secure(!isLocal)
                        .sameSite(isLocal ? "Lax" : "None")
                        .path("/api/auth/refresh")
                        .maxAge(Duration.ofDays(7))
                        .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        response.sendRedirect(frontendUrl + "/api/auth/me");  //change later to where frontend wants to go app.frontend.url
    }
}