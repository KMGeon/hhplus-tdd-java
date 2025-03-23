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

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    private static final Logger logger = LoggerFactory.getLogger(PointService.class);

    public UserPoint point(
            final long userId
    ) {
        UserPoint userPoint = userPointTable.selectById(userId);

        boolean isNewUser = userPoint.isNewUser(userPoint);
        logger.info("[{}.point] :isNewUser : {}", getClass().getSimpleName(), isNewUser);

        /**
         * DB에서 getOrDefault로 empty를 반환하는데 여기서 또 써야할까??
         * getOreDefault가 작은 비즈니스 로직이라고 생각하는데 DB에 비즈니스 로직이 숨겨져 있다.
         * 서비스 Layer에서 중복적으로 Empty를 반환하는 중복코드가 생성되면 비즈니스 로직은 명확하게 드러나지만
         * Repository의 코드의 변경이 불가능하기 때문에 중복적인 로직을  처리한다.
         */
        if (isNewUser) return UserPoint.empty(userId);
        return userPoint;
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

    // todo : 테스트 코드 작성
    public UserPoint use(
            final long id,
            final long amount,
            final long currTimeMillis
    ) {

        UserPoint userPoint = userPointTable.selectById(id);
        long currentUserId = userPoint.id();

        UserPoint validUse = userPoint.isValidUse(amount, currTimeMillis);
        long remainPoint = validUse.point();

        UserPoint rtn = userPointTable.insertOrUpdate(currentUserId, remainPoint);
        pointHistoryTable.insert(currentUserId, amount, TransactionType.USE, currTimeMillis);

        return rtn;
    }
}
