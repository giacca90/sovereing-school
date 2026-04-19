package com.sovereingschool.back_streaming.Configurations;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.filter.CorsFilter;

/**
 * Tests para validar la configuración de seguridad.
 * 
 * Verifica que los beans de seguridad se creen correctamente, incluyendo
 * el filtro de CORS, el PasswordEncoder, y el SecurityFilterChain.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "variable.FRONT=http://localhost:3000"
})
@DisplayName("SecurityConfig - Configuración de Seguridad")
class SecurityConfigTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CorsFilter corsFilter;

    @Test
    @DisplayName("debe crear el bean PasswordEncoder correctamente")
    void shouldCreatePasswordEncoderBean() {
        assertNotNull(passwordEncoder, "El PasswordEncoder no debe ser null");
    }

    @Test
    @DisplayName("debe usar BCryptPasswordEncoder")
    void shouldUseBCryptPasswordEncoder() {
        assertInstanceOf(BCryptPasswordEncoder.class, passwordEncoder,
                "El PasswordEncoder debe ser instancia de BCryptPasswordEncoder");
    }

    @Test
    @DisplayName("debe crear el bean SecurityFilterChain correctamente")
    void shouldCreateSecurityFilterChainBean() {
        assertNotNull(securityFilterChain, "El SecurityFilterChain no debe ser null");
    }

    @Test
    @DisplayName("debe crear el bean AuthenticationManager correctamente")
    void shouldCreateAuthenticationManagerBean() {
        assertNotNull(authenticationManager, "El AuthenticationManager no debe ser null");
    }

    @Test
    @DisplayName("debe crear el bean CorsFilter correctamente")
    void shouldCreateCorsFilterBean() {
        assertNotNull(corsFilter, "El CorsFilter no debe ser null");
    }

    @Test
    @DisplayName("debe encriptar contraseña correctamente")
    void shouldEncryptPasswordCorrectly() {
        String rawPassword = "testPassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword),
                "La contraseña debe validarse contra su versión encriptada");
        assertNotEquals(rawPassword, encodedPassword,
                "La contraseña encriptada no debe ser igual a la original");
    }
}
