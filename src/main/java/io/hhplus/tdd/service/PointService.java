package io.hhplus.tdd.service;

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


    public UserPoint getUserPointWithDefault(
            final long id
    ) {
        return userPointTable.selectById(id);
    }

    public List<PointHistory> history(
            final long id
    ) {
        return userPointTable.selectById(id)
                .isNewUser() ? List.of() :
                pointHistoryTable.selectAllByUserId(id);
    }

    public UserPoint charge(
            final long id,
            final long amount,
            final long currentTimeMillis
    ) {
        UserPoint userPoint = userPointTable.selectById(id);

        long toBeUpdatedUserPoint = userPoint.charge(amount);
        logger.info("[{}.charge] : 충전 후 금액 toBeUpdatedUserPoint: {}",
                getClass().getSimpleName(), toBeUpdatedUserPoint);
        UserPoint savedUserPoint = userPointTable.insertOrUpdate(id, toBeUpdatedUserPoint);

        PointHistory insertHistory = pointHistoryTable.insert(
                id,
                amount,
                TransactionType.CHARGE,
                currentTimeMillis
        );

        logger.info("[{}.charge] : userPointTable success. User ID: {}, Amount Charged: {}, Updated UserPoint: {}",
                getClass().getSimpleName(), id, amount, savedUserPoint);
        logger.info("[{}.charge] : pointHistoryTable success. User ID: {}, Amount Charged: {}, TransactionType: {}, PointHistory: {}",
                getClass().getSimpleName(), id, amount, TransactionType.CHARGE, insertHistory);

        return savedUserPoint;
    }
}
