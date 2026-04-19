package com.sovereingschool.back_chat.DTOs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

class DTOsTest {

    @Test
    void testMensajeChatDTO() {
        MensajeChatDTO dto = new MensajeChatDTO("1", 2L, 3L, 1L, "Curso", "Clase", "User", "fotoC", "fotoU", null, 0,
                "Hola", new Date());

        assertEquals("1", dto.idMensaje());
        assertEquals(1L, dto.idUsuario());
        assertEquals(2L, dto.idCurso());
        assertEquals(3L, dto.idClase());
        assertEquals("Hola", dto.mensaje());
        assertEquals(0, dto.pregunta());
        assertNotNull(dto.fecha());
    }

    @Test
    void testClaseChatDTO() {
        ClaseChatDTO dto = new ClaseChatDTO(10L, 100L, "Nombre", List.of());

        assertEquals(10L, dto.idClase());
        assertEquals(0, dto.mensajes().size());
    }

    @Test
    void testCursoChatDTO() {
        CursoChatDTO dto = new CursoChatDTO(100L, List.of(), List.of(), "Nombre", "Foto");

        assertEquals(100L, dto.idCurso());
        assertEquals(0, dto.clases().size());
        assertEquals(0, dto.mensajes().size());
    }

    @Test
    void testInitChatDTO() {
        InitChatDTO dto = new InitChatDTO(1L, List.of(), List.of());

        assertEquals(1L, dto.idUsuario());
        assertEquals(0, dto.mensajes().size());
        assertEquals(0, dto.cursos().size());
    }
}
