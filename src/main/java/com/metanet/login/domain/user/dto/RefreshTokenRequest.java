package com.metanet.login.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class RefreshTokenRequest {
	@Schema(description = "Refresh JWT")
	private String refreshToken;

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
}
