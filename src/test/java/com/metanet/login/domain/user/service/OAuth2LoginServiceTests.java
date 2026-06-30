package com.metanet.login.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.metanet.login.domain.user.entity.User;
import com.metanet.login.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;

class OAuth2LoginServiceTests {
	private final UserRepository userRepository = mock(UserRepository.class);
	private final OAuth2LoginService oAuth2LoginService = new OAuth2LoginService(userRepository);

	@Test
	void googleLoginCreatesUserAndOAuthAccountForNewVerifiedEmail() {
		OAuth2User googleUser = googleUser("google-sub-1", "User@Example.COM", true, "Google User");

		User createdUser = new User();
		createdUser.setUserId(1L);
		createdUser.setEmail("user@example.com");
		createdUser.setStatus("ACTIVE");

		when(userRepository.insertUser("user@example.com", "Google User")).thenReturn(createdUser);
		when(userRepository.insertOAuthAccount(1L, "GOOGLE", "google-sub-1", "user@example.com", true))
				.thenReturn(1);

		User result = oAuth2LoginService.loginWithGoogle(googleUser);

		assertThat(result.getUserId()).isEqualTo(1L);
		verify(userRepository).findByOAuthAccount("GOOGLE", "google-sub-1");
		verify(userRepository).findByEmail("user@example.com");
		verify(userRepository).insertUser("user@example.com", "Google User");
		verify(userRepository).assignDefaultUserRole(1L);
		verify(userRepository).insertOAuthAccount(1L, "GOOGLE", "google-sub-1", "user@example.com", true);
		verify(userRepository).updateLastLoginAt(1L);
	}

	@Test
	void googleLoginConnectsExistingUserWithSameVerifiedEmail() {
		OAuth2User googleUser = googleUser("google-sub-2", "user@example.com", true, "Google User");

		User existingUser = new User();
		existingUser.setUserId(2L);
		existingUser.setEmail("user@example.com");
		existingUser.setStatus("ACTIVE");

		when(userRepository.findByEmail("user@example.com")).thenReturn(existingUser);
		when(userRepository.insertOAuthAccount(2L, "GOOGLE", "google-sub-2", "user@example.com", true))
				.thenReturn(1);

		User result = oAuth2LoginService.loginWithGoogle(googleUser);

		assertThat(result.getUserId()).isEqualTo(2L);
		verify(userRepository).insertOAuthAccount(2L, "GOOGLE", "google-sub-2", "user@example.com", true);
		verify(userRepository).updateLastLoginAt(2L);
	}

	@Test
	void googleLoginRejectsUnverifiedEmail() {
		OAuth2User googleUser = googleUser("google-sub-3", "user@example.com", false, "Google User");

		assertThatThrownBy(() -> oAuth2LoginService.loginWithGoogle(googleUser))
				.isInstanceOf(ResponseStatusException.class)
				.extracting("statusCode")
				.isEqualTo(HttpStatus.FORBIDDEN);
	}

	private OAuth2User googleUser(String sub, String email, boolean emailVerified, String name) {
		return new DefaultOAuth2User(
				List.of(new SimpleGrantedAuthority("OIDC_USER")),
				Map.of(
						"sub", sub,
						"email", email,
						"email_verified", emailVerified,
						"name", name),
				"sub");
	}
}
