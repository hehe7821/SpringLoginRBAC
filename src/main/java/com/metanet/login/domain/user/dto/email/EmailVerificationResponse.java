package com.metanet.login.domain.user.dto.email;

public class EmailVerificationResponse {
	private final String message;
	private final long expiresIn;

	public EmailVerificationResponse(String message, long expiresIn) {
		this.message = message;
		this.expiresIn = expiresIn;
	}

	public String getMessage() {
		return message;
	}

	public long getExpiresIn() {
		return expiresIn;
	}
}
