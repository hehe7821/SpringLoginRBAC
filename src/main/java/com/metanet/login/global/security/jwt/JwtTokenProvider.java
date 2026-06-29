package com.metanet.login.global.security.jwt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

	private final ObjectMapper objectMapper;
	private final byte[] secret;
	private final long accessTokenValiditySeconds;
	private final long refreshTokenValiditySeconds;

	public JwtTokenProvider(
			ObjectMapper objectMapper,
			@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.access-token-validity-seconds}") long accessTokenValiditySeconds,
			@Value("${app.jwt.refresh-token-validity-seconds}") long refreshTokenValiditySeconds) {
		this.objectMapper = objectMapper;
		this.secret = secret.getBytes(StandardCharsets.UTF_8);
		this.accessTokenValiditySeconds = accessTokenValiditySeconds;
		this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
	}

	public String createAccessToken(Long userId, String email) {
		return createToken(userId, email, "access", accessTokenValiditySeconds);
	}

	public String createRefreshToken(Long userId, String email) {
		return createToken(userId, email, "refresh", refreshTokenValiditySeconds);
	}

	public JwtClaims parseAndValidate(String token, String expectedType) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length != 3) {
				throw new IllegalArgumentException("Invalid token format");
			}

			String unsignedToken = parts[0] + "." + parts[1];
			byte[] expectedSignature = sign(unsignedToken);
			byte[] actualSignature = BASE64_URL_DECODER.decode(parts[2]);
			if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
				throw new IllegalArgumentException("Invalid token signature");
			}

			Map<String, Object> claims = objectMapper.readValue(
					BASE64_URL_DECODER.decode(parts[1]),
					new TypeReference<>() {});
			String type = String.valueOf(claims.get("type"));
			if (!expectedType.equals(type)) {
				throw new IllegalArgumentException("Invalid token type");
			}

			long exp = ((Number) claims.get("exp")).longValue();
			if (Instant.now().getEpochSecond() >= exp) {
				throw new IllegalArgumentException("Expired token");
			}

			return new JwtClaims(
					Long.parseLong(String.valueOf(claims.get("sub"))),
					String.valueOf(claims.get("email")),
					type,
					exp);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid JWT token", e);
		}
	}

	public long getAccessTokenValiditySeconds() {
		return accessTokenValiditySeconds;
	}

	public long getRefreshTokenValiditySeconds() {
		return refreshTokenValiditySeconds;
	}

	private String createToken(Long userId, String email, String type, long validitySeconds) {
		try {
			long now = Instant.now().getEpochSecond();
			Map<String, Object> header = new LinkedHashMap<>();
			header.put("alg", "HS256");
			header.put("typ", "JWT");

			Map<String, Object> claims = new LinkedHashMap<>();
			claims.put("sub", userId.toString());
			claims.put("email", email);
			claims.put("type", type);
			claims.put("iat", now);
			claims.put("exp", now + validitySeconds);

			String encodedHeader = encodeJson(header);
			String encodedClaims = encodeJson(claims);
			String unsignedToken = encodedHeader + "." + encodedClaims;
			return unsignedToken + "." + BASE64_URL_ENCODER.encodeToString(sign(unsignedToken));
		} catch (Exception e) {
			throw new IllegalStateException("Failed to create JWT token", e);
		}
	}

	private String encodeJson(Map<String, Object> value) throws Exception {
		return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
	}

	private byte[] sign(String unsignedToken) throws Exception {
		Mac mac = Mac.getInstance(HMAC_ALGORITHM);
		mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
		return mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8));
	}

	public record JwtClaims(Long userId, String email, String type, long expiresAt) {
	}
}
