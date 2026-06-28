package com.metanet.login.domain.user.dto;

import com.metanet.login.domain.user.entity.User;
import java.time.OffsetDateTime;
import java.util.UUID;

public class UserResponse {
	private final UUID userId;
	private final String email;
	private final String displayName;
	private final String status;
	private final OffsetDateTime emailVerifiedAt;
	private final OffsetDateTime lastLoginAt;
	private final OffsetDateTime createdAt;
	private final OffsetDateTime updatedAt;

	public UserResponse(User user) {
		this.userId = user.getUserId();
		this.email = user.getEmail();
		this.displayName = user.getDisplayName();
		this.status = user.getStatus();
		this.emailVerifiedAt = user.getEmailVerifiedAt();
		this.lastLoginAt = user.getLastLoginAt();
		this.createdAt = user.getCreatedAt();
		this.updatedAt = user.getUpdatedAt();
	}

	public UUID getUserId() {
		return userId;
	}

	public String getEmail() {
		return email;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getStatus() {
		return status;
	}

	public OffsetDateTime getEmailVerifiedAt() {
		return emailVerifiedAt;
	}

	public OffsetDateTime getLastLoginAt() {
		return lastLoginAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
