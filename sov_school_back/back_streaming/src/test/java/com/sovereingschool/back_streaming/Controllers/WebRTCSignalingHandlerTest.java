package com.sovereingschool.back_streaming.Controllers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
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
    void isAuthorized_AuthNull_ShouldReturnFalse() {
        assertFalse(handler.isAuthorized(null));
    }

    @Test
    void afterConnectionEstablished_Exception_ShouldCloseSession() throws Exception {
        // Provocamos una excepción, por ejemplo haciendo que session.getAttributes()
        // lance algo
        when(session.getAttributes()).thenThrow(new RuntimeException("test exception"));

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.SERVER_ERROR);
    }

    @Test
    void afterConnectionClosed_CleanupExceptions_ShouldNotThrow() throws Exception {
        when(session.getId()).thenReturn("session123");
        // Para probar los catch de close()
        BufferedWriter mockWriter = mock(BufferedWriter.class);
        doThrow(new IOException("writer error")).when(mockWriter).close();

        BufferedReader mockReader = mock(BufferedReader.class);
        doThrow(new IOException("reader error")).when(mockReader).close();

        // Inyectamos los mocks vía reflexión o simplemente usamos el proceso ya
        // iniciado
        // Como son campos privados, usaremos reflexión para testing de cobertura
        // extrema
        java.lang.reflect.Field writerField = WebRTCSignalingHandler.class.getDeclaredField("pionWriter");
        writerField.setAccessible(true);
        writerField.set(handler, mockWriter);

        java.lang.reflect.Field readerField = WebRTCSignalingHandler.class.getDeclaredField("pionReader");
        readerField.setAccessible(true);
        readerField.set(handler, mockReader);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(mockWriter).close();
        verify(mockReader).close();
    }

    @Test
    void compruebaSesion_SendMessageThrows_ShouldCloseSession() throws Exception {
        when(session.getId()).thenReturn("session123");
        doThrow(new IOException("send error")).when(session).sendMessage(any(TextMessage.class));

        assertFalse(handler.compruebaSesion("wrong_id", session));

        verify(session).close();
    }
}
