package com.metanet.login.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class PasswordResetRequest {
	@Schema(example = "user@example.com")
	private String email;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
