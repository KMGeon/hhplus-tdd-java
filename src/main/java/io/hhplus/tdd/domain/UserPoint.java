package io.hhplus.tdd.domain;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    private static final Long LIMIT_POINT = 10000L;

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }


    public long charge(long amount) {
        if (!canCharge(amount)) {
            if (amount < 0) {
                throw new IllegalArgumentException(
                        String.format("충전 포인트는 양수여야 합니다. 충전 시도량: %d", amount)
                );
            } else {
                throw new IllegalArgumentException(
                        String.format("충전 한도 초과. 현재 포인트: %d, 충전 시도량: %d, 한도: %d",
                                this.point, amount, LIMIT_POINT)
                );
            }
        }
        return  this.point + amount;
    }

    public long use(long requiredPoint) {
        if (!isEnoughPoint(requiredPoint)) {
            throw new RuntimeException("포인트가 부족합니다.");
        }
        return this.point - requiredPoint;
    }

    private boolean canCharge(long amount) {
        return amount > 0 && this.point + amount <= LIMIT_POINT;
    }

    private boolean isEnoughPoint(long requiredPoint) {
        return this.point >= requiredPoint;
    }
}