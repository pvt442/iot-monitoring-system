package com.demo.client;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** In log ra Terminal, có giờ phía trước, an toàn khi nhiều luồng cùng in. */
public final class Log {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Log() {
    }

    public static synchronized void info(String msg) {
        System.out.println("[" + LocalTime.now().format(FMT) + "] " + msg);
    }
}
