package playground.test.revo.data.impl;

import org.junit.Before;
import org.junit.Test;
import playground.test.revo.data.PrivateAccountDAO;
import playground.test.revo.data.PublicAccountDAO;
import playground.test.revo.data.TxManager;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static playground.test.revo.data.TxManager.DepositResult.*;
import static playground.test.revo.data.TxManager.TxResult.ERROR_FROM_NOT_FOUND;
import static playground.test.revo.data.TxManager.TxResult.ERROR_TO_NOT_FOUND;

public class SimpleTxManagerTest {

    PublicAccountDAO dao;
    SimpleTxManager tx;
    private String TEST_1 = "test 1";
    private String TEST_2 = "test 2";
    private String TEST_3 = "test 3";

    private AtomicBoolean throwException;

    @Before
    public void before() {
        InMemoryAccountDAO inMemoryAccountDAO = new InMemoryAccountDAO();
        this.dao = inMemoryAccountDAO;
        this.throwException = new AtomicBoolean(false);
        this.tx = new SimpleTxManager(new PrivateAccountDAO() {
            @Override
            public BigDecimal reduce(String id, BigDecimal amount) {
                doThrow();
                return inMemoryAccountDAO.reduce(id, amount);
            }

            @Override
            public BigDecimal deposit(String id, BigDecimal amount) {
                doThrow();
                return inMemoryAccountDAO.deposit(id, amount);
            }

            @Override
            public BigDecimal createAndDeposit(String id, BigDecimal amount) {
                doThrow();
                return inMemoryAccountDAO.createAndDeposit(id, amount);
            }

            @Override
            public boolean exists(String id) {
                return inMemoryAccountDAO.exists(id);
            }

            private void doThrow() {
                if (throwException.get()) {
                    throw new RuntimeException("test");
                }
            }
        });
    }

    @Test
    public void deposit() {
        assertEquals(ERROR_NOT_FOUND, tx.deposit(null, BigDecimal.valueOf(1.0)));
        assertEquals(ERROR_NOT_FOUND, tx.deposit("", BigDecimal.valueOf(1.0)));
        assertEquals(ERROR_INCORRECT_AMOUNT, tx.deposit(TEST_1, null));
        assertEquals(ERROR_INCORRECT_AMOUNT, tx.deposit(TEST_1, BigDecimal.valueOf(-1.0)));

        assertEquals(SUCCESS, tx.deposit(TEST_1, BigDecimal.valueOf(1.0)));

        throwException.set(true);
        assertEquals(ERROR_UNKNOWN, tx.deposit(TEST_1, BigDecimal.valueOf(1.0)));
    }

    @Test
    public void transfer() {
        tx.deposit(TEST_1, BigDecimal.valueOf(2.0));
        tx.deposit(TEST_2, BigDecimal.valueOf(2.0));

        assertEquals(ERROR_FROM_NOT_FOUND, tx.transfer(null, TEST_2, BigDecimal.valueOf(-1.0)));
        assertEquals(ERROR_TO_NOT_FOUND, tx.transfer(TEST_1, null, BigDecimal.valueOf(-1.0)));

        assertEquals(ERROR_FROM_NOT_FOUND, tx.transfer("", TEST_2, BigDecimal.valueOf(-1.0)));
        assertEquals(ERROR_TO_NOT_FOUND, tx.transfer(TEST_1, "", BigDecimal.valueOf(-1.0)));

        assertEquals(ERROR_FROM_NOT_FOUND, tx.transfer(TEST_3, TEST_2, BigDecimal.valueOf(-1.0)));
        assertEquals(ERROR_TO_NOT_FOUND, tx.transfer(TEST_1, TEST_3, BigDecimal.valueOf(-1.0)));

        assertEquals(TxManager.TxResult.ERROR_SAME_FROM_TO, tx.transfer(TEST_1, TEST_1, BigDecimal.valueOf(1.0)));
        assertEquals(TxManager.TxResult.ERROR_SAME_FROM_TO, tx.transfer(TEST_2, TEST_2, BigDecimal.valueOf(1.0)));

        assertEquals(TxManager.TxResult.ERROR_INCORRECT_AMOUNT, tx.transfer(TEST_1, TEST_2, null));
        assertEquals(TxManager.TxResult.ERROR_INCORRECT_AMOUNT, tx.transfer(TEST_1, TEST_2, BigDecimal.valueOf(-1.0)));

        assertEquals(TxManager.TxResult.ERROR_INSUFFICIENT_AMOUNT, tx.transfer(TEST_1, TEST_2, BigDecimal.valueOf(100.0)));
        assertEquals(TxManager.TxResult.ERROR_INSUFFICIENT_AMOUNT, tx.transfer(TEST_2, TEST_1, BigDecimal.valueOf(100.0)));


        assertEquals(TxManager.TxResult.SUCCESS, tx.transfer(TEST_1, TEST_2, BigDecimal.valueOf(1.0)));
        assertEquals(TxManager.TxResult.SUCCESS, tx.transfer(TEST_2, TEST_1, BigDecimal.valueOf(1.0)));

        throwException.set(true);
        assertEquals(TxManager.TxResult.ERROR_UNKNOWN, tx.transfer(TEST_1, TEST_2, BigDecimal.valueOf(1.0)));
        assertEquals(TxManager.TxResult.ERROR_UNKNOWN, tx.transfer(TEST_2, TEST_1, BigDecimal.valueOf(1.0)));
    }
}