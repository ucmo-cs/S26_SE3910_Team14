package com.bankscheduling.appointment.controller;

import com.bankscheduling.appointment.dto.auth.AuthStatusResponse;
import com.bankscheduling.appointment.dto.auth.LoginRequest;
import com.bankscheduling.appointment.security.cookie.TokenCookieService;
import com.bankscheduling.appointment.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenCookieService tokenCookieService;

    public AuthController(AuthService authService, TokenCookieService tokenCookieService) {
        this.authService = authService;
        this.tokenCookieService = tokenCookieService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthStatusResponse> login(
            @Valid @RequestBody LoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthService.LoginTokenPair tokens = authService.login(body.getUsername(), body.getPassword(), request);
        tokenCookieService.attachTokenCookies(response, tokens.accessToken(), tokens.refreshToken());
        return ResponseEntity.status(HttpStatus.OK)
                .body(new AuthStatusResponse("success", "Logged in successfully"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthStatusResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String newAccess = authService.refreshAccessToken(request);
        tokenCookieService.attachAccessTokenCookie(response, newAccess);
        return ResponseEntity.ok(new AuthStatusResponse("success", "Access token refreshed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthStatusResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(new AuthStatusResponse("success", "Logged out successfully"));
    }
}
