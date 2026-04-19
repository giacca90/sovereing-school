package com.sovereingschool.back_chat.Models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

class ModelsTest {

    @Test
    void testMensajeChat() {
        MensajeChat msg = new MensajeChat();
        msg.setId("1");
        msg.setIdUsuario(1L);
        msg.setIdCurso(2L);
        msg.setIdClase(3L);
        msg.setMensaje("Hola");
        msg.setRespuesta("0");
        msg.setMomento(1);
        msg.setFecha(new Date());

        assertEquals("1", msg.getId());
        assertEquals(1L, msg.getIdUsuario());
        assertEquals(2L, msg.getIdCurso());
        assertEquals(3L, msg.getIdClase());
        assertEquals("Hola", msg.getMensaje());
        assertEquals("0", msg.getRespuesta());
        assertEquals(1, msg.getMomento());
        assertNotNull(msg.getFecha());
    }

    @Test
    void testUsuarioChat() {
        UsuarioChat user = new UsuarioChat();
        user.setId("u1");
        user.setIdUsuario(1L);
        user.setMensajes(new ArrayList<>(List.of("m1")));
        user.setCursos(new ArrayList<>(List.of("c1")));

        assertEquals("u1", user.getId());
        assertEquals(1L, user.getIdUsuario());
        assertEquals(1, user.getMensajes().size());
        assertEquals(1, user.getCursos().size());
    }

    @Test
    void testClaseChat() {
        ClaseChat clase = new ClaseChat();
        clase.setIdClase(10L);
        clase.setIdCurso(100L);
        clase.setMensajes(new ArrayList<>(List.of("m1")));

        assertEquals(10L, clase.getIdClase());
        assertEquals(100L, clase.getIdCurso());
        assertEquals(1, clase.getMensajes().size());
    }

    @Test
    void testCursoChat() {
        CursoChat curso = new CursoChat();
        curso.setId("cu1");
        curso.setIdCurso(100L);
        curso.setClases(new ArrayList<>(List.of(new ClaseChat())));
        curso.setMensajes(new ArrayList<>(List.of("m1")));

        assertEquals("cu1", curso.getId());
        assertEquals(100L, curso.getIdCurso());
        assertEquals(1, curso.getClases().size());
        assertEquals(1, curso.getMensajes().size());
    }
}
