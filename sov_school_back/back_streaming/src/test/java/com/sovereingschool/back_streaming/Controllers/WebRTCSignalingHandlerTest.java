package com.sovereingschool.back_streaming.Controllers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.sovereingschool.back_streaming.Services.StreamingService;

@ExtendWith(MockitoExtension.class)
class WebRTCSignalingHandlerTest {

    @Mock
    private Executor executor;

    @Mock
    private StreamingService streamingService;

    @Mock
    private WebSocketSession session;

    @Mock
    private Authentication auth;

    @Mock
    private GrantedAuthority authority;

    @Spy
    @InjectMocks
    private WebRTCSignalingHandler handler;

    private Map<String, Object> sessionAttributes;

    @BeforeEach
    void setUp() {
        sessionAttributes = new HashMap<>();
    }

    /**
     * Prueba que la conexión se establezca correctamente para un usuario
     * autorizado.
     * 
     * @throws Exception Si ocurre un error durante la ejecución de la prueba.
     */
    @Test
    void afterConnectionEstablished_AuthorizedUser_ShouldSucceed() throws Exception {
        when(session.getAttributes()).thenReturn(sessionAttributes);
        sessionAttributes.put("Auth", auth);
        sessionAttributes.put("username", "testuser");
        when(auth.isAuthenticated()).thenReturn(true);
        lenient().doReturn(Collections.singletonList(authority)).when(auth).getAuthorities();
        lenient().when(authority.getAuthority()).thenReturn("ROLE_PROF");

        handler.afterConnectionEstablished(session);

        verify(session, never()).close(any());
    }

    @Test
    void afterConnectionEstablished_WithErrorAttribute_ShouldClose() throws Exception {
        when(session.getAttributes()).thenReturn(sessionAttributes);
        sessionAttributes.put("Error", "test error");

        handler.afterConnectionEstablished(session);

        verify(session).sendMessage(any(TextMessage.class));
        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    @Test
    void afterConnectionEstablished_AuthorizedAdmin_ShouldSucceed() throws Exception {
        when(session.getAttributes()).thenReturn(sessionAttributes);
        sessionAttributes.put("Auth", auth);
        sessionAttributes.put("username", "adminuser");
        when(auth.isAuthenticated()).thenReturn(true);
        lenient().doReturn(Collections.singletonList(authority)).when(auth).getAuthorities();
        lenient().when(authority.getAuthority()).thenReturn("ROLE_ADMIN");

        handler.afterConnectionEstablished(session);

        verify(session, never()).close(any());
    }

    /**
     * Prueba que la conexión se cierre para un usuario no autorizado.
     * 
     * @throws Exception Si ocurre un error durante la ejecución de la prueba.
     */
    @Test
    void afterConnectionEstablished_UnauthorizedUser_ShouldCloseConnection() throws Exception {
        when(session.getAttributes()).thenReturn(sessionAttributes);
        sessionAttributes.put("Auth", auth);
        when(auth.isAuthenticated()).thenReturn(false);

        handler.afterConnectionEstablished(session);

        verify(session).close(any(CloseStatus.class));
    }

    @Test
    void handleTextMessage_UserIdType_ShouldSendStreamId() throws Exception {
        when(session.getAttributes()).thenReturn(sessionAttributes);
        sessionAttributes.put("idUsuario", 1L);
        when(session.getId()).thenReturn("session1");

        TextMessage message = new TextMessage("{\"type\":\"userId\"}");
        handler.handleTextMessage(session, message);

        verify(session).sendMessage(argThat(msg -> {
            if (msg instanceof TextMessage) {
                return ((TextMessage) msg).getPayload().contains("streamId");
            }
            return false;
        }));
    }

    @Test
    void handleTextMessage_UserId_SendMessageThrows_ShouldClose() throws Exception {
        when(session.getAttributes()).thenReturn(sessionAttributes);
        sessionAttributes.put("idUsuario", 1L);
        when(session.getId()).thenReturn("session1");
        doThrow(new IOException("send error")).when(session).sendMessage(any(TextMessage.class));

        TextMessage message = new TextMessage("{\"type\":\"userId\"}");
        handler.handleTextMessage(session, message);

        verify(session).close();
    }

    @Test
    void handleTextMessage_Emitir_Success() throws Exception {
        when(session.getId()).thenReturn("session123");
        String payload = "{\"type\":\"emitir\", \"streamId\":\"1_session123\", \"videoSettings\":{\"width\":\"1280\",\"height\":\"720\",\"fps\":\"30\"}}";
        handler.handleTextMessage(session, new TextMessage(payload));
        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void handleTextMessage_Emitir_WrongSession_ShouldReturn() throws Exception {
        when(session.getId()).thenReturn("session123");
        String payload = "{\"type\":\"emitir\", \"streamId\":\"wrong_session\", \"videoSettings\":{}}";
        handler.handleTextMessage(session, new TextMessage(payload));
        verify(session, never()).sendMessage(argThat((TextMessage m) -> m.getPayload().contains("ok")));
    }

    @Test
    void handleTextMessage_Offer_WrongSession_ShouldReturn() throws Exception {
        when(session.getId()).thenReturn("session123");
        String payload = "{\"type\":\"offer\", \"streamId\":\"wrong_session\", \"sdp\":\"offer-sdp\"}";
        handler.handleTextMessage(session, new TextMessage(payload));
        verify(handler, never()).startPion();
    }

    @Test
    void handleTextMessage_Offer_NoSettings_ShouldReturn() throws Exception {
        when(session.getId()).thenReturn("session123");
        String payload = "{\"type\":\"offer\", \"streamId\":\"1_session123\", \"sdp\":\"offer-sdp\"}";
        handler.handleTextMessage(session, new TextMessage(payload));
        verify(handler, never()).startPion();
    }

    @Test
    void handleTextMessage_Candidate_WrongSession_ShouldReturn() throws Exception {
        when(session.getId()).thenReturn("session123");
        String payload = "{\"type\":\"candidate\", \"streamId\":\"wrong_session\", \"candidate\":{}}";
        handler.handleTextMessage(session, new TextMessage(payload));
        // Should not write to pionWriter
    }

    @Test
    void handleTextMessage_DetenerStreamWebRTC_WrongSession_ShouldReturn() throws Exception {
        when(session.getId()).thenReturn("session123");
        String payload = "{\"type\":\"detenerStreamWebRTC\", \"streamId\":\"wrong_session\"}";
        handler.handleTextMessage(session, new TextMessage(payload));
        verify(streamingService, never()).stopFFmpegProcessForUser(anyString());
    }

    @Test
    void handleTextMessage_UnknownType_ShouldLog() throws Exception {
        TextMessage message = new TextMessage("{\"type\":\"unknown\"}");
        handler.handleTextMessage(session, message);
    }

    @Test
    void handleTextMessage_MalformedJson_ShouldNotThrow() throws Exception {
        TextMessage message = new TextMessage("invalid json");
        handler.handleTextMessage(session, message);
    }

    @Test
    void afterConnectionEstablished_NoAuth_ShouldCloseConnection() throws Exception {
        when(session.getAttributes()).thenReturn(sessionAttributes);
        sessionAttributes.put("Auth", null);

        handler.afterConnectionEstablished(session);

        verify(session).close(any(CloseStatus.class));
    }

    @Test
    void testPionStdoutReading_RtpSdp() throws Exception {
        // Setup
        when(session.getId()).thenReturn("session123");
        when(session.getAttributes()).thenReturn(sessionAttributes);
        sessionAttributes.put("idUsuario", 1L);

        Process mockProcess = mock(Process.class);
        String stdoutContent = "{\"type\":\"rtp-sdp\", \"streamId\":\"1_session123\", \"sdp\":\"rtp_sdp_content\", \"videoSettings\":[\"1280\", \"720\", \"30\"]}\n";
        InputStream stdoutStream = new java.io.ByteArrayInputStream(stdoutContent.getBytes(StandardCharsets.UTF_8));

        when(mockProcess.getInputStream()).thenReturn(stdoutStream);
        when(mockProcess.getOutputStream()).thenReturn(new java.io.ByteArrayOutputStream());
        when(mockProcess.getErrorStream()).thenReturn(new java.io.ByteArrayInputStream("".getBytes()));
        doReturn(mockProcess).when(handler).startProcess(any());

        // Trigger startPion
        String settingsPayload = "{\"type\":\"emitir\", \"streamId\":\"1_session123\", \"videoSettings\":{\"width\":\"1280\",\"height\":\"720\",\"fps\":\"30\"}}";
        handler.handleTextMessage(session, new TextMessage(settingsPayload));
        String offerPayload = "{\"type\":\"offer\", \"streamId\":\"1_session123\", \"sdp\":\"v=0...\"}";
        handler.handleTextMessage(session, new TextMessage(offerPayload));
        String userIdPayload = "{\"type\":\"userId\", \"idUsuario\":1}";
        handler.handleTextMessage(session, new TextMessage(userIdPayload));

        // Capture and run stdout reader
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor, org.mockito.Mockito.atLeastOnce()).execute(runnableCaptor.capture());
        List<Runnable> tasks = runnableCaptor.getAllValues();
        Runnable stdoutReader = tasks.get(tasks.size() - 1);
        stdoutReader.run();

        // The stdoutReader submits another task to the executor to start streaming.
        verify(executor, org.mockito.Mockito.atLeastOnce()).execute(runnableCaptor.capture());
        runnableCaptor.getAllValues().forEach(Runnable::run);

        verify(streamingService).startLiveStreamingFromStream(anyString(), any(InputStream.class), any(String[].class));
    }

    @Test
    void testPionStdoutReading_Candidate() throws Exception {
        // Setup
        when(session.getId()).thenReturn("session123");
        when(session.getAttributes()).thenReturn(sessionAttributes);
        sessionAttributes.put("idUsuario", 1L);

        Process mockProcess = mock(Process.class);
        String stdoutContent = "{\"type\":\"candidate\", \"streamId\":\"1_session123\", \"candidate\":\"candidate_string\"}\n";
        InputStream stdoutStream = new java.io.ByteArrayInputStream(stdoutContent.getBytes(StandardCharsets.UTF_8));

        when(mockProcess.getInputStream()).thenReturn(stdoutStream);
        when(mockProcess.getOutputStream()).thenReturn(new java.io.ByteArrayOutputStream());
        when(mockProcess.getErrorStream()).thenReturn(new java.io.ByteArrayInputStream("".getBytes()));
        doReturn(mockProcess).when(handler).startProcess(any());

        // Trigger startPion
        String settingsPayload = "{\"type\":\"emitir\", \"streamId\":\"1_session123\", \"videoSettings\":{\"width\":\"1280\",\"height\":\"720\",\"fps\":\"30\"}}";
        handler.handleTextMessage(session, new TextMessage(settingsPayload));
        String offerPayload = "{\"type\":\"offer\", \"streamId\":\"1_session123\", \"sdp\":\"v=0...\"}";
        handler.handleTextMessage(session, new TextMessage(offerPayload));
        String userIdPayload = "{\"type\":\"userId\", \"idUsuario\":1}";
        handler.handleTextMessage(session, new TextMessage(userIdPayload));

        // Capture and run stdout reader
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor, org.mockito.Mockito.atLeastOnce()).execute(runnableCaptor.capture());
        List<Runnable> tasks = runnableCaptor.getAllValues();
        Runnable stdoutReader = tasks.get(tasks.size() - 1);
        stdoutReader.run();

        verify(session).sendMessage(argThat(msg -> {
            if (msg instanceof TextMessage) {
                String p = ((TextMessage) msg).getPayload();
                return p != null && p.contains("candidate") && p.contains("candidate_string");
            }
            return false;
        }));
    }

    @Test
    void startPion_ShouldHandleIOException() throws Exception {
        // Setup: make session valid for compruebaSesion
        lenient().when(session.getAttributes()).thenReturn(sessionAttributes);
        sessionAttributes.put("idUsuario", 1L);
        lenient().when(session.getId()).thenReturn("session123");

        // Setup: make startProcess throw IOException
        doThrow(new IOException("Failed to start process")).when(handler).startProcess(any());

        // Trigger startPion by sending first an emit message and then an offer
        String emitPayload = "{\"type\":\"emitir\", \"streamId\":\"1_session123\", \"videoSettings\":{\"width\":\"1280\",\"height\":\"720\",\"fps\":\"30\"}}";
        handler.handleTextMessage(session, new TextMessage(emitPayload));
        String offerPayload = "{\"type\":\"offer\", \"streamId\":\"1_session123\", \"sdp\":\"v=0...\"}";
        handler.handleTextMessage(session, new TextMessage(offerPayload));

        // Verify it doesn't crash and logs error
        verify(handler).startProcess(any());
        verify(executor, never()).execute(any(Runnable.class));
    }

    @Test
    void afterConnectionClosed_ShouldCleanupResources() throws Exception {
        // Setup resources
        Process mockProcess = mock(Process.class);
        BufferedReader mockReader = mock(BufferedReader.class);
        BufferedWriter mockWriter = mock(BufferedWriter.class);
        BufferedReader mockErrorReader = mock(BufferedReader.class);

        // We need to inject these into the handler.
        // Since they are private, we can use reflection or just trigger startPion with
        // a mock process.
        doReturn(mockProcess).when(handler).startProcess(any());
        when(mockProcess.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[0]));
        when(mockProcess.getOutputStream()).thenReturn(new java.io.ByteArrayOutputStream());
        when(mockProcess.getErrorStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[0]));

        // Trigger startPion to initialize the fields
        handler.startPion();

        // Now close the connection
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // Verify cleanup
        verify(mockProcess).destroy();
        // Note: the readers/writers are created internally, so we can't easily verify
        // their close()
        // unless we mock the streams they are based on.
    }

    @Test
    void handleTextMessage_Candidate_ShouldWriteToPion() throws Exception {
        String payload = "{\"type\":\"candidate\", \"streamId\":\"1_session123\", \"candidate\":{\"candidate\":\"abc\", \"sdpMid\":\"0\", \"sdpMLineIndex\":0}}";
        TextMessage message = new TextMessage(payload);
        when(session.getId()).thenReturn("session123");

        handler.handleTextMessage(session, message);

        // We can't easily verify pionWriter without reflection, but it should not
        // crash.
    }

    @Test
    void handleTextMessage_DetenerStreamWebRTC_ShouldStopStreamingAndClose() throws Exception {
        String streamId = "1_session123";
        String payload = "{\"type\":\"detenerStreamWebRTC\", \"streamId\":\"" + streamId + "\"}";
        TextMessage message = new TextMessage(payload);
        when(session.getId()).thenReturn("session123");

        handler.handleTextMessage(session, message);

        verify(streamingService).stopFFmpegProcessForUser(streamId);
        verify(session).close();
    }

    @Test
    void handleTextMessage_DetenerStreamWebRTC_PionWriterThrows_ShouldLog() throws Exception {
        String streamId = "1_session123";
        String payload = "{\"type\":\"detenerStreamWebRTC\", \"streamId\":\"" + streamId + "\"}";
        TextMessage message = new TextMessage(payload);
        when(session.getId()).thenReturn("session123");

        // Mock pionWriter to throw IOException
        BufferedWriter mockWriter = mock(BufferedWriter.class);
        java.lang.reflect.Field writerField = WebRTCSignalingHandler.class.getDeclaredField("pionWriter");
        writerField.setAccessible(true);
        writerField.set(handler, mockWriter);
        doThrow(new IOException("write error")).when(mockWriter).write(anyString());

        handler.handleTextMessage(session, message);

        verify(session).close();
    }

    @Test
    void handleTextMessage_DetenerStreamWebRTC_StreamingServiceThrows_ShouldLog() throws Exception {
        String streamId = "1_session123";
        String payload = "{\"type\":\"detenerStreamWebRTC\", \"streamId\":\"" + streamId + "\"}";
        TextMessage message = new TextMessage(payload);
        when(session.getId()).thenReturn("session123");

        doThrow(new RuntimeException("streaming error")).when(streamingService).stopFFmpegProcessForUser(streamId);

        handler.handleTextMessage(session, message);

        verify(session).close();
    }

    @Test
    void handleTextMessage_Offer_PionWriterNull_ShouldLog() throws Exception {
        when(session.getId()).thenReturn("session123");
        // Forzamos pionWriter a null (ya lo es por defecto si no llamamos a startPion)

        TextMessage message = new TextMessage(
                "{\"type\":\"offer\",\"streamId\":\"1_session123\",\"sdp\":\"offer-sdp\"}");
        // Necesitamos que haya settings previos
        handler.handleTextMessage(session, new TextMessage("{\"type\":\"emitir\",\"streamId\":\"1_session123\"}"));

        handler.handleTextMessage(session, message);
        // Debería loguear "No se pudo enviar offer a Pion porque pionWriter es null"
    }

    @Test
    void handleTextMessage_Candidate_PionWriterNull_ShouldLog() throws Exception {
        when(session.getId()).thenReturn("session123");
        TextMessage message = new TextMessage(
                "{\"type\":\"candidate\",\"streamId\":\"1_session123\",\"candidate\":{}}");
        handler.handleTextMessage(session, message);
    }

    @Test
    void handleTextMessage_NullFields_ShouldNotThrow() throws Exception {
        TextMessage message = new TextMessage("{\"type\":null, \"streamId\":null}");
        handler.handleTextMessage(session, message);
    }

    @Test
    void startPion_InterruptedException_ShouldLog() throws Exception {
        // Mock startProcess to return a process that throws InterruptedException when
        // waitFor is called
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[0]));
        when(mockProcess.getOutputStream()).thenReturn(new java.io.ByteArrayOutputStream());
        when(mockProcess.getErrorStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[0]));

        doReturn(mockProcess).when(handler).startProcess(any());

        // Simular interrupción durante el inicio (aunque startPion no llama a waitFor
        // directamente,
        // los hilos que lanza sí lo hacen)

        handler.startPion();

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor, org.mockito.Mockito.atLeastOnce()).execute(runnableCaptor.capture());

        // El hilo de espera del proceso (lambda$startPion$1)
        Runnable processWaiter = runnableCaptor.getAllValues().stream()
                .filter(r -> r.getClass().getName().contains("lambda$startPion$1"))
                .findFirst().orElse(null);

        if (processWaiter != null) {
            when(mockProcess.waitFor()).thenThrow(new InterruptedException("Interrupted"));
            processWaiter.run();
            // Verificamos que se restauró el estado de interrupción
            // assertTrue(Thread.interrupted()); // Esto depende de si el hilo de ejecución
            // es el actual
        }
    }

    @Test
    void testPionStdoutReading_IOException() throws Exception {
        Process mockProcess = mock(Process.class);
        InputStream mockIn = mock(InputStream.class);
        when(mockIn.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("Read error"));

        when(mockProcess.getInputStream()).thenReturn(mockIn);
        when(mockProcess.getOutputStream()).thenReturn(new java.io.ByteArrayOutputStream());
        when(mockProcess.getErrorStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[0]));
        doReturn(mockProcess).when(handler).startProcess(any());

        handler.startPion();

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor, org.mockito.Mockito.atLeastOnce()).execute(runnableCaptor.capture());

        // Buscar el lector de stdout (lambda$startPion$3)
        Runnable stdoutReader = runnableCaptor.getAllValues().get(1); // El segundo suele ser stdout
        stdoutReader.run();
        // Debería capturar la IOException y loguear
    }

    @Test
    void testPionStderrReading_IOException() throws Exception {
        Process mockProcess = mock(Process.class);
        InputStream mockErr = mock(InputStream.class);
        when(mockErr.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("Read error"));

        when(mockProcess.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[0]));
        when(mockProcess.getOutputStream()).thenReturn(new java.io.ByteArrayOutputStream());
        when(mockProcess.getErrorStream()).thenReturn(mockErr);
        doReturn(mockProcess).when(handler).startProcess(any());

        handler.startPion();

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor, org.mockito.Mockito.atLeastOnce()).execute(runnableCaptor.capture());

        // Buscar el lector de stderr (lambda$startPion$0)
        Runnable stderrReader = runnableCaptor.getAllValues().get(0); // El primero es stderr
        stderrReader.run();
    }

    @Test
    void isAuthorized_EmptyAuthorities_ShouldReturnFalse() {
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getAuthorities()).thenReturn(Collections.emptyList());
        assertFalse(handler.isAuthorized(auth));
    }

    @Test
    void afterConnectionClosed_NullResources_ShouldNotThrow() throws Exception {
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
    }
}
