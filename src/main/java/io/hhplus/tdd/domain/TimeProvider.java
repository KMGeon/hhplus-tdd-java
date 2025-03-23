package io.hhplus.tdd.domain;


import org.springframework.stereotype.Component;

@Component
public class TimeProvider {
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    private long cachedTime;

    public long getConsistentTimeMillis() {
        if (cachedTime == 0) {
            cachedTime = System.currentTimeMillis();
        }
        return cachedTime;
    }

}
