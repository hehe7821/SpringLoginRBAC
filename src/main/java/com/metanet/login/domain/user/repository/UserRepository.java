package com.metanet.login.domain.user.repository;

import com.metanet.login.domain.user.entity.User;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserRepository {
	User findById(@Param("userId") Long userId);

	User findByEmail(@Param("email") String email);

	boolean existsByEmail(@Param("email") String email);

	User insertUser(
			@Param("email") String email,
			@Param("displayName") String displayName);

	int insertUserCredential(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

	String findPasswordHashByUserId(@Param("userId") Long userId);

	int assignDefaultUserRole(@Param("userId") Long userId);

	List<String> findAuthoritiesByUserId(@Param("userId") Long userId);

	int updateLastLoginAt(@Param("userId") Long userId);

	int updateDisplayName(@Param("userId") Long userId, @Param("displayName") String displayName);

	int updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

	User findByOAuthAccount(
			@Param("provider") String provider,
			@Param("providerUserId") String providerUserId);

	int insertOAuthAccount(
			@Param("userId") Long userId,
			@Param("provider") String provider,
			@Param("providerUserId") String providerUserId,
			@Param("email") String email,
			@Param("emailVerified") boolean emailVerified);

	int softDelete(@Param("userId") Long userId);

	int softDeleteOAuthAccounts(@Param("userId") Long userId);
}
