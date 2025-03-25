package io.hhplus.tdd.repository;

import io.hhplus.tdd.domain.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


class UserPointTableTest {

    private UserPointTable userPointTable;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
    }

    private UserPoint setUpUserPoint() {
        return userPointTable.insertOrUpdate(1L, 1000L);
    }

    @Test
    public void 유저_테이블_조회() throws Exception {
        // given

        // when
        UserPoint userPoint = userPointTable.selectById(1L);

        // then
        assertThat(userPoint).isNotNull();
    }

    @Test
    public void 기존_유저_조회() throws Exception {
        // given
        UserPoint existUser = setUpUserPoint();
        // when
        UserPoint findUser = userPointTable.selectById(1L);

        // then
        assertTrue(existUser.equals(findUser));
    }

    @Test
    public void 새로운_유저_생성() throws Exception {
        // given
        // when
        UserPoint insertUser = userPointTable.insertOrUpdate(1L, 1000L);

        // then
        UserPoint findUser = userPointTable.selectById(1L);
        assertTrue(insertUser.equals(findUser));
    }

}