package io.hhplus.tdd.service;

import io.hhplus.tdd.ContextConfiguration;
import io.hhplus.tdd.domain.PointHistory;
import io.hhplus.tdd.domain.UserPoint;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.hhplus.tdd.domain.UserPoint.isNewUser;
import static org.junit.jupiter.api.Assertions.*;

public class PointServiceTest extends ContextConfiguration {

    private static final Long NEW_USER_ID = 1L;
    private static final Long EXIST_USER_ID = 99L;
    private static final Long DEFAULT_POINT = 1000L;

    @Test
    @DisplayName("특정 유저가 신규인지 판단")
    public void isValidationNewUser() throws Exception{
        // given
        // when
        UserPoint point = pointService.point(NEW_USER_ID);
        boolean newUser = isNewUser(point);

        // then
        assertTrue(newUser, "유저는 없기 때문에 현재 시간이랑 같다. (신규유저)");
    }

    @Test
    @DisplayName("특정 유저가 기존유저 판단")
    public void isValidationExistUser() throws Exception{
        // given
        setUpExistUserPoint(EXIST_USER_ID);

        // when
        UserPoint existUser = pointService.point(EXIST_USER_ID);
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
    public void 포인트_충전() throws Exception{
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
                () -> Assertions.assertThat(chargeUserPoint.id()).isEqualTo(NEW_USER_ID),
                () -> Assertions.assertThat(chargeUserPoint.point()).isEqualTo(DEFAULT_POINT),
                () -> Assertions.assertThat(chargeUserPoint.updateMillis()).isNotNull()
        );

        Assertions.assertThat(pointHistories).satisfies(v1-> {
            PointHistory pointHistory = pointHistories.get(0);
            assertAll(
                    () -> Assertions.assertThat(pointHistory.userId()).isEqualTo(NEW_USER_ID),
                    () -> Assertions.assertThat(pointHistory.amount()).isEqualTo(DEFAULT_POINT),
                    () -> Assertions.assertThat(pointHistory.updateMillis()).isEqualTo(currentTimeMillis)
            );
        });
    }
}
