package io.hhplus.tdd.controller;

import io.hhplus.tdd.ControllerTest;
import io.hhplus.tdd.controller.api.PointMockApiCaller;
import io.hhplus.tdd.domain.PointHistory;
import io.hhplus.tdd.domain.TransactionType;
import io.hhplus.tdd.domain.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


public class PointControllerTest extends ControllerTest {

    private PointMockApiCaller pointMockApiCaller;

    @BeforeEach
    void setUp() {
        pointMockApiCaller = new PointMockApiCaller(mockMvc, objectMapper);
    }

    @Test
    @DisplayName("사용자 ID로 포인트 정보를 조회할 수 있다")
    public void getPoint_shouldReturnUserPoint() throws Exception {
        // given
        UserPoint emptyPoint = UserPoint.empty(USER_ID);
        when(pointService.getUserPointWithDefault(USER_ID)).thenReturn(emptyPoint);

        // when
        UserPoint rtn = pointMockApiCaller.point(
                USER_ID,
                EXPECT_STATUS_OK
        );

        // then
        assertAll(
                () -> assertNotNull(rtn),
                () -> assertEquals(USER_ID, rtn.id()),
                () -> assertEquals(rtn.point(), 0L),
                () -> assertThat(rtn.updateMillis()).isNotNull()
        );
    }

    @Test
    @DisplayName("사용자 ID로 포인트 기록을 조회할 수 있다.")
    public void getPointHistory_shouldReturnPointHistoryList() throws Exception {
        // given
        List<PointHistory> response = List.of(
                new PointHistory(3L, 1L, 30000L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, 1L, -200L, TransactionType.USE, System.currentTimeMillis()),
                new PointHistory(1L, 1L, 1000L, TransactionType.CHARGE, System.currentTimeMillis())
        );
        when(pointService.history(eq(USER_ID))).thenReturn(response);

        // when
        List<PointHistory> history = pointMockApiCaller.history(
                USER_ID,
                EXPECT_STATUS_OK
        );

        // then
        assertAll(
                () -> assertEquals(3, history.size()),
                () -> assertEquals(response.get(0).id(), history.get(0).id()),
                () -> assertEquals(response.get(0).amount(), history.get(0).amount()),
                () -> assertEquals(response.get(0).type(), history.get(0).type()),
                () -> assertEquals(response.get(1).id(), history.get(1).id()),
                () -> assertEquals(response.get(1).amount(), history.get(1).amount()),
                () -> assertEquals(response.get(1).type(), history.get(1).type()),
                () -> assertEquals(response.get(2).id(), history.get(2).id()),
                () -> assertEquals(response.get(2).amount(), history.get(2).amount()),
                () -> assertEquals(response.get(2).type(), history.get(2).type()),
                () -> verify(pointService, times(1)).history(eq(USER_ID))
        );
    }


    @Test
    @DisplayName("[PATCH] : /point/{id} - pointService.getUserPoint")
    public void charge_shouldReturnUserPoint_isValid() throws Exception {
        // given
        final long chargeAmount = 1000L;
        UserPoint expectUserPoint = fixtureUser(chargeAmount);

        when(pointService.charge(eq(1L), eq(chargeAmount), anyLong())).thenReturn(expectUserPoint);

        // when
        UserPoint charge = pointMockApiCaller.charge(1L, chargeAmount, 200);

        // then
        assertEquals(charge.id(), 1L, "");
        assertEquals(charge.point(), chargeAmount, "");
    }

    @Test
    public void use_shouldReturnUserPoint_isValid() throws Exception {
        // given
        final long useAmount = 1000L;
        UserPoint expectUser = fixtureUser(useAmount);

        when(pointService.use(eq(USER_ID), eq(1000L), anyLong()))
                .thenReturn(expectUser);

        // when
        UserPoint rtn = pointMockApiCaller.use(USER_ID, 1000L, EXPECT_STATUS_OK);

        // then
        assertEquals(rtn, expectUser);
        verify(pointService, times(1)).use(eq(USER_ID), eq(1000L), anyLong());
    }
}
