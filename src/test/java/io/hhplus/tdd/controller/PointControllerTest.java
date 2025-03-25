package io.hhplus.tdd.controller;

import io.hhplus.tdd.ControllerTest;
import io.hhplus.tdd.controller.api.PointMockApiCaller;
import io.hhplus.tdd.domain.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


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
                HttpStatus.OK.value()
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
    @DisplayName("[PATCH] : /point/{id} - pointService.getUserPoint")
    public void charge_shouldReturnUserPoint_isValid() throws Exception {
        // given
        final long chargeAmount = 1000L;
        UserPoint expectUserPoint = fixtureUser(chargeAmount);


        when(pointService.charge(eq(1L), eq(chargeAmount), anyLong())).thenReturn(expectUserPoint);
        // when
        UserPoint charge = pointMockApiCaller.charge(1L, chargeAmount, 200);


        // then
        assertEquals(charge.id(),1L,"");
        assertEquals(charge.point(),chargeAmount,"");
    }


}
