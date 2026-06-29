package com.metanet.login.domain.user.service;

import com.metanet.login.domain.user.dto.LoginRequest;
import com.metanet.login.domain.user.dto.PasswordResetRequest;
import com.metanet.login.domain.user.dto.RefreshTokenRequest;
import com.metanet.login.domain.user.dto.SignupRequest;
import com.metanet.login.domain.user.dto.TokenResponse;
import com.metanet.login.domain.user.dto.UserResponse;
import com.metanet.login.domain.user.dto.UserUpdateRequest;
import com.metanet.login.domain.user.dto.email.EmailVerificationPurpose;
import com.metanet.login.domain.user.entity.User;
import com.metanet.login.domain.user.repository.UserRepository;
import com.metanet.login.global.security.CustomUserDetails;
import com.metanet.login.global.security.jwt.JwtTokenProvider;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
	private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh:";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final StringRedisTemplate redisTemplate;
	private final EmailVerificationService emailVerificationService;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenProvider jwtTokenProvider,
			StringRedisTemplate redisTemplate,
			EmailVerificationService emailVerificationService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
		this.redisTemplate = redisTemplate;
		this.emailVerificationService = emailVerificationService;
	}

	@Transactional
	public TokenResponse signup(SignupRequest request) {
		validateEmail(request.getEmail());
		validatePassword(request.getPassword());
		emailVerificationService.requireVerified(request.getEmail(), EmailVerificationPurpose.SIGNUP);
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
		}

		User user = userRepository.insertUser(
				request.getEmail().trim(),
				passwordEncoder.encode(request.getPassword()),
				blankToNull(request.getDisplayName()));
		userRepository.assignDefaultUserRole(user.getUserId());
		emailVerificationService.consumeVerified(request.getEmail(), EmailVerificationPurpose.SIGNUP);
		return issueTokens(user);
	}

	@Transactional
	public TokenResponse login(LoginRequest request) {
		validateEmail(request.getEmail());
		if (isBlank(request.getPassword())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
		}

		User user = userRepository.findByEmail(request.getEmail());
		if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
		}
		if (!"ACTIVE".equals(user.getStatus()) && !"PENDING".equals(user.getStatus())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not active");
		}

		userRepository.updateLastLoginAt(user.getUserId());
		return issueTokens(user);
	}

	public TokenResponse refresh(RefreshTokenRequest request) {
		if (isBlank(request.getRefreshToken())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required");
		}

		JwtTokenProvider.JwtClaims claims = parseRefreshToken(request.getRefreshToken());
		String redisKey = refreshTokenKey(claims.userId());
		String savedRefreshToken = redisTemplate.opsForValue().get(redisKey);
		if (!request.getRefreshToken().equals(savedRefreshToken)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
		}

		User user = requireUser(claims.userId());
		return issueTokens(user);
	}

	public void logout(Authentication authentication) {
		Long userId = currentUserId(authentication);
		redisTemplate.delete(refreshTokenKey(userId));
	}

	@Transactional
	public void resetPassword(PasswordResetRequest request) {
		validateEmail(request.getEmail());
		emailVerificationService.requireVerified(request.getEmail(), EmailVerificationPurpose.PASSWORD_RESET);
		validatePassword(request.getNewPassword());
		User user = userRepository.findByEmail(request.getEmail());
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}

		userRepository.updatePassword(user.getUserId(), passwordEncoder.encode(request.getNewPassword()));
		emailVerificationService.consumeVerified(request.getEmail(), EmailVerificationPurpose.PASSWORD_RESET);
		redisTemplate.delete(refreshTokenKey(user.getUserId()));
	}

	@Transactional(readOnly = true)
	public UserResponse getMe(Authentication authentication) {
		Long userId = currentUserId(authentication);
		return new UserResponse(requireUser(userId));
	}

	@Transactional
	public UserResponse updateMe(UserUpdateRequest request, Authentication authentication) {
		Long userId = currentUserId(authentication);
		if (request == null || isBlank(request.getDisplayName())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name is required");
		}
		if (request.getDisplayName().trim().length() > 50) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name must be 50 characters or less");
		}

		int updated = userRepository.updateDisplayName(userId, request.getDisplayName().trim());
		if (updated == 0) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}
		return new UserResponse(requireUser(userId));
	}

	@Transactional
	public void withdrawMe(Authentication authentication) {
		Long userId = currentUserId(authentication);
		int updated = userRepository.softDelete(userId);
		if (updated == 0) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}
		redisTemplate.delete(refreshTokenKey(userId));
	}

	private TokenResponse issueTokens(User user) {
		String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getEmail());
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId(), user.getEmail());
		redisTemplate.opsForValue().set(
				refreshTokenKey(user.getUserId()),
				refreshToken,
				Duration.ofSeconds(jwtTokenProvider.getRefreshTokenValiditySeconds()));
		return new TokenResponse(
				accessToken,
				refreshToken,
				jwtTokenProvider.getAccessTokenValiditySeconds(),
				user.getUserId());
	}

	private JwtTokenProvider.JwtClaims parseRefreshToken(String refreshToken) {
		try {
			return jwtTokenProvider.parseAndValidate(refreshToken, "refresh");
		} catch (IllegalArgumentException e) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
		}
	}

	private User requireUser(Long userId) {
		User user = userRepository.findById(userId);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}
		return user;
	}

	private String refreshTokenKey(Long userId) {
		return REFRESH_TOKEN_KEY_PREFIX + userId;
	}

	private Long currentUserId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
		}
		return userDetails.getUserId();
	}

	private void validateEmail(String email) {
		if (isBlank(email) || !email.contains("@")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid email is required");
		}
	}

	private void validatePassword(String password) {
		if (isBlank(password) || password.length() < 8) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
		}
	}

	private String blankToNull(String value) {
		return isBlank(value) ? null : value.trim();
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
