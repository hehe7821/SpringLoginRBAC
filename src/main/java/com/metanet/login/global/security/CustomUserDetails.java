package com.metanet.login.global.security;

import com.metanet.login.domain.user.entity.User;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {
	private final Long userId;
	private final String email;
	private final String password;
	private final String status;
	private final Collection<? extends GrantedAuthority> authorities;

	public CustomUserDetails(
			Long userId,
			String email,
			String password,
			String status,
			Collection<? extends GrantedAuthority> authorities) {
		this.userId = userId;
		this.email = email;
		this.password = password;
		this.status = status;
		this.authorities = authorities;
	}

	public static CustomUserDetails from(User user, Collection<? extends GrantedAuthority> authorities) {
		return new CustomUserDetails(
				user.getUserId(),
				user.getEmail(),
				user.getPasswordHash(),
				user.getStatus(),
				authorities);
	}

	public Long getUserId() {
		return userId;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonLocked() {
		return !"LOCKED".equals(status);
	}

	@Override
	public boolean isEnabled() {
		return "ACTIVE".equals(status) || "PENDING".equals(status);
	}
}
