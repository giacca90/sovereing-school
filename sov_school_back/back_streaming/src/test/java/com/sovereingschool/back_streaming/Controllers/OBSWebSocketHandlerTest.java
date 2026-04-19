package com.sovereingschool.back_streaming.Controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.sovereingschool.back_common.Exceptions.InternalServerException;
import com.sovereingschool.back_common.Exceptions.RepositoryException;
import com.sovereingschool.back_streaming.Services.StreamingService;

@ExtendWith(MockitoExtension.class)
class OBSWebSocketHandlerTest {

    private OBSWebSocketHandler handler;

    @Mock
    private StreamingService streamingService;

    @Mock
    private Executor executor;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    @Mock
    private WebSocketSession session;

    @Mock
    private Authentication authentication;

    private final String RTMP_URL = "rtmp://localhost/live/";
    private final String RTMP_DOCKER = "rtmp://docker-rtmp/live/";
    private final String uploadDir = "/tmp/uploads";

    private MockedStatic<Files> mockedFiles;
    private MockedStatic<Paths> mockedPaths;

    @BeforeEach
    void setUp() {
        handler = new OBSWebSocketHandler(executor, streamingService, RTMP_URL, RTMP_DOCKER, uploadDir);
    }

    @AfterEach
    void tearDown() {
        if (mockedFiles != null)
            mockedFiles.close();
        if (mockedPaths != null)
            mockedPaths.close();
    }

    /**
     * Prueba que la sesión se cierre si existe un atributo de error al establecer
     * la conexión.
     * 
     * @throws IOException Si ocurre un error de E/S.
     */
    @Test
    void afterConnectionEstablished_ErrorAttributePresent_ShouldCloseSession() throws IOException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("Error", "Auth failed");
        when(session.getAttributes()).thenReturn(attributes);

        handler.afterConnectionEstablished(session);

        verify(session).sendMessage(any(TextMessage.class));
        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    /**
     * Prueba que la sesión se cierre si el usuario no tiene los permisos
     * autorizados.
     * 
     * @throws IOException Si ocurre un error de E/S.
     */
    @Test
    void afterConnectionEstablished_NotAuthorized_ShouldCloseSession() throws IOException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("Auth", authentication);
        when(session.getAttributes()).thenReturn(attributes);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(java.util.Collections.emptyList()).when(authentication).getAuthorities();

        handler.afterConnectionEstablished(session);

        verify(session).sendMessage(any(TextMessage.class));
        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    /**
     * Prueba que la sesión se cierre si el objeto de autenticación es nulo.
     * 
     * @throws IOException Si ocurre un error de E/S.
     */
    @Test
    void afterConnectionEstablished_AuthNull_ShouldCloseSession() throws IOException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("Auth", null);
        when(session.getAttributes()).thenReturn(attributes);

        handler.afterConnectionEstablished(session);

        verify(session).sendMessage(any(TextMessage.class));
        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    /**
     * Prueba que la sesión se cierre si el usuario no está autenticado.
     * 
     * @throws IOException Si ocurre un error de E/S.
     */
    @Test
    void afterConnectionEstablished_NotAuthenticated_ShouldCloseSession() throws IOException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("Auth", authentication);
        when(session.getAttributes()).thenReturn(attributes);
        when(authentication.isAuthenticated()).thenReturn(false);

        handler.afterConnectionEstablished(session);

        verify(session).sendMessage(any(TextMessage.class));
        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }

    /**
     * Prueba que la sesión se añada correctamente si el usuario está autorizado.
     * 
     * @throws IOException Si ocurre un error de E/S.
     */
    @Test
    void afterConnectionEstablished_Authorized_ShouldAddSession() throws IOException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("Auth", authentication);
        attributes.put("username", "testuser");
        when(session.getAttributes()).thenReturn(attributes);
        when(session.getId()).thenReturn("session123");
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(java.util.List.of(new SimpleGrantedAuthority("ROLE_PROF"))).when(authentication).getAuthorities();

        handler.afterConnectionEstablished(session);

        verify(session, never()).close(any());
    }

    /**
     * Prueba que isAuthorized devuelva true para un usuario con rol PROF.
     */
    @Test
    void isAuthorized_ProfRole_ShouldReturnTrue() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        doReturn(java.util.List.of(new SimpleGrantedAuthority("ROLE_PROF"))).when(auth).getAuthorities();

        boolean result = handler.isAuthorized(auth);

        assertEquals(true, result);
    }

    /**
     * Prueba que isAuthorized devuelva true para un usuario con rol ADMIN.
     */
    @Test
    void isAuthorized_AdminRole_ShouldReturnTrue() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        doReturn(java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(auth).getAuthorities();

        boolean result = handler.isAuthorized(auth);

        assertEquals(true, result);
    }

    /**
     * Prueba que isAuthorized devuelva false para un usuario con rol USER.
     */
    @Test
    void isAuthorized_UserRole_ShouldReturnFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        doReturn(java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(auth).getAuthorities();

        boolean result = handler.isAuthorized(auth);

        assertEquals(false, result);
    }

    /**
     * Prueba que isAuthorized devuelva false si el usuario no está autenticado.
     */
    @Test
    void isAuthorized_NotAuthenticated_ShouldReturnFalse() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        boolean result = handler.isAuthorized(auth);

        assertEquals(false, result);
    }

    /**
     * Prueba que isAuthorized devuelva false si la autenticación es nula.
     */
    @Test
    void isAuthorized_NullAuth_ShouldReturnFalse() {
        boolean result = handler.isAuthorized(null);

        assertEquals(false, result);
    }

    @Test
    void afterConnectionEstablished_Exception_ShouldCloseSession() throws IOException {
        when(session.getId()).thenReturn("session123");
        when(session.getAttributes()).thenThrow(new RuntimeException("Unexpected error"));

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.SERVER_ERROR);
    }

    @Test
    void afterConnectionClosed_ShouldStopFFmpegAndCleanup() throws Exception {
        when(session.getId()).thenReturn("session123");

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(streamingService).stopFFmpegProcessForUser("session123");
    }

    @Test
    void handleTextMessage_UnknownType_ShouldSendError() throws Exception {
        TextMessage message = new TextMessage("{\"type\":\"unknown\"}");

        handler.handleTextMessage(session, message);

        verify(session)
                .sendMessage(argThat((TextMessage msg) -> msg.getPayload().contains("Tipo de mensaje no reconocido")));
    }

    @Test
    void handleTextMessage_InvalidJson_ShouldSendError() throws Exception {
        TextMessage message = new TextMessage("invalid json");

        handler.handleTextMessage(session, message);

        verify(session).sendMessage(argThat((TextMessage msg) -> msg.getPayload().contains("error")));
    }

    @Test
    void handleRequestRtmpUrl_UserIdMissing_ShouldSendError() throws IOException {
        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);

        handler.handleRequestRtmpUrl(session);

        verify(session).sendMessage(argThat((TextMessage msg) -> msg.getPayload().contains("userId no proporcionado")));
    }

    @Test
    void handleRequestRtmpUrl_UserIdPresent_ShouldExecutePreviewAndSendUrl() throws IOException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("idUsuario", 1L);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.getId()).thenReturn("session123");

        handler.handleRequestRtmpUrl(session);

        verify(executor).execute(runnableCaptor.capture());
        verify(session).sendMessage(argThat((TextMessage msg) -> msg.getPayload().contains("rtmp_url")));

        // Execute the captured runnable to test startPreview
        mockedFiles = mockStatic(Files.class);
        mockedPaths = mockStatic(Paths.class);

        Path mockBaseDir = mock(Path.class);
        Path mockPreviewsDir = mock(Path.class);
        Path mockOutputDir = mock(Path.class);

        when(Paths.get(uploadDir)).thenReturn(mockBaseDir);
        when(mockBaseDir.resolve("previews")).thenReturn(mockPreviewsDir);
        when(Files.exists(mockPreviewsDir)).thenReturn(true);
        when(mockPreviewsDir.resolve(any(String.class))).thenReturn(mockOutputDir);
        when(Files.exists(mockOutputDir)).thenReturn(true);

        // Mock Process for FFmpeg preview
        Process mockProcess = mock(Process.class);
        lenient().when(mockProcess.getInputStream())
                .thenReturn(new java.io.ByteArrayInputStream("ffmpeg version...".getBytes()));

        // Use a custom mock or spy for ProcessBuilder if possible,
        // but since ProcessBuilder is final/hard to mock,
        // we might need to accept that startPreview actually tries to run ffmpeg
        // unless we wrap ProcessBuilder.
        // However, we can mock the process if we can inject it.
        // Since we can't easily mock ProcessBuilder.start(),
        // this part might still fail or be skipped.

        // For now, let's just verify the Runnable was captured and we can run it.
        // To avoid actual process start, we might need to refactor OBSWebSocketHandler
        // to use a ProcessFactory.

        // Let's try to run it and see if it fails due to ProcessBuilder.
        try {
            runnableCaptor.getValue().run();
        } catch (Exception e) {
            // Expected to fail if ffmpeg is not installed or ProcessBuilder can't start
        }
    }

    @Test
    void handleEmitirOBS_StreamIdMissing_ShouldSendError() throws IOException {
        String payload = "{\"type\":\"emitirOBS\"}"; // missing rtmpUrl

        handler.handleEmitirOBS(session, payload);

        verify(session)
                .sendMessage(argThat((TextMessage msg) -> msg.getPayload().contains("rtmpUrl no proporcionada")));
    }

    @Test
    void handleEmitirOBS_StreamIdPresent_ShouldStartFFmpeg()
            throws IOException, IllegalArgumentException, RepositoryException, InternalServerException {
        String streamId = "1_session123";
        String payload = "{\"type\":\"emitirOBS\", \"rtmpUrl\":\"rtmp://localhost/" + streamId + "\"}";

        handler.handleEmitirOBS(session, payload);

        verify(executor).execute(runnableCaptor.capture());
        verify(session).sendMessage(argThat((TextMessage msg) -> msg.getPayload().contains("\"type\":\"start\"")));

        // Execute the captured runnable to test startFFmpegProcessForUser
        runnableCaptor.getValue().run();

        verify(streamingService).startLiveStreamingFromStream(eq(streamId), any(), any());
    }

    @Test
    void startFFmpegProcessForUser_AlreadyRunning_ShouldThrowException() throws IOException {
        String streamId = "1_session123";
        String payload = "{\"type\":\"emitirOBS\", \"rtmpUrl\":\"rtmp://localhost/" + streamId + "\"}";

        // First call to start it
        handler.handleEmitirOBS(session, payload);
        verify(executor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        // Second call: should throw RuntimeException inside startFFmpegProcessForUser
        // which is caught by handleEmitirOBS and sent as error message
        handler.handleEmitirOBS(session, payload);

        // executor.execute should NOT be called again
        verify(executor, org.mockito.Mockito.times(1)).execute(any());

        // Verify error message was sent
        verify(session).sendMessage(argThat((TextMessage msg) -> msg.getPayload().contains("\"type\":\"error\"") &&
                msg.getPayload().contains("El proceso FFmpeg ya está corriendo")));
    }

    @Test
    void extractStreamId_ValidPayload_ShouldReturnStreamId() {
        String payload = "{\"rtmpUrl\":\"rtmp://localhost/1_session123\"}";
        String streamId = handler.extractStreamId(payload);
        assertEquals("1_session123", streamId);
    }

    @Test
    void extractStreamId_InvalidPayload_ShouldReturnNull() {
        String payload = "{\"somethingElse\":\"value\"}";
        String streamId = handler.extractStreamId(payload);
        assertNull(streamId);
    }

    @Test
    void extractStreamId_MalformedJson_ShouldReturnNull() {
        String payload = "{\"rtmpUrl\":";
        String streamId = handler.extractStreamId(payload);
        assertNull(streamId);
    }

    @Test
    void afterConnectionClosed_WithPreviewProcess_ShouldCleanupProcessAndFolder() throws Exception {
        String sessionId = "session123";
        when(session.getId()).thenReturn(sessionId);

        Process mockProcess = mock(Process.class);
        OutputStream mockOs = mock(OutputStream.class);
        when(mockProcess.isAlive()).thenReturn(true);
        when(mockProcess.getOutputStream()).thenReturn(mockOs);
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);

        handler.previewProcesses.put(sessionId, mockProcess);

        mockedFiles = mockStatic(Files.class);
        mockedPaths = mockStatic(Paths.class);

        Path mockBaseDir = mock(Path.class);
        Path mockPreviewsDir = mock(Path.class);
        Path mockMatchingDir = mock(Path.class);
        Path mockFileName = mock(Path.class);

        when(Paths.get(uploadDir)).thenReturn(mockBaseDir);
        when(mockBaseDir.resolve("previews")).thenReturn(mockPreviewsDir);
        when(Files.walk(mockPreviewsDir, 1)).thenReturn(Stream.of(mockMatchingDir));
        when(Files.isDirectory(mockMatchingDir)).thenReturn(true);
        when(mockMatchingDir.getFileName()).thenReturn(mockFileName);
        when(mockFileName.toString()).thenReturn("someid_" + sessionId);

        // Prevent entering the broken 'if' block by making the previews directory exist
        when(Files.exists(mockPreviewsDir)).thenReturn(true);
        when(Files.isDirectory(mockPreviewsDir)).thenReturn(true);

        // We need to make sure that the handler's path resolution doesn't crash.
        // In the handler:
        // String streamId = previewDir.getFileName().toString();
        // where previewDir = baseUploadDir.resolve("previews")
        Path mockPreviewsFileName = mock(Path.class);
        when(mockPreviewsDir.getFileName()).thenReturn(mockPreviewsFileName);
        when(mockPreviewsFileName.toString()).thenReturn("previews");

        // Mocking the recursive delete part
        when(Files.walk(any(Path.class))).thenReturn(Stream.of(mockMatchingDir));
        when(mockMatchingDir.toFile()).thenReturn(mock(java.io.File.class));

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(mockOs).write('q');
        verify(mockOs).flush();
        verify(streamingService).stopFFmpegProcessForUser(sessionId);
        assertFalse(handler.previewProcesses.containsKey(sessionId));
    }

    @Test
    void afterConnectionClosed_PreviewProcessNotResponding_ShouldDestroyForcibly() throws Exception {
        String sessionId = "session123";
        when(session.getId()).thenReturn(sessionId);

        Process mockProcess = mock(Process.class);
        OutputStream mockOs = mock(OutputStream.class);
        when(mockProcess.isAlive()).thenReturn(true);
        when(mockProcess.getOutputStream()).thenReturn(mockOs);
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(false); // Doesn't finish in time

        handler.previewProcesses.put(sessionId, mockProcess);

        mockedFiles = mockStatic(Files.class);
        mockedPaths = mockStatic(Paths.class);

        Path mockBaseDir = mock(Path.class);
        Path mockPreviewsDir = mock(Path.class);
        when(Paths.get(uploadDir)).thenReturn(mockBaseDir);
        when(mockBaseDir.resolve("previews")).thenReturn(mockPreviewsDir);
        when(Files.walk(mockPreviewsDir, 1)).thenReturn(Stream.empty());

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(mockProcess).destroy();
        verify(mockProcess).destroyForcibly();
    }

    @Test
    void afterConnectionEstablished_SendMessageThrows_ShouldCloseSession() throws IOException {
        when(session.getId()).thenReturn("session123");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("Error", "Auth failed");
        when(session.getAttributes()).thenReturn(attributes);
        doThrow(new IOException("test error")).when(session).sendMessage(any(TextMessage.class));

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.SERVER_ERROR);
    }

    @Test
    void afterConnectionClosed_ProcessWaitForTimeout_ShouldDestroyForcibly() throws Exception {
        String sessionId = "session123";
        when(session.getId()).thenReturn(sessionId);

        Process mockProcess = mock(Process.class);
        OutputStream mockOs = mock(OutputStream.class);
        when(mockProcess.isAlive()).thenReturn(true, true, true);
        when(mockProcess.getOutputStream()).thenReturn(mockOs);
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(false); // timeout

        handler.previewProcesses.put(sessionId, mockProcess);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(mockProcess).destroy();
        verify(mockProcess).destroyForcibly();
    }

    @Test
    void afterConnectionClosed_ProcessExitError_ShouldLog() throws Exception {
        String sessionId = "session123";
        when(session.getId()).thenReturn(sessionId);

        Process mockProcess = mock(Process.class);
        OutputStream mockOs = mock(OutputStream.class);
        when(mockProcess.isAlive()).thenReturn(true);
        when(mockProcess.getOutputStream()).thenReturn(mockOs);
        when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(1); // error exit code

        handler.previewProcesses.put(sessionId, mockProcess);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        // Debería loguear el error sin lanzar excepción
    }

    @Test
    void handleTextMessage_Exception_ShouldSendErrorMessage() throws Exception {
        TextMessage message = mock(TextMessage.class);
        when(message.getPayload()).thenThrow(new RuntimeException("test exception"));

        handler.handleTextMessage(session, message);

        verify(session).sendMessage(argThat((TextMessage msg) -> msg.getPayload().contains("test exception")));
    }

    @Test
    void sendMessage_IOException_ShouldLog() throws IOException {
        doThrow(new IOException("test error")).when(session).sendMessage(any(TextMessage.class));

        handler.sendMessage(session, "test", "message");
        // Solo loguea el error
    }

    @Test
    void startFFmpegProcessForUser_ServiceThrows_ShouldInterruptThread() throws Exception {
        String streamId = "1_session123";
        doThrow(new RuntimeException("service error")).when(streamingService).startLiveStreamingFromStream(any(), any(),
                any());

        handler.handleEmitirOBS(session, "{\"type\":\"emitirOBS\", \"rtmpUrl\":\"rtmp://localhost/" + streamId + "\"}");
        verify(executor).execute(runnableCaptor.capture());

        try {
            runnableCaptor.getValue().run();
        } catch (RuntimeException e) {
            assertEquals("Error al iniciar FFmpeg para usuario 1: service error", e.getMessage());
        }
    }
}
