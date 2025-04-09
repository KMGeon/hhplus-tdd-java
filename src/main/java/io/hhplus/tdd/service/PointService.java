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

    /**
     * 유저 ID를 통해 유저 포인트 정보를 조회합니다.
     * - 유저 포인트 정보가 없을 경우 기본값을 제공할 수 있습니다.
     *
     * @param id 유저 ID
     * @return 해당 유저의 포인트 정보
     */
    public UserPoint getUserPointWithDefault(
            final long id
    ) {
        return userPointTable.selectById(id);
    }

    /**
     * 특정 유저의 포인트 사용/충전 이력을 조회합니다.
     *
     * @param id 유저 ID
     * @return 해당 유저의 포인트 이력 리스트
     */
    public List<PointHistory> history(
            final long id
    ) {
        return pointHistoryTable.selectAllByUserId(id);
    }

    /**
     * 사용 및 충전과 같은 포인트 트랜잭션을 실행합니다.
     * - TransactionType에 따라 적절한 전략을 찾아 실행합니다.
     * - 사용자별로 Lock을 사용하여 동시처리 문제를 방지합니다.
     *
     * @param userId        유저 ID
     * @param amount        트랜잭션 금액
     * @param transactionType 트랜잭션 타입 (USE, CHARGE 등)
     * @param timestamp     트랜잭션 타임스탬프
     * @return 업데이트된 유저의 포인트 정보
     */
    public UserPoint executePointTransaction(
            final long userId,
            final long amount,
            final TransactionType transactionType,
            final long timestamp
    ) {
        /**트랜잭션 타입을 변수로 받아 타입에 맞는 컴포넌트 호출**/
        PointTransaction transaction = pointTransactionFinder.findTransactionService(transactionType);

        ReentrantLock lock = userLocks.computeIfAbsent(userId,
                id -> new ReentrantLock(true));
        lock.lock();
        try {
            logger.info("Lock 획득: User ID: {}, Transaction Type: {}", userId, transactionType);
            return transaction.execute(userId, amount, timestamp);
        } finally {
            lock.unlock();
            logger.info("Lock 해제: User ID: {}, Transaction Type: {}", userId, transactionType);
        }
    }

    @Deprecated
    /**
     * 해당 메서드는 유저의 포인트를 충전하는 로직을 구현합니다.
     * - 현재는 @Deprecated로 대체 작업이 진행 중이며, executePointTransaction 메서드를 사용하는 것을 권장합니다.
     *
     * @deprecated 이 메서드는 실행 타입별 공통 로직을 가진 executePointTransaction 메서드로 대체되었습니다.
     * @param id 유저 ID
     * @param amount 충전 금액
     * @param currentTimeMillis 트랜잭션 발생 시간
     * @return 업데이트된 유저 포인트
     */
    public UserPoint charge(
            final long id,
            final long amount,
            final long currentTimeMillis
    ) {
        UserPoint userPoint = userPointTable.selectById(id);

        long toBeUpdatedUserPoint = userPoint.charge(amount);
        logger.info("[{}.charge] : 충전 후 포인트: {}", getClass().getSimpleName(), toBeUpdatedUserPoint);

        UserPoint savedUserPoint = userPointTable.insertOrUpdate(id, toBeUpdatedUserPoint);

        PointHistory insertHistory = pointHistoryTable.insert(
                id,
                amount,
                TransactionType.CHARGE,
                currentTimeMillis
        );

        logger.info("[{}.charge] : 충전 완료. User ID: {}, 충전 금액: {}, 업데이트된 포인트: {}",
                getClass().getSimpleName(), id, amount, savedUserPoint);
        logger.info("[{}.charge] : 포인트 이력 저장. User ID: {}, Amount: {}, TransactionType: {}, History: {}",
                getClass().getSimpleName(), id, amount, TransactionType.CHARGE, insertHistory);

        return savedUserPoint;
    }

    @Deprecated
    /**
     * 해당 메서드는 유저의 포인트를 사용하는 로직을 구현합니다.
     * - 현재는 @Deprecated로 대체 작업이 진행 중이며, executePointTransaction 메서드를 사용하는 것을 권장합니다.
     *
     * @deprecated 이 메서드는 실행 타입별 공통 로직을 가진 executePointTransaction 메서드로 대체되었습니다.
     * @param userId 유저 ID
     * @param amount 사용 포인트
     * @param currTimeMillis 트랜잭션 발생 시간
     * @return 업데이트된 유저 포인트
     */
    public UserPoint use(
            final long userId,
            final long amount,
            final long currTimeMillis
    ) {
        UserPoint userPoint = userPointTable.selectById(userId);

        long usePoint = userPoint.use(amount);
        logger.info("[{}.use] : 사용된 포인트: {}", getClass().getSimpleName(), amount);

        UserPoint rtn = userPointTable.insertOrUpdate(userId, usePoint);

        pointHistoryTable.insert(userId, amount, TransactionType.USE, currTimeMillis);

        return rtn;
    }
}
