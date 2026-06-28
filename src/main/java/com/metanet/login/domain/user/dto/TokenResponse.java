package com.metanet.login.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public class TokenResponse {
	@Schema(example = "Bearer")
	private String tokenType = "Bearer";
	@Schema(description = "Access JWT")
	private String accessToken;
	@Schema(description = "Refresh JWT")
	private String refreshToken;
	@Schema(description = "Access token expiration seconds", example = "900")
	private long expiresIn;
	@Schema(description = "Authenticated user id")
	private UUID userId;

	public TokenResponse(String accessToken, String refreshToken, long expiresIn, UUID userId) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.expiresIn = expiresIn;
		this.userId = userId;
	}

	public String getTokenType() {
		return tokenType;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public long getExpiresIn() {
		return expiresIn;
	}

	public UUID getUserId() {
		return userId;
	}
}
