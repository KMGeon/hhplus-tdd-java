package io.hhplus.tdd.repository;


import io.hhplus.tdd.domain.PointHistory;
import io.hhplus.tdd.domain.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PointHistoryTableTest {

    private PointHistoryTable pointHistoryTable;
    private final long USER_ID = 1L;
    private final long AMOUNT = 1000L;
    private final long UPDATE_MILLIS = System.currentTimeMillis();

    @BeforeEach
    void setUp() {
        pointHistoryTable = new PointHistoryTable();
    }

    @Test
    @DisplayName("포인트 내역 저장 후 조회 시 저장된 내역이 반환된다")
    void insert_shouldCreateAndReturnPointHistory() {
        // given
        TransactionType transactionType = TransactionType.CHARGE;

        // when
        PointHistory pointHistory = pointHistoryTable.insert(USER_ID, AMOUNT, transactionType, UPDATE_MILLIS);

        // then
        assertThat(pointHistory).isNotNull();
        assertThat(pointHistory.userId()).isEqualTo(USER_ID);
        assertThat(pointHistory.amount()).isEqualTo(AMOUNT);
        assertThat(pointHistory.type()).isEqualTo(transactionType);
        assertThat(pointHistory.updateMillis()).isEqualTo(UPDATE_MILLIS);
    }

    @Test
    @DisplayName("여러 내역 저장 후 사용자 ID로 조회 시 해당 사용자의 모든 내역이 반환된다")
    void selectAllByUserId_shouldReturnAllHistoriesForUserId() {
        // given
        long anotherUserId = 2L;
        pointHistoryTable.insert(USER_ID, AMOUNT, TransactionType.CHARGE, UPDATE_MILLIS);
        pointHistoryTable.insert(USER_ID, 500L, TransactionType.USE, UPDATE_MILLIS + 1000);
        pointHistoryTable.insert(anotherUserId, 2000L, TransactionType.CHARGE, UPDATE_MILLIS);

        // when
        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(USER_ID);

        // then
        assertThat(histories).hasSize(2);
        assertThat(histories).allMatch(history -> history.userId() == USER_ID);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 조회 시 빈 리스트가 반환된다")
    void selectAllByUserId_shouldReturnEmptyList_whenUserHasNoHistory() {
        // given
        long nonExistentUserId = 999L;

        // when
        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(nonExistentUserId);

        // then
        assertThat(histories).isEmpty();
    }

    @Test
    @DisplayName("내역 저장 시 ID가 자동으로 증가한다")
    void insert_shouldAutoIncrementId() {
        // given & when
        PointHistory firstHistory = pointHistoryTable.insert(USER_ID, AMOUNT, TransactionType.CHARGE, UPDATE_MILLIS);
        PointHistory secondHistory = pointHistoryTable.insert(USER_ID, AMOUNT, TransactionType.CHARGE, UPDATE_MILLIS);

        // then
        assertThat(secondHistory.id()).isGreaterThan(firstHistory.id());
        assertThat(secondHistory.id() - firstHistory.id()).isEqualTo(1);
    }

    @Test
    @DisplayName("여러 사용자의 내역을 저장하고 각각 조회할 수 있다")
    void canInsertAndRetrieveHistoriesForMultipleUsers() {
        // given
        long user1 = 1L;
        long user2 = 2L;
        long user3 = 3L;

        pointHistoryTable.insert(user1, 1000L, TransactionType.CHARGE, UPDATE_MILLIS);
        pointHistoryTable.insert(user2, 2000L, TransactionType.CHARGE, UPDATE_MILLIS);
        pointHistoryTable.insert(user3, 3000L, TransactionType.CHARGE, UPDATE_MILLIS);
        pointHistoryTable.insert(user1, 500L, TransactionType.USE, UPDATE_MILLIS);

        // when
        List<PointHistory> user1Histories = pointHistoryTable.selectAllByUserId(user1);
        List<PointHistory> user2Histories = pointHistoryTable.selectAllByUserId(user2);
        List<PointHistory> user3Histories = pointHistoryTable.selectAllByUserId(user3);

        // then
        assertThat(user1Histories).hasSize(2);
        assertThat(user2Histories).hasSize(1);
        assertThat(user3Histories).hasSize(1);

        assertThat(user1Histories).allMatch(history -> history.userId() == user1);
        assertThat(user2Histories).allMatch(history -> history.userId() == user2);
        assertThat(user3Histories).allMatch(history -> history.userId() == user3);
    }
}