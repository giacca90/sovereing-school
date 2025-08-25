package com.sovereingschool.back_base.Configurations;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.sovereingschool.back_base.Configurations.Filters.CustomOAuth2SuccessHandler;
import com.sovereingschool.back_base.Configurations.Filters.JwtTokenCookieFilter;
import com.sovereingschool.back_base.Configurations.Filters.JwtTokenValidator;
import com.sovereingschool.back_base.Services.LoginService;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtTokenValidator jwtTokenValidator;
    @Autowired
    private JwtTokenCookieFilter jwtTokenCookieFilter;

    @Value("${variable.FRONT}")
    private String front;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .redirectToHttps(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .userDetailsService(inMemoryUserDetailsManager())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .addFilterBefore(corsFilter(), BasicAuthenticationFilter.class)
                .addFilterAfter(jwtTokenCookieFilter, ExceptionTranslationFilter.class)
                .addFilterAfter(jwtTokenValidator, ExceptionTranslationFilter.class)
                .oauth2Login(oauth2 -> oauth2.successHandler(customOAuth2SuccessHandler())
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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
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
        config.addAllowedMethod("*"); // Métodos permitidos
        config.addAllowedHeader("*"); // Headers permitidos
        config.setAllowCredentials(true); // Permitir credenciales

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    @Bean
    public CustomOAuth2SuccessHandler customOAuth2SuccessHandler() {
        return new CustomOAuth2SuccessHandler();
    }

}
