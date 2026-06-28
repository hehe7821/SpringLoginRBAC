package com.metanet.login.domain.user.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

public class User {
	private UUID userId;
	private String email;
	private String passwordHash;
	private String displayName;
	private String status;
	private OffsetDateTime emailVerifiedAt;
	private OffsetDateTime lastLoginAt;
	private OffsetDateTime passwordChangedAt;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
	private OffsetDateTime deletedAt;

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public OffsetDateTime getEmailVerifiedAt() {
		return emailVerifiedAt;
	}

	public void setEmailVerifiedAt(OffsetDateTime emailVerifiedAt) {
		this.emailVerifiedAt = emailVerifiedAt;
	}

	public OffsetDateTime getLastLoginAt() {
		return lastLoginAt;
	}

	public void setLastLoginAt(OffsetDateTime lastLoginAt) {
		this.lastLoginAt = lastLoginAt;
	}

	public OffsetDateTime getPasswordChangedAt() {
		return passwordChangedAt;
	}

	public void setPasswordChangedAt(OffsetDateTime passwordChangedAt) {
		this.passwordChangedAt = passwordChangedAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public OffsetDateTime getDeletedAt() {
		return deletedAt;
	}

	public void setDeletedAt(OffsetDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}
}
