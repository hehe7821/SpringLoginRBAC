package com.metanet.login.global.config;

import com.metanet.login.global.exception.CustomAccessDeniedHandler;
import com.metanet.login.global.security.jwt.JwtAuthenticationEntryPoint;
import com.metanet.login.global.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final CustomAccessDeniedHandler customAccessDeniedHandler;

	public SecurityConfig(
			JwtAuthenticationFilter jwtAuthenticationFilter,
			JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
			CustomAccessDeniedHandler customAccessDeniedHandler) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
		this.customAccessDeniedHandler = customAccessDeniedHandler;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(jwtAuthenticationEntryPoint)
						.accessDeniedHandler(customAccessDeniedHandler))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST,
								"/api/v1/auth/signup",
								"/api/v1/auth/login",
								"/api/v1/auth/refresh",
								"/api/v1/auth/password/reset/request",
								"/api/v1/auth/password/reset/confirm").permitAll()
						.requestMatchers(
								"/error",
								"/v3/api-docs/**",
								"/swagger-ui/**",
								"/swagger-ui.html").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
