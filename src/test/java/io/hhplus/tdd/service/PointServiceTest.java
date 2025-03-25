package io.hhplus.tdd.service;

import io.hhplus.tdd.domain.UserPoint;
import io.hhplus.tdd.repository.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;

    @InjectMocks
    private PointService pointService;

    private UserPoint existingUserPoint;
    private UserPoint newUserPoint;

    private final long EXISTING_USER_ID = 1L;
    private final long NEW_USER_ID = 2L;
    private final long DEFAULT_CURRENT_TIME = 1111111L;
    private long systemCurrentTime;

    @BeforeEach
    void setUp() {
        systemCurrentTime = System.currentTimeMillis();
        existingUserPoint = new UserPoint(EXISTING_USER_ID, 1000L, DEFAULT_CURRENT_TIME);
        newUserPoint = UserPoint.empty(NEW_USER_ID);
    }

    @Test
    @DisplayName("기존 사용자 ID로 포인트 정보를 조회할 수 있다")
    void getUserPointWithDefault_shouldReturnUserPoint_whenUserExists() {
        // given
        when(userPointTable.selectById(eq(EXISTING_USER_ID))).thenReturn(existingUserPoint);

        // when
        UserPoint result = pointService.getUserPointWithDefault(EXISTING_USER_ID);

        // then
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(EXISTING_USER_ID, result.id()),
                () -> assertEquals(1000L, result.point()),
                () -> assertEquals(DEFAULT_CURRENT_TIME, result.updateMillis())
        );

        verify(userPointTable, times(1)).selectById(EXISTING_USER_ID);
    }

    @Test
    @DisplayName("신규 사용자 ID로 포인트 정보를 조회하면 기본값이 반환된다")
    void getUserPointWithDefault_shouldReturnDefaultUserPoint_whenNewUser() {
        // given
        when(userPointTable.selectById(eq(NEW_USER_ID))).thenReturn(newUserPoint);

        // when
        UserPoint result = pointService.getUserPointWithDefault(NEW_USER_ID);

        // then
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(NEW_USER_ID, result.id()),
                () -> assertEquals(0L, result.point()),
                () -> assertEquals(systemCurrentTime, result.updateMillis())
        );

        verify(userPointTable, times(1)).selectById(NEW_USER_ID);
    }
}