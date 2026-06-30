package com.metanet.login.domain.user.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OAuth2LoginCodeService {
	private static final String LOGIN_CODE_KEY_PREFIX = "auth:oauth2:login-code:";
	private static final int CODE_BYTES = 32;

	private final StringRedisTemplate redisTemplate;
	private final SecureRandom secureRandom = new SecureRandom();
	private final long codeTtlSeconds;

	public OAuth2LoginCodeService(
			StringRedisTemplate redisTemplate,
			@Value("${app.oauth2.login-code-ttl-seconds:60}") long codeTtlSeconds) {
		this.redisTemplate = redisTemplate;
		this.codeTtlSeconds = codeTtlSeconds;
	}

	public String createLoginCode(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("userId is required");
		}
		byte[] bytes = new byte[CODE_BYTES];
		secureRandom.nextBytes(bytes);
		String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		redisTemplate.opsForValue().set(
				loginCodeKey(code),
				userId.toString(),
				Duration.ofSeconds(codeTtlSeconds));
		return code;
	}

	public Long consumeLoginCode(String code) {
		if (isBlank(code)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OAuth2 login code is required");
		}
		String key = loginCodeKey(code.trim());
		String userId = redisTemplate.opsForValue().getAndDelete(key);
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OAuth2 login code");
		}
		try {
			return Long.parseLong(userId);
		} catch (NumberFormatException e) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OAuth2 login code");
		}
	}

	private String loginCodeKey(String code) {
		return LOGIN_CODE_KEY_PREFIX + code;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
