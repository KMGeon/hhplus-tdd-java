package io.hhplus.tdd.controller;

import io.hhplus.tdd.ControllerTest;
import io.hhplus.tdd.controller.api.PointMockApiCaller;
import io.hhplus.tdd.domain.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        UserPoint point = pointMockApiCaller.point(
                USER_ID,
                HttpStatus.OK.value()
        );

        // then
        assertNotNull(point);
        assertEquals(USER_ID, point.id());
    }


}
