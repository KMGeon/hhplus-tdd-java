package io.hhplus.tdd.repository;

import io.hhplus.tdd.domain.UserPoint;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 해당 Table 클래스는 변경하지 않고 공개된 API 만을 사용해 데이터를 제어합니다.
 */
@Component
public class UserPointTable {

    private final Map<Long, UserPoint> table = new HashMap<>();

    public UserPoint selectById(Long id) {
        throttle(200);
        return table.getOrDefault(id, UserPoint.empty(id));
    }

    public UserPoint insertOrUpdate(long id, long amount) {
        throttle(300);
        UserPoint userPoint = new UserPoint(id, amount, System.currentTimeMillis());
        table.put(id, userPoint);
        return userPoint;
    }

    private void throttle(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep((long) (Math.random() * millis));
        } catch (InterruptedException ignored) {

        }
    }
}

/**
 * 동시성이란 언제 발생
 * 같은 자원에 공유할 때
 * 동시성 이슈가 발생하는지 테스트하려면
 * 적어도 공유자원과 접근자는 진짜 객체여야 한다.
 * ===> 통합테스트로 검증해야된다.
 *
 * 동시성 이슈가 발생하는지에 대한 테스트는 행동의 경계 객체에서 테스트 한다.
 *
 */