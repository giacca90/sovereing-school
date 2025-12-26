package com.sovereingschool.back_base.Configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sovereingschool.back_base.Configurations.Filters.CustomOAuth2SuccessHandler;
import com.sovereingschool.back_base.Configurations.Filters.JwtTokenCookieFilter;
import com.sovereingschool.back_base.Configurations.Filters.JwtTokenValidator;
import com.sovereingschool.back_base.Services.LoginService;
import com.sovereingschool.back_base.Services.UsuarioService;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private JwtTokenValidator jwtTokenValidator;
    private JwtTokenCookieFilter jwtTokenCookieFilter;

    private final ObjectMapper objectMapper;

    private String front;

    private String frontDocker;

    /**
     * Constructor de SecurityConfig
     * 
     * @param jwtTokenValidator
     * @param jwtTokenCookieFilter
     * @param objectMapper
     */
    public SecurityConfig(@Value("${variable.FRONT}") String front,
            @Value("${variable.FRONT_DOCKER}") String frontDocker,
            JwtTokenValidator jwtTokenValidator, JwtTokenCookieFilter jwtTokenCookieFilter,
            ObjectMapper objectMapper) {
        this.front = front;
        this.frontDocker = frontDocker;
        this.jwtTokenValidator = jwtTokenValidator;
        this.jwtTokenCookieFilter = jwtTokenCookieFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            CustomOAuth2SuccessHandler customOAuth2SuccessHandler) {
        return http
                .csrf(csrf -> csrf.disable())
                // .redirectToHttps(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.requestMatchers("/oauth2/**", "/login/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .userDetailsService(inMemoryUserDetailsManager())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .addFilterBefore(corsFilter(), BasicAuthenticationFilter.class)
                .addFilterAfter(jwtTokenCookieFilter, ExceptionTranslationFilter.class)
                .addFilterAfter(jwtTokenValidator, ExceptionTranslationFilter.class)
                .oauth2Login(oauth2 -> oauth2.successHandler(customOAuth2SuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            exception.printStackTrace();
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("OAuth2 error: " + exception.getMessage());
                        }))
                .formLogin(form -> form.disable())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("text/plain");
                            response.getWriter().write(authException.getMessage());
                        }))
                .build();
    }

    @Bean
    public UserDetailsService inMemoryUserDetailsManager() {
        UserDetails guest = User.withUsername("Visitante")
                .password(passwordEncoder().encode("visitante"))
                .roles("GUEST")
                .build();
        return new InMemoryUserDetailsManager(guest);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(LoginService loginService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(loginService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // return NoOpPasswordEncoder.getInstance();
        return new BCryptPasswordEncoder();

    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin(front); // Origen permitido
        config.addAllowedOrigin(frontDocker); // Origen permitido
        config.addAllowedMethod("*"); // Métodos permitidos
        config.addAllowedHeader("*"); // Headers permitidos
        config.setAllowCredentials(true); // Permitir credenciales

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    // Dejar la creación del CustomOAuth2SuccessHandler como bean que recibe los
    // servicios necesarios
    @Bean
    public CustomOAuth2SuccessHandler customOAuth2SuccessHandler(LoginService loginService,
            UsuarioService usuarioService) {
        return new CustomOAuth2SuccessHandler(front, loginService, usuarioService, objectMapper);
    }

}
