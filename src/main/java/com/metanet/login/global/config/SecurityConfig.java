package com.metanet.login.global.config;

import com.metanet.login.global.exception.CustomAccessDeniedHandler;
import com.metanet.login.global.security.jwt.JwtAuthenticationEntryPoint;
import com.metanet.login.global.security.jwt.JwtAuthenticationFilter;
import com.metanet.login.global.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.metanet.login.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.metanet.login.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
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
	private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
	private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
	private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

	public SecurityConfig(
			JwtAuthenticationFilter jwtAuthenticationFilter,
			JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
			CustomAccessDeniedHandler customAccessDeniedHandler,
			HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository,
			OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
			OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
		this.customAccessDeniedHandler = customAccessDeniedHandler;
		this.authorizationRequestRepository = authorizationRequestRepository;
		this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
		this.oAuth2AuthenticationFailureHandler = oAuth2AuthenticationFailureHandler;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(jwtAuthenticationEntryPoint)
						.accessDeniedHandler(customAccessDeniedHandler))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST,
								"/api/v1/auth/signup",
								"/api/v1/auth/login",
								"/api/v1/auth/refresh",
								"/api/v1/auth/oauth2/token",
								"/api/v1/auth/email/verification/request",
								"/api/v1/auth/email/verification/confirm",
								"/api/v1/auth/password/reset").permitAll()
						.requestMatchers(
								"/oauth2/**",
								"/login/oauth2/**",
								"/error",
								"/v3/api-docs/**",
								"/swagger-ui/**",
								"/swagger-ui.html").permitAll()
						.anyRequest().authenticated())
				.oauth2Login(oauth2 -> oauth2
						.authorizationEndpoint(authorization -> authorization
								.authorizationRequestRepository(authorizationRequestRepository))
						.successHandler(oAuth2AuthenticationSuccessHandler)
						.failureHandler(oAuth2AuthenticationFailureHandler))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
