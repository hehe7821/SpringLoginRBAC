package com.metanet.login.domain.user.repository;

import com.metanet.login.domain.user.entity.User;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Param;

public interface UserRepository {
	User findById(@Param("userId") UUID userId);

	User findByEmail(@Param("email") String email);

	boolean existsByEmail(@Param("email") String email);

	User insertUser(
			@Param("email") String email,
			@Param("passwordHash") String passwordHash,
			@Param("displayName") String displayName);

	int assignDefaultUserRole(@Param("userId") UUID userId);

	List<String> findAuthoritiesByUserId(@Param("userId") UUID userId);

	int updateLastLoginAt(@Param("userId") UUID userId);

	int updateDisplayName(@Param("userId") UUID userId, @Param("displayName") String displayName);

	int updatePassword(@Param("userId") UUID userId, @Param("passwordHash") String passwordHash);

	int softDelete(@Param("userId") UUID userId);
}
