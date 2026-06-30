package com.metanet.login.global.security.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
		implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
	private static final String AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
	private static final int COOKIE_EXPIRE_SECONDS = 180;

	@Override
	public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
		Cookie cookie = WebUtils.getCookie(request, AUTHORIZATION_REQUEST_COOKIE_NAME);
		if (cookie == null) {
			return null;
		}
		return deserialize(cookie.getValue());
	}

	@Override
	public void saveAuthorizationRequest(
			OAuth2AuthorizationRequest authorizationRequest,
			HttpServletRequest request,
			HttpServletResponse response) {
		if (authorizationRequest == null) {
			deleteCookie(request, response);
			return;
		}
		addCookie(request, response, serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);
	}

	@Override
	public OAuth2AuthorizationRequest removeAuthorizationRequest(
			HttpServletRequest request,
			HttpServletResponse response) {
		OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
		deleteCookie(request, response);
		return authorizationRequest;
	}

	private void addCookie(
			HttpServletRequest request,
			HttpServletResponse response,
			String value,
			int maxAgeSeconds) {
		ResponseCookie cookie = ResponseCookie.from(AUTHORIZATION_REQUEST_COOKIE_NAME, value)
				.path("/")
				.maxAge(maxAgeSeconds)
				.httpOnly(true)
				.secure(request.isSecure())
				.sameSite("Lax")
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private void deleteCookie(HttpServletRequest request, HttpServletResponse response) {
		addCookie(request, response, "", 0);
	}

	private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (ObjectOutputStream outputStream = new ObjectOutputStream(bytes)) {
				outputStream.writeObject(authorizationRequest);
			}
			return Base64.getUrlEncoder().encodeToString(bytes.toByteArray());
		} catch (Exception e) {
			throw new IllegalStateException("Failed to serialize OAuth2 authorization request", e);
		}
	}

	private OAuth2AuthorizationRequest deserialize(String value) {
		try {
			byte[] bytes = Base64.getUrlDecoder().decode(value);
			try (ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
				Object object = inputStream.readObject();
				if (object instanceof OAuth2AuthorizationRequest authorizationRequest) {
					return authorizationRequest;
				}
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}
}
