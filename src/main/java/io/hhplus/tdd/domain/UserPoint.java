package io.hhplus.tdd.domain;


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
        if (isZeroPointUse(requiredPoint)) return this;

        validateForUse(requiredPoint);

        long remainingPoint = this.point - requiredPoint;
        return new UserPoint(this.id, remainingPoint, currentTimeMiles);
    }

    private void validateForUse(long requiredPoint) {
        if (isNewUser()) {
            throw new RuntimeException("존재하지 않는 회원입니다.");
        }

        if (!isEnoughPoint(requiredPoint)) {
            throw new RuntimeException("포인트가 부족합니다.");
        }
    }

    private boolean isEnoughPoint(long requiredPoint) {
        return this.point >= requiredPoint;
    }

    private boolean isNewUser() {
        return updateMillis == getCurrentTimeMillis();
    }

    private boolean isZeroPointUse(long requiredPoint) {
        return this.point == requiredPoint;
    }

    private long getCurrentTimeMillis() {
        if (timeProvider == null) throw new IllegalStateException("TimeProvider가 설정되지 않았습니다.");

        return timeProvider.getConsistentTimeMillis();
    }
}
