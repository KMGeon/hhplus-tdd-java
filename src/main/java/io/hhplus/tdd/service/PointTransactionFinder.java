package io.hhplus.tdd.service;

import io.hhplus.tdd.service.transaction.PointTransaction;
import io.hhplus.tdd.service.transaction.TransactionType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PointTransactionFinder {

    private final List<PointTransaction> transactionServices;

    public PointTransactionFinder(List<PointTransaction> transactionServices) {
        this.transactionServices = transactionServices;
    }

    public PointTransaction findTransactionService(TransactionType type) {
        return transactionServices.stream()
                .filter(service -> service.getTransactionType() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 트랜잭션 타입: " + type));
    }
}