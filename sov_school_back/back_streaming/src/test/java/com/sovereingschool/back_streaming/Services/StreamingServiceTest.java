package com.sovereingschool.back_streaming.Services;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sovereingschool.back_common.Repositories.ClaseRepository;

@ExtendWith(MockitoExtension.class)
class StreamingServiceTest {

    // ==========================
    // Tests convertVideos()
    // ==========================
    @Nested
    class ConvertVideosTests {
    }

    // ==========================
    // Tests startLiveStreamingFromStream()
    // ==========================
    @Nested
    class StartLiveStreamingFromStreamTests {
    }

    // ==========================
    // Tests stopFFmpegProcessForUser()
    // ==========================
    @Nested
    class StopFFmpegProcessForUserTests {
    }

    // ==========================
    // Tests getPreview()
    // ==========================
    @Nested
    class GetPreviewTests {
    }

    @Mock
    private ClaseRepository claseRepo;

    @TempDir
    Path tempDir;
    private final String uploadDir = tempDir.toString();

    private StreamingService streamingService;

    @BeforeEach
    void setUp() {
        streamingService = new StreamingService(uploadDir, claseRepo);
    }

}
