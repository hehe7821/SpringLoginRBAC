package com.metanet.login.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class SignupRequest {
	@Schema(example = "user@example.com")
	private String email;
	@Schema(example = "Password123!")
	private String password;
	@Schema(example = "User Name")
	private String displayName;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
}
