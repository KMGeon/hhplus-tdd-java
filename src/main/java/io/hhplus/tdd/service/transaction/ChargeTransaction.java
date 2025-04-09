package io.hhplus.tdd.service.transaction;

import io.hhplus.tdd.domain.PointHistory;
import io.hhplus.tdd.domain.UserPoint;
import io.hhplus.tdd.repository.PointHistoryTable;
import io.hhplus.tdd.repository.UserPointTable;
import org.springframework.stereotype.Component;

import static io.hhplus.tdd.controller.dto.ThreadLogger.log;

@Component
public class ChargeTransaction implements PointTransaction {
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public ChargeTransaction(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    @Override
    public UserPoint execute(long userId, long amount, long timestamp) {
        log("CHARGE 트랜잭션 시작 - 사용자: " + userId + ", 금액: " + amount);

        UserPoint userPoint = userPointTable.selectById(userId);
        log("사용자 포인트 조회 완료 - 사용자: " + userId + ", 현재 포인트: " + userPoint.point());

        long updatedPoint = calculatePoint(userPoint, amount);
        log("포인트 계산 완료 - 사용자: " + userId + ", 계산된 포인트: " + updatedPoint);

        UserPoint savedUserPoint = userPointTable.insertOrUpdate(userId, updatedPoint);
        log("포인트 업데이트 완료 - 사용자: " + userId + ", 업데이트된 포인트: " + savedUserPoint.point());

        PointHistory insertHistory = createHistory(userId, amount, timestamp);
        log("이력 저장 완료 - 사용자: " + userId + ", history ID: " + insertHistory.id());


        log("CHARGE 트랜잭션 완료 - 사용자: " + userId);
        return savedUserPoint;
    }

    @Override
    public long calculatePoint(UserPoint userPoint, long amount) {
        log("포인트 충전 계산 - 현재: " + userPoint.point() + ", 충전액: " + amount);
        return userPoint.charge(amount);
    }

    @Override
    public PointHistory createHistory(long userId, long amount, long timestamp) {
        log("충전 이력 생성 - 사용자: " + userId + ", 금액: " + amount);
        return pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, timestamp);
    }

    @Override
    public TransactionType getTransactionType() {
        return TransactionType.CHARGE;
    }
}