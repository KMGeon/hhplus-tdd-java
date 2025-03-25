package io.hhplus.tdd.service;

import io.hhplus.tdd.repository.PointHistoryTable;
import io.hhplus.tdd.repository.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {
    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;


}
