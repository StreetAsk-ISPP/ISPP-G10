package com.streetask.app.configuration;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.streetask.app.configuration.jwt.AuthEntryPointJwt;
import com.streetask.app.configuration.jwt.AuthTokenFilter;
import com.streetask.app.configuration.services.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

	@Autowired
	UserDetailsServiceImpl userDetailsService;

	@Autowired
	private AuthEntryPointJwt unauthorizedHandler;

	@Autowired
	DataSource dataSource;

	@Value("${streetask.websocket.endpoint:/ws}")
	private String websocketEndpoint;

	@Value("${streetask.http.allowed-origin-patterns:http://localhost:8080,http://localhost:8081,http://localhost:19006,https://streetask.expo.app,https://sprint2-streetask.expo.app,https://streetask-preprod-frontend.onrender.com, https://sprint3-streetask.expo.app}")
	private String[] allowedHttpOriginPatterns;

	private static final String ADMIN = "ADMIN";

	@Bean
	protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
		http
				.cors(withDefaults())
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
				.exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))

				.authorizeHttpRequests(auth -> auth
						.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
						.requestMatchers("/", "/oups").permitAll()
						.requestMatchers(
								"/v3/api-docs/**",
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/swagger-resources/**")
						.permitAll()
						.requestMatchers("/h2-console/**").permitAll()
						.requestMatchers(webSocketHandshakePattern()).permitAll()

						.requestMatchers("/api/v1/auth/**").permitAll()
						.requestMatchers("/api/v1/developers").permitAll()
						.requestMatchers("/api/v1/plan").permitAll()

						.requestMatchers("/api/v1/locations/public/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/locations/user/**").permitAll()

						.requestMatchers("/api/v1/plan").hasAuthority("OWNER")

						.requestMatchers(HttpMethod.GET, "/api/v1/users/me/reputation").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/users/*/reputation").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/users/*/stats").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/users/*/questions").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/users/*/answers").authenticated()
						.requestMatchers(HttpMethod.PUT, "/api/v1/users/*").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/users/*").authenticated()

						.requestMatchers(HttpMethod.GET, "/api/v1/users").hasAuthority(ADMIN)
						.requestMatchers(HttpMethod.POST, "/api/v1/users").hasAuthority(ADMIN)
						.requestMatchers(HttpMethod.GET, "/api/v1/users/*").hasAuthority(ADMIN)
						.requestMatchers(HttpMethod.DELETE, "/api/v1/users/*").hasAuthority(ADMIN)

						// Restricted API for administrators
						.requestMatchers("/api/v1/users/**").hasAuthority(ADMIN)
						.requestMatchers("/api/v1/users").hasAuthority(ADMIN)

						// Questions & Answers require auth
						.requestMatchers("/api/v1/questions/**").authenticated()
						.requestMatchers("/api/v1/answers", "/api/v1/answers/**").authenticated()
						.requestMatchers("/api/v1/reports/**").authenticated()

						// Feedback requires auth
						.requestMatchers("/api/v1/feedback", "/api/v1/feedback/**").authenticated()

						// Push devices require auth
						.requestMatchers("/api/push-devices/**").authenticated()

						// Moderation endpoints require ADMIN authority
						.requestMatchers("/api/v1/moderation/**").hasAuthority(ADMIN)

						.anyRequest().denyAll())

				.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public AuthTokenFilter authenticationJwtTokenFilter() {
		return new AuthTokenFilter();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(List.of(allowedHttpOriginPatterns));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	private String webSocketHandshakePattern() {
		String normalized = websocketEndpoint == null ? "/ws" : websocketEndpoint.trim();
		if (!normalized.startsWith("/")) {
			normalized = "/" + normalized;
		}
		if (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized + "/**";
	}
}