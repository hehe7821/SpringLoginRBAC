package com.metanet.login.domain.user.controller;

import com.metanet.login.domain.user.dto.LoginRequest;
import com.metanet.login.domain.user.dto.PasswordResetRequest;
import com.metanet.login.domain.user.dto.RefreshTokenRequest;
import com.metanet.login.domain.user.dto.SignupRequest;
import com.metanet.login.domain.user.dto.TokenResponse;
import com.metanet.login.domain.user.dto.UserResponse;
import com.metanet.login.domain.user.dto.UserUpdateRequest;
import com.metanet.login.domain.user.dto.email.EmailVerificationConfirmRequest;
import com.metanet.login.domain.user.dto.email.EmailVerificationRequest;
import com.metanet.login.domain.user.dto.email.EmailVerificationResponse;
import com.metanet.login.domain.user.service.AuthService;
import com.metanet.login.domain.user.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Auth/User", description = "JWT authentication and user account APIs")
public class AuthController {
	private final AuthService authService;
	private final EmailVerificationService emailVerificationService;

	public AuthController(AuthService authService, EmailVerificationService emailVerificationService) {
		this.authService = authService;
		this.emailVerificationService = emailVerificationService;
	}

	@PostMapping("/auth/email/verification/request")
	@Operation(summary = "Request email verification", description = "Sends a 6-character verification code by Gmail SMTP.")
	public EmailVerificationResponse requestEmailVerification(@RequestBody EmailVerificationRequest request) {
		return emailVerificationService.request(request);
	}

	@PostMapping("/auth/email/verification/confirm")
	@Operation(summary = "Confirm email verification", description = "Verifies the email verification code stored in Redis.")
	public EmailVerificationResponse confirmEmailVerification(@RequestBody EmailVerificationConfirmRequest request) {
		return emailVerificationService.confirm(request);
	}

	@PostMapping("/auth/signup")
	@Operation(summary = "Signup", description = "Creates a user after SIGNUP email verification and issues JWT tokens.")
	public ResponseEntity<TokenResponse> signup(@RequestBody SignupRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
	}

	@PostMapping("/auth/login")
	@Operation(summary = "Login", description = "Authenticates with email and password and issues JWT tokens.")
	public TokenResponse login(@RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/auth/refresh")
	@Operation(summary = "Refresh JWT", description = "Issues new JWT tokens using a valid refresh token.")
	public TokenResponse refresh(@RequestBody RefreshTokenRequest request) {
		return authService.refresh(request);
	}

	@PostMapping("/auth/logout")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Logout", description = "Revokes the current user's refresh token.")
	public Map<String, String> logout(Authentication authentication) {
		authService.logout(authentication);
		return Map.of("message", "Logout completed");
	}

	@GetMapping("/users/me")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Get my profile", description = "Returns the current user's profile.")
	public UserResponse getMe(Authentication authentication) {
		return authService.getMe(authentication);
	}

	@PatchMapping("/users/me")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Update my profile", description = "Partially updates the current user's profile.")
	public UserResponse updateMeByPatch(@RequestBody UserUpdateRequest request, Authentication authentication) {
		return authService.updateMe(request, authentication);
	}

	@DeleteMapping("/users/me")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Withdraw", description = "Soft-deletes the current user account.")
	public ResponseEntity<Void> withdrawMe(Authentication authentication) {
		authService.withdrawMe(authentication);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/auth/password/reset")
	@Operation(summary = "Reset password", description = "Changes the password after PASSWORD_RESET email verification.")
	public Map<String, String> resetPassword(@RequestBody PasswordResetRequest request) {
		authService.resetPassword(request);
		return Map.of("message", "Password reset completed");
	}
}
