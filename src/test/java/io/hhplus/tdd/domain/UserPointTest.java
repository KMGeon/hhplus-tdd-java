package io.hhplus.tdd.domain;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class UserPointTest {

    private static final long USER_ID = 1L;

    @ParameterizedTest
    @ValueSource(longs = {1L, 100L, 9999L})
    void 신규유저_생성(long userId) {
        // when
        UserPoint rtn = UserPoint.empty(userId);

        // then
        assertEquals(userId, rtn.id());
        assertEquals(0, rtn.point());
        assertTrue(rtn.updateMillis() > 0);
    }

    /**
     *
     * @param initialPoint : 초기 포인트
     * @param chargeAmount : 충전 포인트
     * @param expectedPoint : 기대 포인트
     */
    @ParameterizedTest
    @CsvSource({
            "0, 1000, 1000",
            "5000, 3000, 8000",
            "9000, 1000, 10000",
            "5000, 0, 5000"
    })
    void 포인트_충전_성공_테스트(long initialPoint, long chargeAmount, long expectedPoint) {
        // given
        UserPoint userPoint = new UserPoint(
                USER_ID,
                initialPoint,
                System.currentTimeMillis()
        );

        // when
        long newPoint = userPoint.charge(chargeAmount);

        // then
        assertEquals(expectedPoint, newPoint);
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, -100, -5000})
    void 음수_포인트_충전_예외_테스트(long negativeAmount) {
        // given
        UserPoint userPoint = new UserPoint(
                USER_ID,
                5000,
                System.currentTimeMillis()
        );

        // when
        // then
        assertThatThrownBy(() -> userPoint.charge(negativeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("충전 포인트는 양수여야 합니다");
    }

    /**
     *
     * @param initialPoint : 초기 포인트
     * @param chargeAmount : 충전 포인트
     */
    @ParameterizedTest
    @CsvSource({
            "8000, 3000",    // 일반적인 한도 초과
            "9000, 1001",    // 한도에서 1만큼 더 충전
            "10000, 1",      // 한도 -> 충전
            "0, 10001"       // 바로 한도를 넘어서 충전
    })
    void 한도초과_충전_예외_테스트(long initialPoint, long chargeAmount) {
        // given
        UserPoint userPoint = new UserPoint(
                USER_ID,
                initialPoint,
                System.currentTimeMillis()
        );

        // when
        // then
        assertThatThrownBy(() -> userPoint.charge(chargeAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("충전 한도 초과");

    }

    @Test
    public void 신규_유저_판단() throws Exception{
        // given
        // when
        boolean newUser = UserPoint.empty(USER_ID).isNewUser();
        // then
        assertEquals(newUser,true,"");
    }
}