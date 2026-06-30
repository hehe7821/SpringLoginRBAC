package com.metanet.login.domain.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.metanet.login.domain.user.dto.LoginRequest;
import com.metanet.login.domain.user.dto.OAuth2TokenRequest;
import com.metanet.login.domain.user.dto.SignupRequest;
import com.metanet.login.domain.user.dto.email.EmailVerificationPurpose;
import com.metanet.login.domain.user.entity.User;
import com.metanet.login.domain.user.repository.UserRepository;
import com.metanet.login.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class AuthServiceTests {
	private final UserRepository userRepository = mock(UserRepository.class);
	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
	private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
	private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
	private final EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
	private final OAuth2LoginCodeService oAuth2LoginCodeService = mock(OAuth2LoginCodeService.class);
	private final AuthService authService = new AuthService(
			userRepository,
			passwordEncoder,
			jwtTokenProvider,
			redisTemplate,
			emailVerificationService,
			oAuth2LoginCodeService);

	@Test
	void signupRejectsNullRequestWithBadRequest() {
		assertThatThrownBy(() -> authService.signup(null))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode")
				.isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	@SuppressWarnings("unchecked")
	void signupNormalizesEmailBeforeVerificationLookupAndInsert() {
		SignupRequest request = new SignupRequest();
		request.setEmail(" User@Example.COM ");
		request.setPassword("Password123!");
		request.setDisplayName(" Tester ");

		User user = new User();
		user.setUserId(1L);
		user.setEmail("user@example.com");
		user.setStatus("ACTIVE");

		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");
		when(userRepository.insertUser("user@example.com", "Tester")).thenReturn(user);
		when(jwtTokenProvider.createAccessToken(1L, "user@example.com")).thenReturn("access-token");
		when(jwtTokenProvider.createRefreshToken(1L, "user@example.com")).thenReturn("refresh-token");

		authService.signup(request);

		verify(emailVerificationService).requireVerified("user@example.com", EmailVerificationPurpose.SIGNUP);
		verify(userRepository).existsByEmail("user@example.com");
		verify(userRepository).insertUser("user@example.com", "Tester");
		verify(userRepository).insertUserCredential(1L, "encoded-password");
		verify(emailVerificationService).consumeVerified("user@example.com", EmailVerificationPurpose.SIGNUP);
		verify(valueOperations).set(any(), any(), any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void loginNormalizesEmailBeforeLookup() {
		LoginRequest request = new LoginRequest();
		request.setEmail(" User@Example.COM ");
		request.setPassword("Password123!");

		User user = new User();
		user.setUserId(1L);
		user.setEmail("user@example.com");
		user.setStatus("ACTIVE");

		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(userRepository.findByEmail("user@example.com")).thenReturn(user);
		when(userRepository.findPasswordHashByUserId(1L)).thenReturn("encoded-password");
		when(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(true);
		when(jwtTokenProvider.createAccessToken(1L, "user@example.com")).thenReturn("access-token");
		when(jwtTokenProvider.createRefreshToken(1L, "user@example.com")).thenReturn("refresh-token");

		authService.login(request);

		verify(userRepository).findByEmail("user@example.com");
		verify(userRepository).findPasswordHashByUserId(1L);
		verify(valueOperations).set(any(), any(), any());
	}

	@Test
	void loginRejectsUserWithoutPasswordCredential() {
		LoginRequest request = new LoginRequest();
		request.setEmail("google@example.com");
		request.setPassword("Password123!");

		User user = new User();
		user.setUserId(2L);
		user.setEmail("google@example.com");
		user.setStatus("ACTIVE");

		when(userRepository.findByEmail("google@example.com")).thenReturn(user);

		assertThatThrownBy(() -> authService.login(request))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode")
				.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	@SuppressWarnings("unchecked")
	void exchangeOAuth2TokenIssuesJwtForOneTimeCode() {
		OAuth2TokenRequest request = new OAuth2TokenRequest();
		request.setCode("login-code");

		User user = new User();
		user.setUserId(3L);
		user.setEmail("google@example.com");
		user.setStatus("ACTIVE");

		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(oAuth2LoginCodeService.consumeLoginCode("login-code")).thenReturn(3L);
		when(userRepository.findById(3L)).thenReturn(user);
		when(jwtTokenProvider.createAccessToken(3L, "google@example.com")).thenReturn("access-token");
		when(jwtTokenProvider.createRefreshToken(3L, "google@example.com")).thenReturn("refresh-token");

		authService.exchangeOAuth2Token(request);

		verify(oAuth2LoginCodeService).consumeLoginCode("login-code");
		verify(valueOperations).set(any(), any(), any());
	}
}
