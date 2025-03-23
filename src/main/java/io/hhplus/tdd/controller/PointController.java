package io.hhplus.tdd.controller;

import io.hhplus.tdd.domain.PointHistory;
import io.hhplus.tdd.domain.TimeProvider;
import io.hhplus.tdd.domain.UserPoint;
import io.hhplus.tdd.service.PointService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/point")
@RequiredArgsConstructor
public class PointController {

    private static final Logger logger = LoggerFactory.getLogger(PointController.class);

    private final PointService pointService;
    private final TimeProvider timeProvider;

    private long getCurrTimeMillis() {
        return timeProvider.currentTimeMillis();
    }


    @GetMapping("{id}")
    public UserPoint point(
            @PathVariable long id
    ) {
        logger.info("====== /point/{id} [{}.point()] start ======", getClass().getSimpleName());

        logger.info("[{}] ======  /point/{id} [userId : {}]", getClass().getSimpleName(), id);
        UserPoint rtn = pointService.getUserPointWithDefault(id);
        logger.info("====== /point/{id} [{}.point()] end ======", getClass().getSimpleName());
        return rtn;
    }

    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable long id
    ) {
        return List.of();
    }


    @PatchMapping("{id}/charge")
    public UserPoint charge(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        logger.info("====== /{id}/point [{}.charge()] start ======", getClass().getSimpleName());
        logger.info("[{}] ======  /{id}/point [userId : {}]", getClass().getSimpleName(), id);
        logger.info("[{}] ======  /{id}/point [amount : {}]", getClass().getSimpleName(), amount);
        long currTimeMillis = getCurrTimeMillis();
        UserPoint rtn = pointService.charge(id, amount, currTimeMillis);
        logger.info("[{}] ======  /{id}/point [rtn : {}]", getClass().getSimpleName(), rtn);
        logger.info("====== /{id}/point [{}.charge()] end ======", getClass().getSimpleName());
        return rtn;
    }

    @PatchMapping("{id}/use")
    public UserPoint use(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        logger.info("====== /{id}/use [{}.use()] start ======", getClass().getSimpleName());

        long currTimeMillis = getCurrTimeMillis();
        logger.info("[{}] ======  /{id}/use [currTimeMillis : {}]", getClass().getSimpleName(), currTimeMillis);

        UserPoint rtn = pointService.use(id,amount, currTimeMillis);
        logger.info("[{}] ======  /{id}/use [rtn : {}]", getClass().getSimpleName(), rtn);
        logger.info("====== /{id}/use [{}.use()] end ======", getClass().getSimpleName());
        return rtn;
    }
}
