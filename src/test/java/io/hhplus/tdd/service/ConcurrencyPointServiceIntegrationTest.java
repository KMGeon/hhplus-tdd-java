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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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


    @Test
    @DisplayName("동일 사용자가 동시에 충전과 사용을 실행할 경우 순차적으로 처리되어야 한다")
    void concurrent_charge_and_use_expect_correct_total() throws InterruptedException {
        // given
        long userId = 1L;
        long initialCharge = 1000L;
        long chargeAmount = 100L;
        long useAmount = 50L;
        int chargeCount = 25;
        int useCount = 25;
        int totalThreads = chargeCount + useCount;

        // 초기 1000원 충전
        pointService.executePointTransaction(userId, initialCharge, TransactionType.CHARGE, System.currentTimeMillis());

        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(totalThreads);

        ExecutorService executorService = Executors.newFixedThreadPool(50);

        IntStream.range(0, chargeCount).forEach(i -> {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    pointService.executePointTransaction(userId, chargeAmount, TransactionType.CHARGE, System.currentTimeMillis());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    completionLatch.countDown();
                }
            });
        });

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

        assertEquals(2250L, userPoint.point(), "초기 1000원 + (100원 충전 * 25) - (50원 사용 * 25) = 2250원");
        assertEquals(51, pointHistories.size(), "초기 충전 1회 + 50번 트랜잭션 = 51개");
    }

    @Test
    @DisplayName("포인트 한도 초과 시 예외가 발생해야 한다")
    void charge_exceed_limit_throw_exception() throws InterruptedException {
        // given
        long userId = 1L;
        long amount = 2000L;  // 2000포인트씩 충전
        TransactionType transactionType = TransactionType.CHARGE;
        long timestamp = System.currentTimeMillis();
        int threadCount = 10;

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);

        // when
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        IntStream.range(0, threadCount).forEach(i -> {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    pointService.executePointTransaction(
                            userId, amount, transactionType, timestamp);
                } catch (IllegalArgumentException e) {
                    exceptionCount.incrementAndGet();
                    assertEquals("충전 한도 초과. 현재 포인트: 10000, 충전 시도량: 2000, 한도: 10000", e.getMessage());
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

        assertEquals(10000L, userPoint.point(), "포인트는 한도인 10000을 초과하지 않아야 함");
        assertTrue(exceptionCount.get() > 0, "한도 초과 예외가 발생해야 함");
    }

    @Test
    @DisplayName("음수 포인트 충전 시도 시 예외가 발생해야 한다")
    void charge_negative_amount_throw_exception() throws InterruptedException {
        // given
        long userId = 1L;
        long amount = -100L;  // 음수 포인트 충전 시도
        TransactionType transactionType = TransactionType.CHARGE;
        long timestamp = System.currentTimeMillis();
        int threadCount = 10;

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);

        // when
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        IntStream.range(0, threadCount).forEach(i -> {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    pointService.executePointTransaction(
                            userId, amount, transactionType, timestamp);
                } catch (IllegalArgumentException e) {
                    exceptionCount.incrementAndGet();
                    assertEquals("충전 포인트는 양수여야 합니다. 충전 시도량: -100", e.getMessage());
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
        assertEquals(0L, userPoint.point(), "음수 충전으로 인해 포인트가 변경되지 않아야 함");
        assertTrue(exceptionCount.get() > 0, "음수 충전 예외가 발생해야 함");
    }

    @Test
    @DisplayName("포인트 부족 시 사용 시도 시 예외가 발생해야 한다")
    void use_insufficient_point_throw_exception() throws InterruptedException {
        // given
        long userId = 1L;
        long chargeAmount = 100L;  // 충전 금액
        long useAmount = 1000L;    // 사용 금액 (충전 금액보다 큼)
        long timestamp = System.currentTimeMillis();
        int threadCount = 10;

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);

        // when
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        // 먼저 포인트 충전
        pointService.executePointTransaction(userId, chargeAmount, TransactionType.CHARGE, timestamp);

        IntStream.range(0, threadCount).forEach(i -> {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    pointService.executePointTransaction(
                            userId, useAmount, TransactionType.USE, timestamp);
                } catch (RuntimeException e) {
                    exceptionCount.incrementAndGet();
                    assertEquals("포인트가 부족합니다.", e.getMessage());
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
        assertEquals(chargeAmount, userPoint.point(), "포인트 부족으로 인해 포인트가 변경되지 않아야 함");
        assertTrue(exceptionCount.get() > 0, "포인트 부족 예외가 발생해야 함");
    }
}
