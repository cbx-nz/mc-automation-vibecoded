package com.cbx.vibeauto.blockentity;

public record SerialMessage(String text, String level, long timestamp) {
    public static SerialMessage info(String msg) {
        return new SerialMessage(msg, "INFO", System.currentTimeMillis());
    }

    public static SerialMessage warning(String msg) {
        return new SerialMessage(msg, "WARN", System.currentTimeMillis());
    }

    public static SerialMessage error(String msg) {
        return new SerialMessage(msg, "ERROR", System.currentTimeMillis());
    }
}
