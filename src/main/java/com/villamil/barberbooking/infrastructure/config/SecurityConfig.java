package com.villamil.barberbooking.infrastructure.config;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.villamil.barberbooking.infrastructure.security.JwtAuthenticationFilter;
import com.villamil.barberbooking.infrastructure.tenant.TemporaryTenantHeaderFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtAuthenticationFilter jwtAuthenticationFilter,
			TemporaryTenantHeaderFilter temporaryTenantHeaderFilter,
			ObjectMapper objectMapper
	) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(
								"/api/v1/auth/login",
								"/api/v1/auth/bootstrap",
								"/actuator/health",
								"/actuator/health/**",
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/v3/api-docs/**"
						).permitAll()
						.requestMatchers("/api/v1/user-accounts/**").hasAnyRole("PLATFORM_OWNER", "COMPANY_OWNER", "BRANCH_MANAGER")
						.requestMatchers(HttpMethod.GET, "/api/v1/barbers/*/availability")
								.hasAnyRole("PLATFORM_OWNER", "COMPANY_OWNER", "BRANCH_MANAGER", "RECEPTIONIST", "BARBER")
						.requestMatchers(HttpMethod.GET, "/api/v1/barbers/*/daily-dashboard")
								.hasAnyRole("PLATFORM_OWNER", "COMPANY_OWNER", "BRANCH_MANAGER", "RECEPTIONIST", "BARBER")
						.requestMatchers(HttpMethod.GET, "/api/v1/barbers/*/appointments")
								.hasAnyRole("PLATFORM_OWNER", "COMPANY_OWNER", "BRANCH_MANAGER", "RECEPTIONIST", "BARBER")
						.requestMatchers("/api/v1/appointments/**")
								.hasAnyRole("PLATFORM_OWNER", "COMPANY_OWNER", "BRANCH_MANAGER", "RECEPTIONIST", "BARBER")
						.requestMatchers("/api/v1/customers/**")
								.hasAnyRole("PLATFORM_OWNER", "COMPANY_OWNER", "BRANCH_MANAGER", "RECEPTIONIST")
						.requestMatchers("/api/v1/barbers/**", "/api/v1/services/**")
								.hasAnyRole("PLATFORM_OWNER", "COMPANY_OWNER", "BRANCH_MANAGER")
						.requestMatchers("/api/v1/**").authenticated()
						.anyRequest().authenticated()
				)
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) ->
								writeProblem(response, objectMapper, request, HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication is required"))
						.accessDeniedHandler((request, response, exception) ->
								writeProblem(response, objectMapper, request, HttpStatus.FORBIDDEN, "Forbidden", "Insufficient permissions"))
				)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(temporaryTenantHeaderFilter, JwtAuthenticationFilter.class)
				.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService() {
		return username -> {
			throw new UsernameNotFoundException("UserDetailsService is not used for JWT authentication");
		};
	}

	@Bean
	FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(JwtAuthenticationFilter filter) {
		FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	FilterRegistrationBean<TemporaryTenantHeaderFilter> temporaryTenantHeaderFilterRegistration(TemporaryTenantHeaderFilter filter) {
		FilterRegistrationBean<TemporaryTenantHeaderFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}

	private void writeProblem(
			HttpServletResponse response,
			ObjectMapper objectMapper,
			HttpServletRequest request,
			HttpStatus status,
			String title,
			String detail
	) throws IOException {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
		problemDetail.setTitle(title);
		problemDetail.setProperty("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
		problemDetail.setProperty("status", status.value());
		problemDetail.setProperty("error", status.getReasonPhrase());
		problemDetail.setProperty("message", detail);
		problemDetail.setProperty("path", request.getRequestURI());
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problemDetail);
	}
}
