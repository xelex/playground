package playground.test.revo.data.impl;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class InMemoryAccountDAOTest {

    private InMemoryAccountDAO dao;
    private String TEST_1 = "test 1";
    private String TEST_2 = "test 2";
    private double DELTA = 0.0001;

    @Before
    public void before() {
        dao = new InMemoryAccountDAO();
    }

    @Test(expected = IllegalArgumentException.class)
    public void reduce_Throw_1() {
        dao.reduce(TEST_1, BigDecimal.valueOf(1.0));
    }

    @Test
    public void reduce_Ok() {
        dao.createAndDeposit(TEST_1, BigDecimal.valueOf(1.0));
        assertEquals(0.0, dao.reduce(TEST_1, BigDecimal.valueOf(1.0)).doubleValue(), DELTA);
        assertEquals(null, dao.reduce(TEST_1, BigDecimal.valueOf(1.0)));

        dao.deposit(TEST_1, BigDecimal.valueOf(2.0));
        assertEquals(1.0, dao.reduce(TEST_1, BigDecimal.valueOf(1.0)).doubleValue(), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void reduce_Throw_2() {
        dao.createAndDeposit(TEST_1, BigDecimal.valueOf(1.0));
        dao.reduce(TEST_1, BigDecimal.valueOf(-1.0));
    }

    @Test
    public void deposit() {
        assertEquals(0, dao.allAccounts().size());
        assertEquals(null, dao.deposit(TEST_1, BigDecimal.valueOf(1.0)));
        dao.createAndDeposit(TEST_1, BigDecimal.valueOf(1.0));
        assertEquals(1, dao.allAccounts().size());
        assertEquals(2.0, dao.deposit(TEST_1, BigDecimal.valueOf(1.0)).doubleValue(), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void deposit_throw() {
        dao.createAndDeposit(TEST_1, BigDecimal.valueOf(1.0));
        dao.deposit(TEST_1, BigDecimal.valueOf(-1.0));
    }

    @Test
    public void createAndDeposit() {
        assertEquals(0, dao.allAccounts().size());
        dao.createAndDeposit(TEST_1, BigDecimal.valueOf(1.0));
        assertEquals(1, dao.allAccounts().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createAndDeposit_throw() {
        dao.createAndDeposit(TEST_1, BigDecimal.valueOf(-1.0));
    }

    @Test
    public void allAccounts() {
        assertEquals(0, dao.allAccounts().size());

        dao.createAndDeposit(TEST_1, BigDecimal.valueOf(1.0));
        assertEquals(1, dao.allAccounts().size());

        dao.createAndDeposit(TEST_1, BigDecimal.valueOf(1.0));
        assertEquals(1, dao.allAccounts().size());

        dao.createAndDeposit(new String(TEST_1), BigDecimal.valueOf(1.0));
        assertEquals(1, dao.allAccounts().size());

        dao.createAndDeposit(TEST_2, BigDecimal.valueOf(1.0));
        assertEquals(2, dao.allAccounts().size());
    }

    @Test
    public void balance() {
        assertEquals(null, dao.balance(TEST_1));

        dao.createAndDeposit(TEST_1, BigDecimal.valueOf(1.0));
        assertEquals(1.0, dao.balance(TEST_1).doubleValue(), DELTA);

        dao.createAndDeposit(TEST_1, BigDecimal.valueOf(2.0));
        assertEquals(3.0, dao.balance(TEST_1).doubleValue(), DELTA);
    }
}