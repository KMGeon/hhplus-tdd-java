package io.hhplus.tdd;

import io.hhplus.tdd.domain.UserPoint;
import io.hhplus.tdd.repository.PointHistoryTable;
import io.hhplus.tdd.repository.UserPointTable;
import io.hhplus.tdd.service.PointService;
import io.hhplus.tdd.domain.TimeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ContextConfiguration {
    @Autowired
    protected PointService pointService;

    @Autowired
    protected TimeProvider timeProvider;

    @Autowired
    protected UserPointTable userPointTable;

    @Autowired
    protected PointHistoryTable pointHistoryTable;

    protected static final Long NEW_USER_ID = 1L;
    protected static final Long EXIST_USER_ID = 99L;
    protected static final Long DEFAULT_POINT = 1000L;


    protected long currentTimeMillis() {
        return timeProvider.getConsistentTimeMillis();
    }

    /**
     * todo
     * table을 접근하면 안되서 일단은 thread sleep으로 처리함
     * 이후에 mocking으로 currentTime 모킹 가능한지 알아보기
     */
    protected UserPoint setUpExistUserPoint(long userId) throws InterruptedException {
        int luckNumber = 7;
        UserPoint userPoint = userPointTable.insertOrUpdate(userId, 1000L);
        Thread.sleep(luckNumber);
        return userPoint;
    }
}
