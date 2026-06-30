package com.metanet.login.domain.user.service;

import com.metanet.login.domain.user.dto.email.EmailVerificationConfirmRequest;
import com.metanet.login.domain.user.dto.email.EmailVerificationPurpose;
import com.metanet.login.domain.user.dto.email.EmailVerificationRequest;
import com.metanet.login.domain.user.dto.email.EmailVerificationResponse;
import com.metanet.login.domain.user.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailVerificationService {
	private static final String CODE_KEY_PREFIX = "auth:email-verification:code:";
	private static final String VERIFIED_KEY_PREFIX = "auth:email-verification:verified:";
	private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
	private static final int CODE_LENGTH = 6;

	private final UserRepository userRepository;
	private final StringRedisTemplate redisTemplate;
	private final JavaMailSender mailSender;
	private final SecureRandom secureRandom = new SecureRandom();
	private final String fromAddress;
	private final long codeTtlSeconds;
	private final long verifiedTtlSeconds;

	public EmailVerificationService(
			UserRepository userRepository,
			StringRedisTemplate redisTemplate,
			JavaMailSender mailSender,
			@Value("${app.email.verification.from}") String fromAddress,
			@Value("${app.email.verification.code-ttl-seconds}") long codeTtlSeconds,
			@Value("${app.email.verification.verified-ttl-seconds}") long verifiedTtlSeconds) {
		this.userRepository = userRepository;
		this.redisTemplate = redisTemplate;
		this.mailSender = mailSender;
		this.fromAddress = fromAddress;
		this.codeTtlSeconds = codeTtlSeconds;
		this.verifiedTtlSeconds = verifiedTtlSeconds;
	}

	public EmailVerificationResponse request(EmailVerificationRequest request) {
		String email = normalizeEmail(request == null ? null : request.getEmail());
		EmailVerificationPurpose purpose = requirePurpose(request == null ? null : request.getPurpose());
		if (!shouldSendVerification(email, purpose)) {
			return new EmailVerificationResponse("Verification code sent", codeTtlSeconds);
		}

		String code = generateCode();
		String codeKey = codeKey(email, purpose);
		redisTemplate.opsForValue().set(codeKey, code, Duration.ofSeconds(codeTtlSeconds));
		try {
			sendVerificationEmail(email, purpose, code);
		} catch (ResponseStatusException e) {
			redisTemplate.delete(codeKey);
			throw e;
		}
		return new EmailVerificationResponse("Verification code sent", codeTtlSeconds);
	}

	public EmailVerificationResponse confirm(EmailVerificationConfirmRequest request) {
		String email = normalizeEmail(request == null ? null : request.getEmail());
		EmailVerificationPurpose purpose = requirePurpose(request == null ? null : request.getPurpose());
		String code = normalizeCode(request == null ? null : request.getCode());

		String codeKey = codeKey(email, purpose);
		String savedCode = redisTemplate.opsForValue().get(codeKey);
		if (savedCode == null || !savedCode.equals(code)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid verification code");
		}

		redisTemplate.delete(codeKey);
		redisTemplate.opsForValue().set(
				verifiedKey(email, purpose),
				"true",
				Duration.ofSeconds(verifiedTtlSeconds));
		return new EmailVerificationResponse("Email verified", verifiedTtlSeconds);
	}

	public void requireVerified(String email, EmailVerificationPurpose purpose) {
		String normalizedEmail = normalizeEmail(email);
		if (!Boolean.TRUE.equals(redisTemplate.hasKey(verifiedKey(normalizedEmail, purpose)))) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Email verification is required");
		}
	}

	public void consumeVerified(String email, EmailVerificationPurpose purpose) {
		redisTemplate.delete(verifiedKey(normalizeEmail(email), purpose));
	}

	private boolean shouldSendVerification(String email, EmailVerificationPurpose purpose) {
		boolean exists = userRepository.existsByEmail(email);
		if (purpose == EmailVerificationPurpose.SIGNUP && exists) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
		}
		if (purpose == EmailVerificationPurpose.PASSWORD_RESET && !exists) {
			return false;
		}
		return true;
	}

	private void sendVerificationEmail(String email, EmailVerificationPurpose purpose, String code) {
		if (isBlank(fromAddress)) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Email sender is not configured");
		}

		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(fromAddress);
		message.setTo(email);
		message.setSubject("[Spring Auth] 이메일 인증 코드 발송");
		message.setText("""
				인증코드 : %s

				Purpose: %s
				이 코드는 %d분 뒤에 만료됩니다..
				""".formatted(code, purpose.name(), Math.max(1, codeTtlSeconds / 60)));
		try {
			mailSender.send(message);
		} catch (MailException e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to send verification email", e);
		}
	}

	private String generateCode() {
		StringBuilder code = new StringBuilder(CODE_LENGTH);
		for (int i = 0; i < CODE_LENGTH; i++) {
			code.append(CODE_CHARS[secureRandom.nextInt(CODE_CHARS.length)]);
		}
		return code.toString();
	}

	private EmailVerificationPurpose requirePurpose(EmailVerificationPurpose purpose) {
		if (purpose == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification purpose is required");
		}
		return purpose;
	}

	private String normalizeEmail(String email) {
		if (isBlank(email) || !email.contains("@")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid email is required");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private String normalizeCode(String code) {
		if (isBlank(code)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code is required");
		}
		return code.trim().toUpperCase(Locale.ROOT);
	}

	private String codeKey(String email, EmailVerificationPurpose purpose) {
		return CODE_KEY_PREFIX + purpose.name() + ":" + email;
	}

	private String verifiedKey(String email, EmailVerificationPurpose purpose) {
		return VERIFIED_KEY_PREFIX + purpose.name() + ":" + email;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
