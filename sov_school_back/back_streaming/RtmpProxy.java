import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap; // Mapa de sesiones (sessionId → puerto ffmpeg)

import javax.naming.AuthenticationException;

public class RtmpProxy {

    public static void main(String[] args) throws IOException {
        // Puerto que expone Java para que OBS se conecte:
        final int LOCAL_PORT = 8060;
        // Puerto al que está escuchando FFmpeg (servidor RTMP local)
        final String FFMPEG_HOST = "127.0.0.1";
        // Mapa de sesiones (sessionId → puerto ffmpeg)
        Map<String, Integer> sessionIdToPort = new ConcurrentHashMap<>();
        Map<Integer, Thread> portToFFmpegThread = new ConcurrentHashMap<>();

        ServerSocket serverSocket = new ServerSocket(LOCAL_PORT);
        System.out.println("Java RTMP proxy listening on port " + LOCAL_PORT);

        while (true) {
            // 1) Esperamos a que OBS se conecte a 127.0.0.1:8060
            Socket socketObs = serverSocket.accept();
            InputStream inObs = socketObs.getInputStream();
            OutputStream outObs = socketObs.getOutputStream();
            System.out.println("OBS connected from " + socketObs.getRemoteSocketAddress());

            // 2) Leer C0 (1 byte) + C1 (1536 bytes)
            byte[] c0 = new byte[1];
            int read = 0;
            while (read < c0.length) {
                int r = inObs.read(c0, read, c0.length - read);
                if (r < 0)
                    throw new IOException("EOF antes de C0");
                read += r;
            }
            byte[] c1 = new byte[1536];
            read = 0;
            while (read < c1.length) {
                int r = inObs.read(c1, read, c1.length - read);
                if (r < 0)
                    throw new IOException("EOF antes de C1");
                read += r;
            }

            // 3) Reenviamos c0 y c1 como S0 y S1 a OBS
            byte s0 = c0[0]; // normalmente 0x03
            byte[] s1 = Arrays.copyOf(c1, 1536); // espejo exacto de C1

            // 4) Enviamos S0 y S1 al cliente
            outObs.write(s0);
            outObs.write(s1);
            outObs.flush();

            // 5) Recibimos el c2 (1536 bytes) del cliente
            byte[] c2 = new byte[1536];
            read = 0;
            while (read < c2.length) {
                int r = inObs.read(c2, read, c2.length - read);
                if (r < 0)
                    throw new IOException("EOF antes de C2");
                read += r;
            }

            // 6) Leemos el chunk header de 12 bytes:
            byte[] chunkHeader = new byte[12];
            read = 0;
            while (read < 12) {
                int n = inObs.read(chunkHeader, read, 12 - read);
                if (n < 0) {
                    throw new IOException("EOF antes de leer todo el chunk header");
                }
                read += n;
            }

            // 7) Extraemos el mensaje length (3 bytes big-endian) de chunkHeader[4..6]:
            int msgLen = ((chunkHeader[4] & 0xFF) << 16) |
                    ((chunkHeader[5] & 0xFF) << 8) |
                    ((chunkHeader[6] & 0xFF) << 0);

            // 8) leemos exactamente msgLen bytes, que contienen el objeto AMF0
            byte[] amfPayload = new byte[msgLen];
            read = 0;
            while (read < msgLen) {
                int n = inObs.read(amfPayload, read, msgLen - read);
                if (n < 0) {
                    throw new IOException("EOF antes de leer todo el payload AMF0");
                }
                read += n;
            }

            // 9) leemos el stream key
            String streamKey = parseStreamKeyFromAmf0(amfPayload);

            // 10) Comprobamos que el streamKey sea correcto
            String sessionId = streamKey.split("_")[1];
            System.out.println("Session ID: " + sessionId);
            Integer port = sessionIdToPort.get(sessionId);
            if (port == null) {
                throw new AuthenticationException("Invalid session ID");
            }

            // 11) Creamo el socket a FFmpeg
            Socket socketFfmpeg = new Socket(FFMPEG_HOST, port);
            System.out.println("Connected to FFmpeg RTMP server at " +
                    FFMPEG_HOST + ":" + port);
            InputStream inFfmpeg = socketFfmpeg.getInputStream();
            OutputStream outFfmpeg = socketFfmpeg.getOutputStream();

            // 12) Enviamos el c0, c1, c2, chunk header y payload a FFmpeg
            outFfmpeg.write(c0);
            outFfmpeg.write(c1);
            outFfmpeg.flush();

            // 13) Leemos en s0 y s1 de FFmpeg y los descartamos
            byte[] fs0 = new byte[1];
            inFfmpeg.read(fs0);

            byte[] fs1 = new byte[1536];
            inFfmpeg.read(fs1);

            // 14) Enviamos el c2, chunk header y payload a FFmpeg
            outFfmpeg.write(c2);
            outFfmpeg.write(chunkHeader);
            outFfmpeg.write(amfPayload);
            outFfmpeg.flush();

            // 15) Leemos en s2 de FFmpeg y lo enviamos a OBS
            byte[] fs2 = new byte[1536];
            inFfmpeg.read(fs2);
            outObs.write(fs2);
            outObs.flush();

            // 16) Iniciamos un hilo que copia datos de FFmpeg → OBS
            Thread tFfmpeg = new Thread(() -> {
                forwardData(inFfmpeg, outObs);
            }, "FFMPEG→OBS-" + port);

            // 17) Iniciamos un hilo que copia datos de OBS → FFmpeg
            Thread tObs = new Thread(() -> {
                forwardData(inObs, outFfmpeg);
            }, "OBS→FFMPEG-" + port);

            tFfmpeg.start();
            tObs.start();
        }
    }

    private static void forwardData(InputStream in, OutputStream out) {
        try {
            byte[] buffer = new byte[8 * 1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                out.flush();
            }
        } catch (IOException e) {
            // Puede cerrarse el cliente o FFmpeg; no le hacemos crash a la JVM:
            // cerramos ambos sockets y finalizamos el hilo
        } finally {
            try {
                in.close();
            } catch (Exception ex) {
            }
            try {
                out.close();
            } catch (Exception ex) {
            }
        }
    }
}
