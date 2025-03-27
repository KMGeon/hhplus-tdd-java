package io.hhplus.tdd.controller.dto;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public abstract class ThreadLogger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static void log(Object obj) {
        String time = LocalTime.now().format(formatter);
        String threadName = Thread.currentThread().getName();
        String message = String.format("%s [%s] %s", time, threadName, obj);
        log.debug(message);
    }
}
