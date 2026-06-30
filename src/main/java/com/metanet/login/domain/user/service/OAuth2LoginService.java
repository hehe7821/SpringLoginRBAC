package com.metanet.login.domain.user.service;

import com.metanet.login.domain.user.entity.User;
import com.metanet.login.domain.user.repository.UserRepository;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OAuth2LoginService {
	private static final String GOOGLE_PROVIDER = "GOOGLE";
	private static final int MAX_DISPLAY_NAME_LENGTH = 50;

	private final UserRepository userRepository;

	public OAuth2LoginService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional
	public User loginWithGoogle(OAuth2User oAuth2User) {
		GoogleProfile profile = extractGoogleProfile(oAuth2User);
		if (!profile.emailVerified()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Google email verification is required");
		}

		User linkedUser = userRepository.findByOAuthAccount(GOOGLE_PROVIDER, profile.providerUserId());
		if (linkedUser != null) {
			return completeLogin(linkedUser);
		}

		User user = userRepository.findByEmail(profile.email());
		boolean createdUser = false;
		if (user == null) {
			user = userRepository.insertUser(profile.email(), normalizeDisplayName(profile.displayName()));
			userRepository.assignDefaultUserRole(user.getUserId());
			createdUser = true;
		}
		ensureLoginAllowed(user);

		try {
			int inserted = userRepository.insertOAuthAccount(
					user.getUserId(),
					GOOGLE_PROVIDER,
					profile.providerUserId(),
					profile.email(),
					true);
			if (inserted == 0) {
				if (createdUser) {
					throw new ResponseStatusException(HttpStatus.CONFLICT, "Google account is already connected");
				}
				User concurrentLinkedUser = userRepository.findByOAuthAccount(
						GOOGLE_PROVIDER,
						profile.providerUserId());
				if (concurrentLinkedUser != null) {
					return completeLogin(concurrentLinkedUser);
				}
				throw new ResponseStatusException(HttpStatus.CONFLICT, "Google account is already connected");
			}
		} catch (DataIntegrityViolationException e) {
			User concurrentLinkedUser = userRepository.findByOAuthAccount(
					GOOGLE_PROVIDER,
					profile.providerUserId());
			if (concurrentLinkedUser != null) {
				return completeLogin(concurrentLinkedUser);
			}
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Google account is already connected", e);
		}

		return completeLogin(user);
	}

	private User completeLogin(User user) {
		ensureLoginAllowed(user);
		userRepository.updateLastLoginAt(user.getUserId());
		return user;
	}

	private void ensureLoginAllowed(User user) {
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OAuth2 login");
		}
		if (!"ACTIVE".equals(user.getStatus()) && !"PENDING".equals(user.getStatus())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not active");
		}
	}

	private GoogleProfile extractGoogleProfile(OAuth2User oAuth2User) {
		if (oAuth2User == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OAuth2 login");
		}
		String providerUserId = requireStringAttribute(oAuth2User, "sub");
		String email = normalizeEmail(requireStringAttribute(oAuth2User, "email"));
		boolean emailVerified = requireBooleanAttribute(oAuth2User, "email_verified");
		String displayName = stringAttribute(oAuth2User, "name");
		return new GoogleProfile(providerUserId, email, emailVerified, displayName);
	}

	private String requireStringAttribute(OAuth2User oAuth2User, String name) {
		String value = stringAttribute(oAuth2User, name);
		if (isBlank(value)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google profile");
		}
		return value.trim();
	}

	private String stringAttribute(OAuth2User oAuth2User, String name) {
		Object value = oAuth2User.getAttribute(name);
		return value == null ? null : String.valueOf(value);
	}

	private boolean requireBooleanAttribute(OAuth2User oAuth2User, String name) {
		Object value = oAuth2User.getAttribute(name);
		if (value instanceof Boolean booleanValue) {
			return booleanValue;
		}
		if (value instanceof String stringValue) {
			return Boolean.parseBoolean(stringValue);
		}
		return false;
	}

	private String normalizeEmail(String email) {
		if (isBlank(email) || !email.contains("@")) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google profile");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private String normalizeDisplayName(String displayName) {
		if (isBlank(displayName)) {
			return null;
		}
		String normalized = displayName.trim();
		if (normalized.length() <= MAX_DISPLAY_NAME_LENGTH) {
			return normalized;
		}
		return normalized.substring(0, MAX_DISPLAY_NAME_LENGTH);
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private record GoogleProfile(
			String providerUserId,
			String email,
			boolean emailVerified,
			String displayName) {
	}
}
