package com.metanet.login.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class PasswordResetConfirmRequest {
	@Schema(description = "Password reset token issued by reset request API")
	private String resetToken;
	@Schema(example = "NewPassword123!")
	private String newPassword;

	public String getResetToken() {
		return resetToken;
	}

	public void setResetToken(String resetToken) {
		this.resetToken = resetToken;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}
}
