package com.sovereingschool.back_streaming.Models;

import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "presets")
public class Preset {
    @Data
    public static class PresetValue {
        private List<VideoElement> elements;
        private String shortcut;
    }

    @Data
    public static class VideoElement {
        private String id;
        private Object element;
        private boolean painted;
        private double scale;
        private Position position;
    }

    @Data
    public static class Position {
        private int x;
        private int y;
    }

    @Id
    private String id;
    private Long idUsuario;
    private Map<String, PresetValue> presets;

    public Preset(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Preset(Long idUsuario, Map<String, PresetValue> presets) {
        this.idUsuario = idUsuario;
        this.presets = presets;
    }
}
