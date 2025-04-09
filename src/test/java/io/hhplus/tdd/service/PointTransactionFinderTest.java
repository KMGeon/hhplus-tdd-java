package io.hhplus.tdd.service;

import io.hhplus.tdd.service.transaction.PointTransaction;
import io.hhplus.tdd.service.transaction.TransactionType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PointTransactionFinderTest {

    @Autowired
    private PointTransactionFinder pointTransactionFinder;

    @Test
    @DisplayName("USE 트랜잭션 타입에 맞는 컴포넌트를 찾을 수 있다")
    public void find_type_use() throws Exception{
        // given

        // when
        PointTransaction transactionService = pointTransactionFinder.findTransactionService(TransactionType.USE);

        // then
        TransactionType transactionType = transactionService.getTransactionType();
        assertEquals(TransactionType.USE, transactionType);
    }

    @Test
    @DisplayName("CHARGE 트랜잭션 타입에 맞는 컴포넌트를 찾을 수 있다")
    public void find_type_charge() throws Exception{
        // given

        // when
        PointTransaction transactionService = pointTransactionFinder.findTransactionService(TransactionType.CHARGE);

        // then
        TransactionType transactionType = transactionService.getTransactionType();
        assertEquals(TransactionType.CHARGE, transactionType);
    }

    @Test
    @DisplayName("지원하지 않는 트랜잭션 타입은 예외가 발생한다")
    public void find_noType_throwError() throws Exception{
        // given
        TransactionType nonExistingType = null;

        // when
        // then
        Assertions.assertThatThrownBy(() -> pointTransactionFinder.findTransactionService(nonExistingType))
                        .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 트랜잭션 타입");
    }

}