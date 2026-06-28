package com.metanet.login.domain.user.dto;

public class PasswordResetRequestResponse {
	private final String message;
	private final String resetToken;
	private final long expiresIn;

	public PasswordResetRequestResponse(String message, String resetToken, long expiresIn) {
		this.message = message;
		this.resetToken = resetToken;
		this.expiresIn = expiresIn;
	}

	public String getMessage() {
		return message;
	}

	public String getResetToken() {
		return resetToken;
	}

	public long getExpiresIn() {
		return expiresIn;
	}
}
