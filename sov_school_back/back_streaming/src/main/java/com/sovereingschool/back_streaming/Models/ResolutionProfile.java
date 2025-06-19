package com.sovereingschool.back_streaming.Models;

public enum ResolutionProfile {
    // 8K
    // Excede límites H264 (macroblocks/frame)
    RES_8K_30(7680, 4320, 30, "65000k", "75000k", "150000k", "192k", "high", "5.1"),
    // Excede límites H264 (macroblocks/frame y macroblocks/s)
    RES_8K_60(7680, 4320, 60, "85000k", "95000k", "190000k", "192k", "high", "5.1"),
    // Excede límites H264 (macroblocks/frame y macroblocks/s)
    RES_8K_90(7680, 4320, 90, "105000k", "125000k", "240000k", "192k", "high", "5.2"),
    // Excede límites H264 (macroblocks/frame y macroblocks/s)
    RES_8K_120(7680, 4320, 120, "115000k", "135000k", "260000k", "192k", "high", "5.2"),
    // Excede límites H264 (macroblocks/frame y macroblocks/s)
    RES_8K_144(7680, 4320, 144, "125000k", "145000k", "280000k", "192k", "high", "5.2"),

    // 4K UHD
    RES_4K_30(3840, 2160, 30, "35000k", "40000k", "80000k", "160k", "high", "5.1"),
    RES_4K_60(3840, 2160, 60, "45000k", "50000k", "95000k", "160k", "high", "5.1"),
    // Excede límites H264(macroblocks/s)
    RES_4K_90(3840, 2160, 90, "55000k", "65000k", "120000k", "160k", "high", "5.2"),
    // Excede límites H264 (macroblocks/s)
    RES_4K_120(3840, 2160, 120, "60000k", "70000k", "130000k", "160k", "high", "5.2"),
    // Excede límites H264 (macroblocks/s)
    RES_4K_144(3840, 2160, 144, "65000k", "75000k", "140000k", "160k", "high", "5.2"),

    // 1440p
    RES_1440P_30(2560, 1440, 30, "18000k", "20000k", "40000k", "128k", "high", "4.1"),
    RES_1440P_60(2560, 1440, 60, "24000k", "26000k", "52000k", "128k", "high", "4.1"),
    RES_1440P_90(2560, 1440, 90, "28000k", "30000k", "60000k", "128k", "high", "4.2"),
    RES_1440P_120(2560, 1440, 120, "30000k", "32000k", "64000k", "128k", "high", "4.2"),
    RES_1440P_144(2560, 1440, 144, "32000k", "34000k", "68000k", "128k", "high", "5.0"),

    // 1080p
    RES_1080P_30(1920, 1080, 30, "10000k", "11000k", "22000k", "96k", "high", "4.1"),
    RES_1080P_60(1920, 1080, 60, "15000k", "17000k", "34000k", "96k", "high", "4.1"),
    RES_1080P_90(1920, 1080, 90, "18000k", "20000k", "40000k", "96k", "high", "4.1"),
    RES_1080P_120(1920, 1080, 120, "20000k", "22000k", "44000k", "96k", "high", "4.2"),
    RES_1080P_144(1920, 1080, 144, "22000k", "24000k", "48000k", "96k", "high", "4.2"),

    // 720p
    RES_720P_30(1280, 720, 30, "6000k", "7000k", "14000k", "64k", "high", "3.1"),
    RES_720P_60(1280, 720, 60, "9000k", "10000k", "20000k", "64k", "high", "4.1"),
    RES_720P_90(1280, 720, 90, "11000k", "12000k", "24000k", "64k", "high", "4.1"),
    RES_720P_120(1280, 720, 120, "13000k", "14000k", "28000k", "64k", "high", "4.1"),
    RES_720P_144(1280, 720, 144, "15000k", "16000k", "32000k", "64k", "high", "4.2"),

    // 480p
    RES_480P_30(854, 480, 30, "3000k", "3500k", "7000k", "48k", "main", "3.1"),
    RES_480P_60(854, 480, 60, "4500k", "5000k", "10000k", "48k", "main", "3.1"),
    RES_480P_90(854, 480, 90, "5500k", "6000k", "12000k", "48k", "main", "3.1"),
    RES_480P_120(854, 480, 120, "6500k", "7000k", "14000k", "48k", "main", "3.1"),
    RES_480P_144(854, 480, 144, "7500k", "8000k", "16000k", "48k", "main", "4.0"),

    // 360p
    RES_360P_30(640, 360, 30, "1500k", "1800k", "3600k", "48k", "main", "3.1"),
    RES_360P_60(640, 360, 60, "2250k", "2600k", "5200k", "48k", "main", "3.1"),
    RES_360P_90(640, 360, 90, "2750k", "3200k", "6400k", "48k", "main", "3.1"),
    RES_360P_120(640, 360, 120, "3250k", "3800k", "7600k", "48k", "main", "3.1"),
    RES_360P_144(640, 360, 144, "3750k", "4400k", "8800k", "48k", "main", "4.0"),

    // 320p
    RES_320P_30(480, 320, 30, "1200k", "1440k", "2880k", "48k", "constrained_baseline", "3.0"),
    RES_320P_60(480, 320, 60, "1800k", "2160k", "4320k", "48k", "constrained_baseline", "3.0"),
    RES_320P_90(480, 320, 90, "2200k", "2600k", "5200k", "48k", "constrained_baseline", "3.1"),
    RES_320P_120(480, 320, 120, "2600k", "3120k", "6240k", "48k", "constrained_baseline", "3.1"),
    RES_320P_144(480, 320, 144, "3000k", "3600k", "7200k", "48k", "constrained_baseline", "4.0");

    private final int width;
    private final int height;
    private final int fps;
    private final String bitrate;
    private final String maxrate;
    private final String bufsize;
    private final String audioBitrate;
    private final String profile;
    private final String level;
    private final double aspectRatio;

    ResolutionProfile(int width, int height, int fps,
            String bitrate, String maxrate, String bufsize,
            String audioBitrate, String profile, String level) {
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.bitrate = bitrate;
        this.maxrate = maxrate;
        this.bufsize = bufsize;
        this.audioBitrate = audioBitrate;
        this.profile = profile;
        this.level = level;
        this.aspectRatio = (double) width / height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFps() {
        return fps;
    }

    public String getBitrate() {
        return bitrate;
    }

    public String getMaxrate() {
        return maxrate;
    }

    public String getBufsize() {
        return bufsize;
    }

    public String getAudioBitrate() {
        return audioBitrate;
    }

    public String getProfile() {
        return profile;
    }

    public String getLevel() {
        return level;
    }

    public double getAspectRatio() {
        return aspectRatio;
    }

    @Override
    public String toString() {
        return String.format("%dx%d@%dfps (br=%s, mr=%s, buf=%s, audio=%s, prof=%s, lvl=%s)",
                width, height, fps, bitrate, maxrate, bufsize, audioBitrate, profile, level);
    }
}
