package com.sovereingschool.back_streaming.Models;

import lombok.Data;

@Data
public class VideoElement {
    @Data
    public static class Position {
        private double x;
        private double y;
    }

    private String id;
    private Object element;
    private boolean painted;
    private double scale;
    private Position position;
}
