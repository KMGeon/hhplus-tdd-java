package io.hhplus.tdd.service;

import io.hhplus.tdd.domain.PointHistory;
import io.hhplus.tdd.service.transaction.PointTransaction;
import io.hhplus.tdd.service.transaction.TransactionType;
import io.hhplus.tdd.domain.UserPoint;
import io.hhplus.tdd.repository.PointHistoryTable;
import io.hhplus.tdd.repository.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final PointTransactionFinder pointTransactionFinder;

    private final Map<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(PointService.class);


    public UserPoint getUserPointWithDefault(
            final long id
    ) {
        return userPointTable.selectById(id);
    }

    public List<PointHistory> history(
            final long id
    ) {
        return pointHistoryTable.selectAllByUserId(id);
    }


    /**
     * use, charge 모두 하는 것은 비슷하여 Type 따라
     * 컴포넌트를 호출하는 전략패턴으로 Refactor
     *
     * 1. 유저 조회
     * 2. TransactionType 따라서 유효성 검증
     * 3. 유저 포인트 insertOrUpdate
     * 4. 기록 저장하기
     *
     * Re
     */
    public UserPoint executePointTransaction(
            final long userId,
            final long amount,
            final TransactionType transactionType,
            final long timestamp
    ) {
        PointTransaction transaction = pointTransactionFinder.findTransactionService(transactionType);

        ReentrantLock lock = userLocks.computeIfAbsent(userId,
                id -> new ReentrantLock(true));
        lock.lock();
        try {
            logger.info("Lock user hi hi: {}, transaction type: {}", userId, transactionType);
            return transaction.execute(userId, amount, timestamp);
        } finally {
            lock.unlock();
            logger.info("Lock user bye bye: {}, transaction type: {}", userId, transactionType);
        }
    }

    public UserPoint charge(
            final long id,
            final long amount,
            final long currentTimeMillis
    ) {
        UserPoint userPoint = userPointTable.selectById(id);

        long toBeUpdatedUserPoint = userPoint.charge(amount);
        logger.info("[{}.charge] : toBeUpdatedUserPoint: {}",
                getClass().getSimpleName(), toBeUpdatedUserPoint);
        UserPoint savedUserPoint = userPointTable.insertOrUpdate(id, toBeUpdatedUserPoint);

        PointHistory insertHistory = pointHistoryTable.insert(
                id,
                amount,
                TransactionType.CHARGE,
                currentTimeMillis
        );

        logger.info("[{}.charge] : User ID: {}, Amount: {}, Updated: {}",
                getClass().getSimpleName(), id, amount, savedUserPoint);
        logger.info("[{}.charge] : pointHistoryTable. User ID: {}, Amount: {}, TransactionType: {}, PointHistory: {}",
                getClass().getSimpleName(), id, amount, TransactionType.CHARGE, insertHistory);

        return savedUserPoint;
    }


    public UserPoint use(
            final long userId,
            final long amount,
            final long currTimeMillis
    ) {
        UserPoint userPoint = userPointTable.selectById(userId);

        long usePoint = userPoint.use(amount);
        logger.info("[{}.use] : usePoint: {}", getClass().getSimpleName(), amount);

        UserPoint rtn = userPointTable.insertOrUpdate(
                userId,
                usePoint
        );
        pointHistoryTable.insert(
                userId,
                amount,
                TransactionType.USE,
                currTimeMillis
        );

        return rtn;
    }


}
