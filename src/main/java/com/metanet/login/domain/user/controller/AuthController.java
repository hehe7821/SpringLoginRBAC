package com.metanet.login.domain.user.controller;

import com.metanet.login.domain.user.dto.LoginRequest;
import com.metanet.login.domain.user.dto.PasswordResetConfirmRequest;
import com.metanet.login.domain.user.dto.PasswordResetRequest;
import com.metanet.login.domain.user.dto.PasswordResetRequestResponse;
import com.metanet.login.domain.user.dto.RefreshTokenRequest;
import com.metanet.login.domain.user.dto.SignupRequest;
import com.metanet.login.domain.user.dto.TokenResponse;
import com.metanet.login.domain.user.dto.UserResponse;
import com.metanet.login.domain.user.dto.UserUpdateRequest;
import com.metanet.login.domain.user.service.AuthService;
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

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/auth/signup")
	@Operation(summary = "회원가입", description = "새 사용자를 생성하고 access token과 refresh token을 발급합니다.")
	public ResponseEntity<TokenResponse> signup(@RequestBody SignupRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
	}

	@PostMapping("/auth/login")
	@Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 JWT 토큰을 발급합니다.")
	public TokenResponse login(@RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/auth/refresh")
	@Operation(summary = "JWT 재발급", description = "refresh token으로 새 access token과 refresh token을 발급합니다.")
	public TokenResponse refresh(@RequestBody RefreshTokenRequest request) {
		return authService.refresh(request);
	}

	@PostMapping("/auth/logout")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "로그아웃", description = "현재 사용자의 refresh token을 폐기합니다.")
	public Map<String, String> logout(Authentication authentication) {
		authService.logout(authentication);
		return Map.of("message", "Logout completed");
	}

	@GetMapping("/users/me")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
	public UserResponse getMe(Authentication authentication) {
		return authService.getMe(authentication);
	}

	@PatchMapping("/users/me")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "내 정보 수정", description = "현재 로그인한 사용자의 프로필 정보를 부분 수정합니다.")
	public UserResponse updateMeByPatch(@RequestBody UserUpdateRequest request, Authentication authentication) {
		return authService.updateMe(request, authentication);
	}

	@DeleteMapping("/users/me")
	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "회원 탈퇴", description = "현재 로그인한 사용자를 탈퇴 처리합니다.")
	public ResponseEntity<Void> withdrawMe(Authentication authentication) {
		authService.withdrawMe(authentication);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/auth/password/reset/request")
	@Operation(summary = "비밀번호 재설정 요청", description = "비밀번호 재설정 토큰을 발급합니다. 현재 구현은 개발용으로 토큰을 응답에 포함합니다.")
	public PasswordResetRequestResponse requestPasswordReset(@RequestBody PasswordResetRequest request) {
		return authService.requestPasswordReset(request);
	}

	@PostMapping("/auth/password/reset/confirm")
	@Operation(summary = "비밀번호 재설정 완료", description = "재설정 토큰과 새 비밀번호로 비밀번호를 변경합니다.")
	public Map<String, String> confirmPasswordReset(@RequestBody PasswordResetConfirmRequest request) {
		authService.confirmPasswordReset(request);
		return Map.of("message", "Password reset completed");
	}
}
