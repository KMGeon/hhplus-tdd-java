package io.hhplus.tdd.utils;


import io.hhplus.tdd.domain.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TimeProviderTest {

    private TimeProvider timeProvider;

    @BeforeEach
    void setUp() {
        timeProvider = new TimeProvider();
    }

    @Test
    void testCurrentTimeMillis() throws InterruptedException {
        long time1 = timeProvider.currentTimeMillis();

        Thread.sleep(1);

        long time2 = timeProvider.currentTimeMillis();

        assertNotEquals(time1, time2, "time1, time2는 같지 않다.");

    }

    @Test
    void testGetConsistentTimeMillis() {
        long consistentTime1 = timeProvider.getConsistentTimeMillis();
        long consistentTime2 = timeProvider.getConsistentTimeMillis();
        assertEquals(consistentTime1, consistentTime2, "같은 Provider에서 현재 시간을 가져올 때 시간은 같다.");
    }
}
