package com.financetracker.config;

import com.financetracker.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // Public web pages
                    .requestMatchers("/", "/login", "/register", "/logout").permitAll()
                    // Static resources
                    .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                    // Public API endpoints
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    // Everything else requires authentication
                    .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                    // Redirect browser requests to the login page.
                    .defaultAuthenticationEntryPointFor(
                            (request, response, authException) ->
                                    response.sendRedirect("/login"),
                            request -> request.getHeader("Accept") != null
                                    && request.getHeader("Accept").contains("text/html")
                    )
                    // Return 401 for requests that do not match the HTML condition.
                    .authenticationEntryPoint((request, response, authException) ->
                            response.sendError(
                                    jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED
                            )
                    )
            )
            .addFilterBefore(
                    jwtAuthFilter,
                    UsernamePasswordAuthenticationFilter.class
            );

    return http.build();
}

}