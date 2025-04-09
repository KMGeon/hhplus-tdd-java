package io.hhplus.tdd.service.transaction;

import io.hhplus.tdd.domain.PointHistory;
import io.hhplus.tdd.domain.UserPoint;


public interface PointTransaction {

    UserPoint execute(long userId, long amount, long timestamp);

    long calculatePoint(UserPoint userPoint, long amount);

    PointHistory createHistory(long userId, long amount, long timestamp);

    TransactionType getTransactionType();
}