package com.metanet.login.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTests {
	private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
			new ObjectMapper(),
			"test-secret-with-enough-length-for-hmac",
			900,
			1209600);

	@Test
	void accessTokenUsesNumericUserIdSubject() {
		String token = jwtTokenProvider.createAccessToken(123L, "user@example.com");

		JwtTokenProvider.JwtClaims claims = jwtTokenProvider.parseAndValidate(token, "access");

		assertThat(claims.userId()).isEqualTo(123L);
		assertThat(claims.email()).isEqualTo("user@example.com");
		assertThat(claims.type()).isEqualTo("access");
	}
}
