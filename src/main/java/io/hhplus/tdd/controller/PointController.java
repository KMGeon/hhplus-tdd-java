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

    @GetMapping("{id}")
    public UserPoint point(
            @PathVariable long id
    ) {
        logger.info("====== /point/{id} [{}.point()] start ======", getClass().getSimpleName());

        logger.info("[{}] ======  /point/{id} [userId : {}]", getClass().getSimpleName(), id);
        UserPoint rtn = pointService.point(id);

        logger.info("====== /point/{id} [{}.point()] end ======", getClass().getSimpleName());
        return rtn;
    }

    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable long id
    ) {
        logger.info("====== /{id}/histories [{}.history()] start ======", getClass().getSimpleName());

        logger.info("[{}] ======  /{id}/histories [userId : {}]", getClass().getSimpleName(), id);
        List<PointHistory> rtn  = pointService.history(id);
        logger.info("====== /{id}/histories [{}.history()] start ======", getClass().getSimpleName());
        return rtn;
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

    private long getCurrTimeMillis() {
        return timeProvider.currentTimeMillis();
    }

}
