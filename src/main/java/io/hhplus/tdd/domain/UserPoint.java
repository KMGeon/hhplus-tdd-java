package io.hhplus.tdd.domain;


import io.hhplus.tdd.common.exception.NotFoundUserException;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {
    private static TimeProvider timeProvider;

    public static TimeProvider setTimeProvider(TimeProvider provider) {
        return timeProvider = provider;
    }

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, timeProvider.getConsistentTimeMillis());
    }

    public static boolean isNewUser(UserPoint userPoint) {
        return userPoint.updateMillis() == timeProvider.getConsistentTimeMillis();
    }


    public UserPoint isValidUse(
            long requiredPoint,
            long currentTimeMiles
    ) {
        validateForUse(requiredPoint);

        long remainingPoint = this.point - requiredPoint;
        return new UserPoint(this.id, remainingPoint, currentTimeMiles);
    }

    private void validateForUse(long requiredPoint) {
        if (isNewUser()) throw new NotFoundUserException();


        if (!isEnoughPoint(requiredPoint)) {
            throw new RuntimeException("포인트가 부족합니다.");
        }
    }

    private boolean isEnoughPoint(long requiredPoint) {
        return this.point >= requiredPoint;
    }

    public boolean isNewUser() {
        return updateMillis == getCurrentTimeMillis();
    }

    public boolean isZeroPointUse(long requiredPoint) {
        return requiredPoint == 0L;
    }

    public long getCurrentTimeMillis() {
        if (timeProvider == null) throw new IllegalStateException("TimeProvider가 설정되지 않았습니다.");

        return timeProvider.getConsistentTimeMillis();
    }
}
