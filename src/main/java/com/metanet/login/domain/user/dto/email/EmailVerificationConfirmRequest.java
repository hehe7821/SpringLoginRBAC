package com.metanet.login.domain.user.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;

public class EmailVerificationConfirmRequest {
	@Schema(example = "user@example.com")
	private String email;

	@Schema(example = "SIGNUP", allowableValues = {"SIGNUP", "PASSWORD_RESET"})
	private EmailVerificationPurpose purpose;

	@Schema(example = "A1B2C3")
	private String code;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public EmailVerificationPurpose getPurpose() {
		return purpose;
	}

	public void setPurpose(EmailVerificationPurpose purpose) {
		this.purpose = purpose;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
