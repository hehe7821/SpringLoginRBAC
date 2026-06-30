package com.metanet.login.global.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {
	private final String failureRedirectUri;

	public OAuth2AuthenticationFailureHandler(
			@Value("${app.oauth2.failure-redirect-uri:http://localhost:3000/login?error=oauth2}") String failureRedirectUri) {
		this.failureRedirectUri = failureRedirectUri;
	}

	@Override
	public void onAuthenticationFailure(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		response.sendRedirect(failureRedirectUri);
	}
}
