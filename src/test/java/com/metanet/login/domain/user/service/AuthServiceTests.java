package com.metanet.login.domain.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.metanet.login.domain.user.dto.LoginRequest;
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
	private final AuthService authService = new AuthService(
			userRepository,
			passwordEncoder,
			jwtTokenProvider,
			redisTemplate,
			emailVerificationService);

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
		user.setPasswordHash("encoded-password");
		user.setStatus("ACTIVE");

		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");
		when(userRepository.insertUser("user@example.com", "encoded-password", "Tester")).thenReturn(user);
		when(jwtTokenProvider.createAccessToken(1L, "user@example.com")).thenReturn("access-token");
		when(jwtTokenProvider.createRefreshToken(1L, "user@example.com")).thenReturn("refresh-token");

		authService.signup(request);

		verify(emailVerificationService).requireVerified("user@example.com", EmailVerificationPurpose.SIGNUP);
		verify(userRepository).existsByEmail("user@example.com");
		verify(userRepository).insertUser("user@example.com", "encoded-password", "Tester");
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
		user.setPasswordHash("encoded-password");
		user.setStatus("ACTIVE");

		ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(userRepository.findByEmail("user@example.com")).thenReturn(user);
		when(passwordEncoder.matches("Password123!", "encoded-password")).thenReturn(true);
		when(jwtTokenProvider.createAccessToken(1L, "user@example.com")).thenReturn("access-token");
		when(jwtTokenProvider.createRefreshToken(1L, "user@example.com")).thenReturn("refresh-token");

		authService.login(request);

		verify(userRepository).findByEmail("user@example.com");
		verify(valueOperations).set(any(), any(), any());
	}
}
