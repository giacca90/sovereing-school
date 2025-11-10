package com.sovereingschool.back_streaming.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class GPUDetector {

    public enum VideoAcceleration {
        VAAPI,
        NVIDIA,
        CPU
    }

    public static VideoAcceleration detectAcceleration() {
        if (isVaapiAvailable()) {
            return VideoAcceleration.VAAPI;
        }
        if (isNvidiaAvailable()) {
            return VideoAcceleration.NVIDIA;
        }
        return VideoAcceleration.CPU;
    }

    private static boolean isVaapiAvailable() {
        try {
            Process process = new ProcessBuilder("bash", "-c", "ls /dev/dri/render* 2>/dev/null").start();
            return readProcessOutput(process).contains("renderD");
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isNvidiaAvailable() {
        try {
            Process process = new ProcessBuilder("bash", "-c", "nvidia-smi -L").start();
            return readProcessOutput(process).contains("GPU");
        } catch (IOException e) {
            return false;
        }
    }

    private static String readProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            return result.toString();
        }
    }
}