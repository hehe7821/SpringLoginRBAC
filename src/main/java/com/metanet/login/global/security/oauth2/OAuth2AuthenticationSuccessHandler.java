package com.metanet.login.global.security.oauth2;

import com.metanet.login.domain.user.entity.User;
import com.metanet.login.domain.user.service.OAuth2LoginCodeService;
import com.metanet.login.domain.user.service.OAuth2LoginService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
	private final OAuth2LoginService oAuth2LoginService;
	private final OAuth2LoginCodeService oAuth2LoginCodeService;
	private final String authorizedRedirectUri;
	private final String failureRedirectUri;

	public OAuth2AuthenticationSuccessHandler(
			OAuth2LoginService oAuth2LoginService,
			OAuth2LoginCodeService oAuth2LoginCodeService,
			@Value("${app.oauth2.authorized-redirect-uri:http://localhost:3000/oauth2/success}") String authorizedRedirectUri,
			@Value("${app.oauth2.failure-redirect-uri:http://localhost:3000/login?error=oauth2}") String failureRedirectUri) {
		this.oAuth2LoginService = oAuth2LoginService;
		this.oAuth2LoginCodeService = oAuth2LoginCodeService;
		this.authorizedRedirectUri = authorizedRedirectUri;
		this.failureRedirectUri = failureRedirectUri;
	}

	@Override
	public void onAuthenticationSuccess(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		if (!(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		try {
			User user = oAuth2LoginService.loginWithGoogle(oAuth2User);
			String code = oAuth2LoginCodeService.createLoginCode(user.getUserId());
			String redirectUri = UriComponentsBuilder.fromUriString(authorizedRedirectUri)
					.queryParam("code", code)
					.build()
					.toUriString();
			response.sendRedirect(redirectUri);
		} catch (ResponseStatusException e) {
			response.sendRedirect(failureRedirectUri);
		}
	}
}
