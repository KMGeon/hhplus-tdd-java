package io.hhplus.tdd.service;

import io.hhplus.tdd.common.exception.NotFoundUserException;
import io.hhplus.tdd.domain.PointHistory;
import io.hhplus.tdd.domain.TransactionType;
import io.hhplus.tdd.domain.UserPoint;
import io.hhplus.tdd.repository.PointHistoryTable;
import io.hhplus.tdd.repository.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    private static final Logger logger = LoggerFactory.getLogger(PointService.class);

    public UserPoint point(
            final long userId
    ) {
        validateUserExistence(userId);

        return userPointTable.selectById(userId);
    }

    private void validateUserExistence(long userId) {
        boolean isNewUser = userPointTable.selectById(userId).isNewUser();
        logger.info("[{}.validateUserExistence] isNewUser: {}", getClass().getSimpleName(), isNewUser);
        if (isNewUser) throw new NotFoundUserException(userId);
    }

    public List<PointHistory> history(
            final long userId
    ) {
        validateUserExistence(userId);
        
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public UserPoint charge(
            final long id,
            final long amount,
            final long currentTimeMillis
    ) {
        UserPoint userPoint = userPointTable.insertOrUpdate(id, amount);
        PointHistory insertHistory = pointHistoryTable.insert(id, amount, TransactionType.CHARGE, currentTimeMillis);

        logger.info("[{}.charge] : userPointTable successful. User ID: {}, Amount Charged: {}, Updated UserPoint: {}",
                getClass().getSimpleName(), id, amount, userPoint);
        logger.info("[{}.charge] : pointHistoryTable successful. User ID: {}, Amount Charged: {}, TransactionType: {}, PointHistory: {}",
                getClass().getSimpleName(), id, amount, TransactionType.CHARGE, insertHistory);

        return userPoint;
    }

    public UserPoint use(
            final long id,
            final long amount,
            final long currTimeMillis
    ) {
        UserPoint userPoint = userPointTable.selectById(id);
        long currentUserId = userPoint.id();

        boolean zeroPointUse = userPoint.isZeroPointUse(amount);
        if (zeroPointUse) return userPoint;

        UserPoint validUse = userPoint.isValidUse(amount, currTimeMillis);
        long remainPoint = validUse.point();

        UserPoint rtn = userPointTable.insertOrUpdate(currentUserId, remainPoint);
        pointHistoryTable.insert(currentUserId, amount, TransactionType.USE, currTimeMillis);

        return rtn;
    }
}

