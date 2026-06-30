package com.metanet.login.domain.user.service;

import com.metanet.login.domain.user.dto.LoginRequest;
import com.metanet.login.domain.user.dto.OAuth2TokenRequest;
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
import java.util.Locale;
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
	private final OAuth2LoginCodeService oAuth2LoginCodeService;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenProvider jwtTokenProvider,
			StringRedisTemplate redisTemplate,
			EmailVerificationService emailVerificationService,
			OAuth2LoginCodeService oAuth2LoginCodeService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
		this.redisTemplate = redisTemplate;
		this.emailVerificationService = emailVerificationService;
		this.oAuth2LoginCodeService = oAuth2LoginCodeService;
	}

	@Transactional
	public TokenResponse signup(SignupRequest request) {
		String email = normalizeEmail(request == null ? null : request.getEmail());
		String password = request == null ? null : request.getPassword();
		validatePassword(password);
		emailVerificationService.requireVerified(email, EmailVerificationPurpose.SIGNUP);
		if (userRepository.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
		}

		User user = userRepository.insertUser(
				email,
				blankToNull(request.getDisplayName()));
		userRepository.insertUserCredential(user.getUserId(), passwordEncoder.encode(password));
		userRepository.assignDefaultUserRole(user.getUserId());
		emailVerificationService.consumeVerified(email, EmailVerificationPurpose.SIGNUP);
		return issueTokens(user);
	}

	@Transactional
	public TokenResponse login(LoginRequest request) {
		String email = normalizeEmail(request == null ? null : request.getEmail());
		String password = request == null ? null : request.getPassword();
		if (isBlank(password)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
		}

		User user = userRepository.findByEmail(email);
		String passwordHash = user == null ? null : userRepository.findPasswordHashByUserId(user.getUserId());
		if (user == null || passwordHash == null || !passwordEncoder.matches(password, passwordHash)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
		}
		if (!"ACTIVE".equals(user.getStatus()) && !"PENDING".equals(user.getStatus())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not active");
		}

		userRepository.updateLastLoginAt(user.getUserId());
		return issueTokens(user);
	}

	public TokenResponse refresh(RefreshTokenRequest request) {
		String refreshToken = request == null ? null : request.getRefreshToken();
		if (isBlank(refreshToken)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required");
		}

		JwtTokenProvider.JwtClaims claims = parseRefreshToken(refreshToken);
		String redisKey = refreshTokenKey(claims.userId());
		String savedRefreshToken = redisTemplate.opsForValue().get(redisKey);
		if (!refreshToken.equals(savedRefreshToken)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
		}

		User user = requireUser(claims.userId());
		return issueTokens(user);
	}

	public TokenResponse exchangeOAuth2Token(OAuth2TokenRequest request) {
		String code = request == null ? null : request.getCode();
		Long userId = oAuth2LoginCodeService.consumeLoginCode(code);
		User user = requireUser(userId);
		return issueTokens(user);
	}

	public void logout(Authentication authentication) {
		Long userId = currentUserId(authentication);
		redisTemplate.delete(refreshTokenKey(userId));
	}

	@Transactional
	public void resetPassword(PasswordResetRequest request) {
		String email = normalizeEmail(request == null ? null : request.getEmail());
		String newPassword = request == null ? null : request.getNewPassword();
		emailVerificationService.requireVerified(email, EmailVerificationPurpose.PASSWORD_RESET);
		validatePassword(newPassword);
		User user = userRepository.findByEmail(email);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}

		userRepository.updatePassword(user.getUserId(), passwordEncoder.encode(newPassword));
		emailVerificationService.consumeVerified(email, EmailVerificationPurpose.PASSWORD_RESET);
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
		userRepository.softDeleteOAuthAccounts(userId);
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

	private String normalizeEmail(String email) {
		if (isBlank(email) || !email.contains("@")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid email is required");
		}
		return email.trim().toLowerCase(Locale.ROOT);
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
