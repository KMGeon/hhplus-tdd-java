package io.hhplus.tdd.service;

import io.hhplus.tdd.ContextConfiguration;
import io.hhplus.tdd.domain.PointHistory;
import io.hhplus.tdd.domain.UserPoint;
import org.junit.jupiter.api.*;

import java.util.List;

import static io.hhplus.tdd.domain.UserPoint.isNewUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

public class PointServiceTest extends ContextConfiguration {

    @BeforeEach
    public void setUp() {
        timeProvider.reset();
    }

    @Test
    @DisplayName("특정 유저가 신규인지 판단")
    public void isValidationNewUser() throws Exception {
        // given
        // when
        UserPoint point = pointService.getUserPointWithDefault(2L);
        boolean newUser = isNewUser(point);

        // then
        assertTrue(newUser, "유저는 없기 때문에 현재 시간이랑 같다. (신규유저)");
    }

    @Test
    @DisplayName("특정 유저가 기존유저 판단")
    public void isValidationExistUser() throws Exception {
        // given
        setUpExistUserPoint(EXIST_USER_ID);

        // when
        UserPoint existUser = pointService.getUserPointWithDefault(EXIST_USER_ID);
        boolean isNewUser = isNewUser(existUser);

        // then
        assertFalse(isNewUser);
    }

    @Test
    @DisplayName("포인트 충전 후 신규 유저가 아님을 검증")
    public void chargePointAndVerifyNotNewUser() throws Exception {
        // given
        setUpExistUserPoint(EXIST_USER_ID);
        long currentTimeMillis = currentTimeMillis();

        UserPoint charge = pointService.charge(
                EXIST_USER_ID,
                DEFAULT_POINT,
                currentTimeMillis
        );
        boolean isNewUser = isNewUser(charge);

        assertFalse(isNewUser, "setUpExistUserPoint으로 유저를 먼저 세팅하여 시간이 다르다.");
    }

    @Test
    public void 포인트_충전() throws Exception {
        // given
        long currentTimeMillis = currentTimeMillis();
        // when
        UserPoint chargeUserPoint = pointService.charge(
                NEW_USER_ID,
                DEFAULT_POINT,
                currentTimeMillis
        );

        List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(NEW_USER_ID);

        // then
        assertAll(
                () -> assertThat(chargeUserPoint.id()).isEqualTo(NEW_USER_ID),
                () -> assertThat(chargeUserPoint.point()).isEqualTo(DEFAULT_POINT),
                () -> assertThat(chargeUserPoint.updateMillis()).isNotNull()
        );

        assertThat(pointHistories).satisfies(v1 -> {
            PointHistory pointHistory = pointHistories.get(0);
            assertAll(
                    () -> assertThat(pointHistory.userId()).isEqualTo(NEW_USER_ID),
                    () -> assertThat(pointHistory.amount()).isEqualTo(DEFAULT_POINT),
                    () -> assertThat(pointHistory.updateMillis()).isEqualTo(currentTimeMillis)
            );
        });
    }

    @Nested
    @DisplayName("유저 포인트 사용")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class UseCase {

        private static final long USE_NEW_USER_ID = 300;
        private UserPoint userPoint;

        @BeforeEach
        public void setUp() throws Exception {
            userPoint = setUpExistUserPoint(EXIST_USER_ID);
            timeProvider.reset();
        }

        @Test
        @Order(5)
        public void 유저_포인트_사용_성공() throws Exception {
            // given

            // when
            long requestPoint = DEFAULT_POINT - 100L;

            long expectPoint = DEFAULT_POINT - requestPoint;

            UserPoint userPoint = pointService.use(EXIST_USER_ID, requestPoint, currentTimeMillis());
            List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(EXIST_USER_ID);

            // then
            assertAll(
                    () -> assertThat(userPoint.id()).isEqualTo(EXIST_USER_ID),
                    () -> assertThat(userPoint.point()).isEqualTo(expectPoint),
                    () -> assertThat(userPoint.updateMillis()).isNotNull()
            );

            assertThat(pointHistories).satisfies(v1 -> {
                PointHistory pointHistory = pointHistories.get(0);
                assertAll(
                        () -> assertThat(pointHistory.userId()).isEqualTo(EXIST_USER_ID),
                        () -> assertThat(pointHistory.amount()).isEqualTo(requestPoint)
                );
            });
        }

        @Test
        @Order(4)
        @DisplayName("포인트 0원 사용하면 자신 반환")
        public void 유저_포인트_사용_오류_1() throws Exception {
            // given

            // when
            UserPoint use = pointService.use(EXIST_USER_ID, 0L, currentTimeMillis());

            // then
            assertEquals(use, userPoint);
        }

        @Test
        @Order(3)
        @DisplayName("새로운 회원이면 Exception")
        public void 유저_포인트_사용_오류_2() throws Exception {
            // given

            // when
            // then
            assertThatThrownBy(() -> pointService.use(NEW_USER_ID, DEFAULT_POINT, currentTimeMillis()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("존재하지 않는 회원입니다.");
        }

        @Test
        @Order(2)
        @DisplayName("기존 금액보다 많은 포인트 사용 Exception")
        public void 유저_포인트_사용_오류_3() throws Exception {
            // given

            // when
            // then
            assertThatThrownBy(() -> pointService.use(EXIST_USER_ID, DEFAULT_POINT + 1L, currentTimeMillis()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("포인트가 부족합니다.");
        }

        @Test
        @Order(1)
        @DisplayName("""
                상황 : 새로운 유저 + 포인트 부족
                expect : 새로운 유저 Exception 선행
                """)
        public void 유저_포인트_사용_오류_4() throws Exception {
            // given

            // when

            // then
            assertThatThrownBy(() -> pointService.use(NEW_USER_ID, DEFAULT_POINT + 1L, currentTimeMillis()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("존재하지 않는 회원입니다.");
        }
    }
}
