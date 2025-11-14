package com.sovereingschool.back_common.Utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.sovereingschool.back_common.Models.Usuario;

class JwtUtilTest {

    @Test
    void convertirObjetoABase64_Success() throws IOException {
        // Arrange
        Usuario persona = new Usuario();
        persona.setNombreUsuario("pepito");

        // Act
        String base64 = JwtUtil.convertirObjetoABase64(persona);

        // Assert
        assertNotNull(base64);
        assertFalse(base64.isBlank());

        // Decodificar para asegurar que el resultado sea Base64 válido
        byte[] decoded = Base64.getDecoder().decode(base64);
        assertTrue(decoded.length > 0);
    }
}
