package com.metanet.login.global.security;

import com.metanet.login.domain.user.entity.User;
import com.metanet.login.domain.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) {
		User user = userRepository.findByEmail(username);
		if (user == null) {
			throw new UsernameNotFoundException("User not found");
		}
		return buildUserDetails(user);
	}

	public UserDetails loadUserById(UUID userId) {
		User user = userRepository.findById(userId);
		if (user == null) {
			throw new UsernameNotFoundException("User not found");
		}
		return buildUserDetails(user);
	}

	private CustomUserDetails buildUserDetails(User user) {
		List<SimpleGrantedAuthority> authorities = userRepository.findAuthoritiesByUserId(user.getUserId()).stream()
				.map(SimpleGrantedAuthority::new)
				.toList();
		return CustomUserDetails.from(user, authorities);
	}
}
