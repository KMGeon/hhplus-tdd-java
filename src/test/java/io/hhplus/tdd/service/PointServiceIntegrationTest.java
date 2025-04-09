package io.hhplus.tdd.service;

import io.hhplus.tdd.domain.PointHistory;
import io.hhplus.tdd.service.transaction.TransactionType;
import io.hhplus.tdd.domain.UserPoint;
import io.hhplus.tdd.repository.PointHistoryTable;
import io.hhplus.tdd.repository.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(
        classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD
)
public class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointTable userPointTable;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    private static final long USER_ID = 1L;
    private static final long ANOTHER_USER_ID = 2L;
    private static final long TEST_TIME = System.currentTimeMillis();


    @Nested
    @DisplayName("사용자 포인트 조회 테스트")
    class GetUserPointTest {

        @Test
        @DisplayName("존재하는 사용자의 포인트 조회")
        void getUserPointWithDefault_ExistingUser_ShouldReturnUserPoint() {
            // given
            final long initialPoint = 1000L;
            userPointTable.insertOrUpdate(USER_ID, initialPoint);

            // when
            UserPoint userPoint = pointService.getUserPointWithDefault(USER_ID);

            // then
            assertAll(
                    () -> assertNotNull(userPoint, "getOrDefault로 신규 유저를 반환한다."),
                    () -> assertEquals(USER_ID, userPoint.id(), "사용자 ID가 일치"),
                    () -> assertEquals(initialPoint, userPoint.point(), "포인트 값이 일치")
            );
        }

        @Test
        @DisplayName("신규 사용자의 포인트 조회 - 기본값 테스트")
        void getUserPointWithDefault_NewUser_ShouldReturnEmptyUserPoint() {
            // given
            final long newUserId = 999L;

            // when
            UserPoint userPoint = pointService.getUserPointWithDefault(newUserId);
            System.out.println("userPoint = " + userPoint);

            // then
            assertAll(
                    () -> assertNotNull(userPoint),
                    () -> assertEquals(newUserId, userPoint.id()),
                    () -> assertEquals(0, userPoint.point()),
                    () -> assertTrue(userPoint.updateMillis() > 0)
            );
        }
    }

    @Nested
    @DisplayName("포인트 충전 테스트")
    class ChargePointTest {

        @Test
        @DisplayName("포인트 충전 - 성공 케이스")
        void charge_ValidAmount_ShouldIncreaseUserPoint() {
            // given
            final long chargeAmount = 1000L;

            // when
            UserPoint userPoint = pointService.charge(USER_ID, chargeAmount, TEST_TIME);

            // then
            assertAll(
                    () -> assertNotNull(userPoint),
                    () -> assertEquals(USER_ID, userPoint.id()),
                    () -> assertEquals(chargeAmount, userPoint.point()),

                    () -> {
                        UserPoint savedUserPoint = userPointTable.selectById(USER_ID);
                        assertEquals(chargeAmount, savedUserPoint.point());
                    },

                    () -> {
                        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(USER_ID);
                        assertFalse(histories.isEmpty());

                        PointHistory lastHistory = histories.get(histories.size() - 1);
                        assertEquals(USER_ID, lastHistory.userId());
                        assertEquals(chargeAmount, lastHistory.amount());
                        assertEquals(TransactionType.CHARGE, lastHistory.type());
                    }
            );
        }

        @ParameterizedTest
        @ValueSource(longs = {1, 1, 5000, 10000})
        @DisplayName("다양한 금액의 포인트 충전 테스트")
        void charge_VariousAmounts_ShouldSucceed(long amount) {
            // when
            UserPoint userPoint = pointService.charge(USER_ID, amount, TEST_TIME);

            // then
            assertEquals(amount, userPoint.point(), "충전 금액이 정확하게 반영이 되어야 한다.");
            UserPoint savedUserPoint = userPointTable.selectById(USER_ID);
            assertEquals(amount, savedUserPoint.point(), "DB에 저장된 포인트가 일치해야 함");
        }

        @Test
        @DisplayName("포인트 충전 한도 초과 테스트")
        void charge_ExceedingLimit_ShouldThrowException() {
            // given
            final long excessiveAmount = 11000L; // LIMIT_POINT(10000)보다 큰 값

            // when
            // then
            assertThatThrownBy(() -> pointService.charge(USER_ID, excessiveAmount, TEST_TIME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("충전 한도 초과");

            // 포인트가 증가 X
            UserPoint userPoint = userPointTable.selectById(USER_ID);
            assertEquals(0, userPoint.point(), "예외 발생 시 포인트가 변경 X");

            // 히스토리에 기록 X
            List<PointHistory> histories = pointHistoryTable.selectAllByUserId(USER_ID);
            assertTrue(histories.isEmpty(), "실패한 트랜잭션은 히스토리에 기록 X");
        }

        @Test
        @DisplayName("음수 금액 충전 시도 테스트")
        void charge_NegativeAmount_ShouldThrowException() {
            // given
            final long negativeAmount = -500L;

            // when
            // then
            assertThatThrownBy(() -> pointService.charge(USER_ID, negativeAmount, TEST_TIME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("충전 포인트는 양수");

            // 포인트가 변경되지 않았는지 확인
            UserPoint userPoint = userPointTable.selectById(USER_ID);
            assertEquals(0, userPoint.point(), "예외 발생 시 포인트가 변경되지 않아야 함");
        }

        @Test
        @DisplayName("여러 번 충전 시 포인트 누적 테스트")
        void charge_MultipleCharges_ShouldAccumulatePoints() {
            // given
            final long firstCharge = 1000L;
            final long secondCharge = 2000L;
            final long thirdCharge = 3000L;

            // when
            pointService.charge(USER_ID, firstCharge, TEST_TIME);
            pointService.charge(USER_ID, secondCharge, TEST_TIME);
            UserPoint userPoint = pointService.charge(USER_ID, thirdCharge, TEST_TIME);

            // then
            final long expectedTotal = firstCharge + secondCharge + thirdCharge;
            assertEquals(expectedTotal, userPoint.point(), "여러 번 충전 후 총 포인트가 정확해야 함");

            List<PointHistory> histories = pointHistoryTable.selectAllByUserId(USER_ID);
            assertEquals(3, histories.size(), "3번의 충전 기록이 있어야 함");

            assertEquals(firstCharge, histories.get(0).amount(), "첫 번째 충전 기록의 금액이 일치해야 함");
            assertEquals(secondCharge, histories.get(1).amount(), "두 번째 충전 기록의 금액이 일치해야 함");
            assertEquals(thirdCharge, histories.get(2).amount(), "세 번째 충전 기록의 금액이 일치해야 함");
        }

        @Test
        @DisplayName("한도에 근접한 포인트 충전 테스트")
        void charge_NearLimit_ShouldSucceed() {
            // given
            final long firstCharge = 6000L;
            final long secondCharge = 4000L; // 합계 10000, 한도까지 정확히

            // when
            pointService.charge(USER_ID, firstCharge, TEST_TIME);
            UserPoint userPoint = pointService.charge(USER_ID, secondCharge, TEST_TIME);

            // then
            assertEquals(10000L, userPoint.point(), "한도까지 충전이 가능해야 함");

            // 한도 초과 시도
            assertThatThrownBy(() -> pointService.charge(USER_ID, 1L, TEST_TIME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("충전 한도 초과");
        }
    }

    @Nested
    @DisplayName("포인트 사용 테스트")
    class UsePointTest {

        @Test
        @DisplayName("포인트 사용 - 성공 케이스")
        void use_ValidAmount_ShouldDecreaseUserPoint() {
            // given
            final long initialCharge = 2000L;
            final long useAmount = 500L;

            pointService.charge(USER_ID, initialCharge, TEST_TIME);

            // when
            UserPoint userPoint = pointService.use(USER_ID, useAmount, TEST_TIME);

            // then
            assertAll(
                    () -> assertNotNull(userPoint, "사용 결과는 null이 아니어야 함"),
                    () -> assertEquals(USER_ID, userPoint.id(), "사용자 ID가 일치해야 함"),
                    () -> assertEquals(initialCharge - useAmount, userPoint.point(), "사용 후 포인트가 정확해야 함"),

                    // DB에 실제로 반영되었는지 확인
                    () -> {
                        UserPoint savedUserPoint = userPointTable.selectById(USER_ID);
                        assertEquals(initialCharge - useAmount, savedUserPoint.point(), "DB에 저장된 포인트가 일치해야 함");
                    },

                    () -> {
                        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(USER_ID);
                        assertEquals(2, histories.size(), "충전과 사용 기록이 모두 있어야 함");

                        PointHistory lastHistory = histories.get(histories.size() - 1);
                        assertEquals(USER_ID, lastHistory.userId(), "히스토리의 사용자 ID가 일치해야 함");
                        assertEquals(useAmount, lastHistory.amount(), "히스토리의 금액이 일치해야 함");
                        assertEquals(TransactionType.USE, lastHistory.type(), "히스토리의 타입이 USE");
                    }
            );
        }

        @Test
        @DisplayName("포인트 전액 사용 테스트")
        void use_EntireAmount_ShouldResultInZeroBalance() {
            // given
            final long initialCharge = 2000L;

            pointService.charge(USER_ID, initialCharge, TEST_TIME);

            // when
            UserPoint userPoint = pointService.use(USER_ID, initialCharge, TEST_TIME);

            // then
            assertEquals(0, userPoint.point(), "모든 포인트 사용 후 잔액은 0이어야 함");

            // DB 확인
            UserPoint savedUserPoint = userPointTable.selectById(USER_ID);
            assertEquals(0, savedUserPoint.point(), "DB에 저장된 포인트도 0이어야 함");
        }

        @Test
        @DisplayName("포인트 부족 시 예외 발생 테스트")
        void use_InsufficientPoints_ShouldThrowException() {
            // given
            final long initialCharge = 300L;
            final long useAmount = 500L;

            pointService.charge(USER_ID, initialCharge, TEST_TIME);

            // when
            // then
            assertThatThrownBy(() -> pointService.use(USER_ID, useAmount, TEST_TIME))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("포인트가 부족합니다");

            UserPoint userPoint = userPointTable.selectById(USER_ID);
            assertEquals(initialCharge, userPoint.point(), "예외 발생 시 포인트가 변경되지 않아야 함");

            List<PointHistory> histories = pointHistoryTable.selectAllByUserId(USER_ID);
            assertEquals(1, histories.size(), "실패한 트랜잭션은 히스토리에 기록되지 않아야 함");
            assertEquals(TransactionType.CHARGE, histories.get(0).type(), "히스토리에는 충전 기록만 있어야 함");
        }

        @Test
        @DisplayName("여러 번 포인트 사용 테스트")
        void use_MultipleUses_ShouldDecrementCorrectly() {
            // given
            final long initialCharge = 5000L;
            final long firstUse = 1000L;
            final long secondUse = 2000L;

            pointService.charge(USER_ID, initialCharge, TEST_TIME);

            // when
            pointService.use(USER_ID, firstUse, TEST_TIME);
            UserPoint userPoint = pointService.use(USER_ID, secondUse, TEST_TIME);

            // then
            final long expectedRemaining = initialCharge - firstUse - secondUse;
            assertEquals(expectedRemaining, userPoint.point(), "여러 번 사용 후 남은 포인트가 정확해야 함");

            List<PointHistory> histories = pointHistoryTable.selectAllByUserId(USER_ID);
            assertEquals(3, histories.size(), "충전 1번, 사용 2번의 기록이 있어야 함");

            assertEquals(TransactionType.CHARGE, histories.get(0).type());
            assertEquals(initialCharge, histories.get(0).amount());

            assertEquals(TransactionType.USE, histories.get(1).type());
            assertEquals(firstUse, histories.get(1).amount());

            assertEquals(TransactionType.USE, histories.get(2).type());
            assertEquals(secondUse, histories.get(2).amount());
        }

        @Test
        @DisplayName("잔액 이상 포인트 사용 시도 테스트")
        void use_ExactlyBalanceAmount_ShouldSucceed() {
            // given
            final long initialCharge = 1000L;

            pointService.charge(USER_ID, initialCharge, TEST_TIME);

            // when
            UserPoint userPoint = pointService.use(USER_ID, initialCharge, TEST_TIME);

            // then
            assertEquals(0, userPoint.point(), "잔액 전체 사용 후 포인트는 0이어야 함");

            // 포인트가 없을 때 사용할 때
            assertThatThrownBy(() -> pointService.use(USER_ID, 1L, TEST_TIME))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("포인트가 부족합니다");
        }
    }

    @Nested
    @DisplayName("포인트 내역 조회 테스트")
    class PointHistoryTest {

        @Test
        @DisplayName("포인트 내역 조회 - 기본 테스트")
        void history_ShouldReturnAllUserTransactions() {
            // given
            final long firstCharge = 1000L;
            final long useAmount = 300L;
            final long secondCharge = 500L;

            // 여러 트랜잭션 생성
            pointService.charge(USER_ID, firstCharge, TEST_TIME);
            pointService.use(USER_ID, useAmount, TEST_TIME);
            pointService.charge(USER_ID, secondCharge, TEST_TIME);

            // when
            List<PointHistory> histories = pointService.history(USER_ID);

            // then
            assertAll(
                    () -> assertEquals(3, histories.size(), "충전, 사용, 충전 총 3개의 내역이 있어야 함"),

                    // 처음 충전
                    () -> {
                        PointHistory first = histories.get(0);
                        assertEquals(USER_ID, first.userId());
                        assertEquals(firstCharge, first.amount());
                        assertEquals(TransactionType.CHARGE, first.type());
                    },

                    // 사용
                    () -> {
                        PointHistory second = histories.get(1);
                        assertEquals(USER_ID, second.userId());
                        assertEquals(useAmount, second.amount());
                        assertEquals(TransactionType.USE, second.type());
                    },

                    // 두 번째 충전
                    () -> {
                        PointHistory third = histories.get(2);
                        assertEquals(USER_ID, third.userId());
                        assertEquals(secondCharge, third.amount());
                        assertEquals(TransactionType.CHARGE, third.type());
                    },

                    // 1000 - 300 + 500 = 1200
                    () -> {
                        UserPoint finalUserPoint = userPointTable.selectById(USER_ID);
                        assertEquals(1200L, finalUserPoint.point());
                    }
            );
        }

        @Test
        @DisplayName("거래 내역이 없는 사용자의 히스토리 조회")
        void history_ForUserWithNoTransactions_ShouldReturnEmptyList() {
            // given
            final long newUserId = 9999L;

            // when
            List<PointHistory> histories = pointService.history(newUserId);

            // then
            assertTrue(histories.isEmpty(), "거래 내역이 없는 사용자의 히스토리는 빈 목록이어야 함");
        }

        @Test
        @DisplayName("여러 사용자의 포인트 내역 독립성 테스트")
        void history_MultipeUsers_ShouldReturnCorrectHistory() {
            // given
            // 첫 번째 사용자 트랜잭션
            pointService.charge(USER_ID, 1000L, TEST_TIME);
            pointService.use(USER_ID, 300L, TEST_TIME);

            // 두 번째 사용자 트랜잭션
            pointService.charge(ANOTHER_USER_ID, 2000L, TEST_TIME);
            pointService.use(ANOTHER_USER_ID, 500L, TEST_TIME);
            pointService.charge(ANOTHER_USER_ID, 1000L, TEST_TIME);

            // when
            List<PointHistory> user1Histories = pointService.history(USER_ID);
            List<PointHistory> user2Histories = pointService.history(ANOTHER_USER_ID);

            // then
            assertEquals(2, user1Histories.size(), "첫 번째 사용자는 2개의 내역이 있어야 함");
            assertEquals(3, user2Histories.size(), "두 번째 사용자는 3개의 내역이 있어야 함");

            // 각 사용자의 내역이 올바른지 확인
            for (PointHistory history : user1Histories) {
                assertEquals(USER_ID, history.userId(), "첫 번째 사용자의 내역에는 해당 사용자 ID만 있어야 함");
            }

            for (PointHistory history : user2Histories) {
                assertEquals(ANOTHER_USER_ID, history.userId(), "두 번째 사용자의 내역에는 해당 사용자 ID만 있어야 함");
            }
        }
    }

    @Nested
    @DisplayName("다양한 트랜잭션 테스트")
    class ComplexTransactionTest {

        @Test
        @DisplayName("충전, 사용, 충전, 사용의 복합 시나리오 테스트")
        void complexScenario_ShouldWorkCorrectly() {
            // given
            final long initialCharge = 5000L;
            final long firstUse = 2000L;
            final long secondCharge = 3000L;
            final long secondUse = 4000L;

            // when
            pointService.charge(USER_ID, initialCharge, TEST_TIME); // 잔액: 5000
            pointService.use(USER_ID, firstUse, TEST_TIME);         // 잔액: 3000
            pointService.charge(USER_ID, secondCharge, TEST_TIME);  // 잔액: 6000
            UserPoint finalUserPoint = pointService.use(USER_ID, secondUse, TEST_TIME); // 잔액: 2000

            // then
            final long expectedFinalPoint = initialCharge - firstUse + secondCharge - secondUse;
            assertEquals(expectedFinalPoint, finalUserPoint.point(), "복합 트랜잭션 후 최종 포인트가 정확해야 함");

            List<PointHistory> histories = pointService.history(USER_ID);
            assertEquals(4, histories.size(), "총 4개의 트랜잭션 기록이 있어야 함");

            // 트랜잭션 순서 확인
            assertEquals(TransactionType.CHARGE, histories.get(0).type());
            assertEquals(TransactionType.USE, histories.get(1).type());
            assertEquals(TransactionType.CHARGE, histories.get(2).type());
            assertEquals(TransactionType.USE, histories.get(3).type());

            assertEquals(initialCharge, histories.get(0).amount());
            assertEquals(firstUse, histories.get(1).amount());
            assertEquals(secondCharge, histories.get(2).amount());
            assertEquals(secondUse, histories.get(3).amount());
        }

        @Test
        @DisplayName("한도까지 충전 후 전액 사용 테스트")
        void chargeToLimitThenUseAll_ShouldWorkCorrectly() {
            // given
            final long maxCharge = 10000L; // 최대 한도

            // when
            pointService.charge(USER_ID, maxCharge, TEST_TIME); // 잔액: 10000
            UserPoint finalUserPoint = pointService.use(USER_ID, maxCharge, TEST_TIME); // 잔액: 0

            // then
            assertEquals(0, finalUserPoint.point(), "전액 사용 후 포인트는 0이어야 함");

            List<PointHistory> histories = pointService.history(USER_ID);
            assertEquals(2, histories.size(), "충전과 사용 기록이 각각 1개씩 있어야 함");

            assertEquals(TransactionType.CHARGE, histories.get(0).type());
            assertEquals(maxCharge, histories.get(0).amount());

            assertEquals(TransactionType.USE, histories.get(1).type());
            assertEquals(maxCharge, histories.get(1).amount());
        }
    }
}