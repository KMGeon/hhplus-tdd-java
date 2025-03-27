package io.hhplus.tdd.service;


import io.hhplus.tdd.domain.PointHistory;
import io.hhplus.tdd.domain.UserPoint;
import io.hhplus.tdd.repository.PointHistoryTable;
import io.hhplus.tdd.repository.UserPointTable;
import io.hhplus.tdd.service.transaction.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@DirtiesContext(
        classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD
)
public class ConcurrencyPointServiceIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrencyPointServiceIntegrationTest.class);
    private static final String START_LOG_MSG = "모든 스레드 준비 완료. 🏃‍♂️🏃‍♀️🏃‍♂️🏃‍♀️🏃‍";

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointTable userPointTable;
    @Autowired
    private PointHistoryTable pointHistoryTable;

    @Nested
    @DisplayName("동일 사용자에 대한 동시 요청에 대한 시나리오")
    class OnePersonConcurrencyTest {

        @Test
        @DisplayName("동일 사용자에 대한 동시 요청은 순차적으로 처리되어야 한다")
        void ten_person_charge_100L_expect_total_1000_ok() throws InterruptedException {
            // given
            long userId = 1L;
            long amount = 100L;
            TransactionType transactionType = TransactionType.CHARGE;
            long timestamp = System.currentTimeMillis();
            int threadCount = 10;

            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(threadCount);

            // when
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

            IntStream.range(0, threadCount).forEach(i -> {
                executorService.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();

                        pointService.executePointTransaction(
                                userId, amount, transactionType, timestamp);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            });

            readyLatch.await();
            logger.info(START_LOG_MSG);

            startLatch.countDown();

            completionLatch.await();
            executorService.shutdown();

            // then
            UserPoint userPoint = userPointTable.selectById(userId);
            List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userId);

            assertEquals(1000L, userPoint.point(), "1번 유저가 100원을 10번 충전해서 1,000원");
            assertEquals(10, pointHistories.size(), "10번 충전해서 10개");
        }

        @Test
        @DisplayName("동일 사용자에 대한 충전 후 다중 사용 요청은 순차적으로 처리되어야 한다")
        void init_1000_use_ten_50L_expect_500_ok() throws InterruptedException {
            // given
            long userId = 1L;
            long initialCharge = 1000L;
            long useAmount = 50L;
            int useCount = 10;

            pointService.executePointTransaction(userId, initialCharge, TransactionType.CHARGE, System.currentTimeMillis());

            CountDownLatch readyLatch = new CountDownLatch(useCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(useCount);

            ExecutorService executorService = Executors.newFixedThreadPool(useCount);
            IntStream.range(0, useCount).forEach(i -> {
                executorService.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();
                        pointService.executePointTransaction(userId, useAmount, TransactionType.USE, System.currentTimeMillis());
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            });

            readyLatch.await();
            logger.info(START_LOG_MSG);
            startLatch.countDown();
            completionLatch.await();
            executorService.shutdown();

            // then
            UserPoint userPoint = userPointTable.selectById(userId);
            List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userId);
            assertEquals(500L, userPoint.point(), "초기 충전 1000원 - 사용 50원 * 10번 = 500원");
            assertEquals(11, pointHistories.size(), "초기 충전 1번 + 사용 10번 = 11개");
        }

        @Test
        @DisplayName("동일 사용자에 대한 50 동시 요청이 순차적으로 처리되어야 한다")
        void single_user_10000_requests_expect_correct_total() throws InterruptedException {
            // given
            long userId = 1L;
            long amount = 100L;
            TransactionType transactionType = TransactionType.CHARGE;
            int threadCount = 50;

            CountDownLatch readyLatch = new CountDownLatch(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completionLatch = new CountDownLatch(threadCount);

            ExecutorService executorService = Executors.newFixedThreadPool(100);

            IntStream.range(0, threadCount).forEach(i -> {
                executorService.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();

                        pointService.executePointTransaction(
                                userId, amount, transactionType, System.currentTimeMillis());
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            });

            readyLatch.await();
            logger.info(START_LOG_MSG);

            startLatch.countDown();
            completionLatch.await();
            executorService.shutdown();

            // then
            UserPoint userPoint = userPointTable.selectById(userId);
            List<PointHistory> pointHistories = pointHistoryTable.selectAllByUserId(userId);

            assertEquals(5000L, userPoint.point());
            assertEquals(50, pointHistories.size());
        }
    }
}
