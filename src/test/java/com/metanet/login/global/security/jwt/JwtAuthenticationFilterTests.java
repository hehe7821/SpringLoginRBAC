package com.metanet.login.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metanet.login.global.security.CustomUserDetailsService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User;

class JwtAuthenticationFilterTests {
	private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
			new ObjectMapper(),
			"test-secret-with-enough-length-for-hmac",
			900,
			1209600);
	private final CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
	private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void invalidUserForValidTokenDoesNotPropagateAsServerError() {
		String token = jwtTokenProvider.createAccessToken(123L, "deleted@example.com");
		when(userDetailsService.loadUserById(123L)).thenThrow(new UsernameNotFoundException("User not found"));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + token);
		MockHttpServletResponse response = new MockHttpServletResponse();

		assertThatCode(() -> filter.doFilter(request, response, new MockFilterChain()))
				.doesNotThrowAnyException();
	}

	@Test
	void disabledUserForValidTokenIsNotAuthenticated() throws Exception {
		String token = jwtTokenProvider.createAccessToken(123L, "locked@example.com");
		User disabledUser = new User("locked@example.com", "password", false, true, true, true, List.of());
		when(userDetailsService.loadUserById(123L)).thenReturn(disabledUser);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + token);
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		org.assertj.core.api.Assertions.assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}
}
