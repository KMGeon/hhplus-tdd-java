package io.hhplus.tdd.service;

import io.hhplus.tdd.domain.PointHistory;
import io.hhplus.tdd.domain.TransactionType;
import io.hhplus.tdd.domain.UserPoint;
import io.hhplus.tdd.repository.PointHistoryTable;
import io.hhplus.tdd.repository.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserPointTable userPointTable;
    @Mock
    private PointHistoryTable pointHistoryTable;

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

    @Nested
    @DisplayName("[ GET ]/point/{id} - pointService.getUserPointWithDefault")
    class getUserPointWithDefault {


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

    @Nested
    @DisplayName("[GET] : {id}/histories - pointService.history")
     class history{

        @Test
        @DisplayName("기존 유저가 있을 시 List<History> 반환")
        public void history_shouldReturnUser_existUser() throws Exception{
            // given
            List<PointHistory> expectHistory = List.of(new PointHistory(1L, 1L, 1000L, TransactionType.CHARGE, 1234L));

            when(userPointTable.selectById(eq(EXISTING_USER_ID))).thenReturn(existingUserPoint);
            when(pointHistoryTable.selectAllByUserId(eq(EXISTING_USER_ID))).thenReturn(expectHistory);
            // when
            List<PointHistory> rtn = pointService.history(EXISTING_USER_ID);

            // then
            assertEquals(rtn,expectHistory);
            verify(userPointTable, times(1)).selectById(EXISTING_USER_ID);
            verify(pointHistoryTable, times(1)).selectAllByUserId(EXISTING_USER_ID);
        }

        @Test
        @DisplayName("신규 유저 [] 반환")
        public void history_shouldReturnEmpty_newUser() throws Exception{
            // given
            when(userPointTable.selectById(eq(NEW_USER_ID))).thenReturn(UserPoint.empty(NEW_USER_ID));
            // when
            List<PointHistory> rtn = pointService.history(NEW_USER_ID);

            // then
            assertThat(rtn).isEmpty();
            verify(userPointTable, times(1)).selectById(NEW_USER_ID);
        }

     }

    @Nested
    @DisplayName("[PATCH] : {id}/charge - pointService.charge")
    class charge {

        @Test
        @DisplayName("포인트 충전")
        public void charge_shouldReturnUserPoint_isValid() throws Exception {
            // given
            when(userPointTable.selectById(eq(EXISTING_USER_ID))).thenReturn(new UserPoint(EXISTING_USER_ID, 1000L, DEFAULT_CURRENT_TIME));

            when(userPointTable.insertOrUpdate(eq(EXISTING_USER_ID), eq(2000L)))
                    .thenReturn(new UserPoint(EXISTING_USER_ID, 2000L, DEFAULT_CURRENT_TIME));

            when(pointHistoryTable.insert(eq(EXISTING_USER_ID), eq(1000L), eq(TransactionType.CHARGE), eq(1234L)))
                    .thenReturn(new PointHistory(1, EXISTING_USER_ID, 1000L, TransactionType.CHARGE, 1234L));

            // when
            UserPoint rtn = pointService.charge(EXISTING_USER_ID, 1000L, 1234);

            // then
            verify(userPointTable, times(1)).selectById(eq(EXISTING_USER_ID));
            verify(userPointTable, times(1)).insertOrUpdate(eq(EXISTING_USER_ID), eq(2000L));
            verify(pointHistoryTable, times(1)).insert(
                    eq(EXISTING_USER_ID),
                    eq(1000L),
                    eq(TransactionType.CHARGE),
                    eq(1234L)
            );

            assertNotNull(rtn);
            assertEquals(EXISTING_USER_ID, rtn.id());
            assertEquals(2000L, rtn.point());
            assertThat(rtn.updateMillis()).isNotNull();

            verify(userPointTable, times(1)).selectById(eq(EXISTING_USER_ID));
            verify(userPointTable, times(1)).insertOrUpdate(anyLong(), anyLong());
            verify(pointHistoryTable, times(1)).insert(anyLong(), anyLong(), any(), anyLong());
        }

        @Test
        @DisplayName("포인트 충전 - 유효하지 않은 경우 예외 발생 - 음수 충전")
        public void charge_isNotValid_shouldThrow_minusPoint() {
            // given
            long initialPoint = 9000L;
            when(userPointTable.selectById(eq(EXISTING_USER_ID)))
                    .thenReturn(new UserPoint(EXISTING_USER_ID, initialPoint, DEFAULT_CURRENT_TIME));

            // when
            assertThatThrownBy(() -> pointService.charge(EXISTING_USER_ID, -1000L, 1234L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("충전 포인트는 양수여야 합니다. 충전 시도량: -1000");


            // then
            verify(userPointTable, times(1)).selectById(eq(EXISTING_USER_ID));
            verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
            verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
        }

        @Test
        @DisplayName("포인트 충전 - 유효하지 않은 경우 예외 발생 - 한도 초과")
        public void charge_isNotValid_shouldThrow_overPoint() throws Exception {
            // given
            long initialPoint = 9000L;
            when(userPointTable.selectById(eq(EXISTING_USER_ID)))
                    .thenReturn(new UserPoint(EXISTING_USER_ID, initialPoint, DEFAULT_CURRENT_TIME));


            // when
            assertThatThrownBy(() -> pointService.charge(EXISTING_USER_ID, 2000L, 1234L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("충전 한도 초과. 현재 포인트: 9000, 충전 시도량: 2000, 한도: 10000");

            //then
            verify(userPointTable, times(1)).selectById(eq(EXISTING_USER_ID));
            verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
            verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
        }

    }


}