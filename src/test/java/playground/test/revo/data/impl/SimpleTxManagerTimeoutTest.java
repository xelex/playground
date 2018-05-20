package playground.test.revo.data.impl;

import org.junit.Before;
import org.junit.Test;
import playground.test.revo.data.TxManager;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertEquals;

public class SimpleTxManagerTimeoutTest {

    private final TestRunnable noOp = () -> {
    };
    SimpleTxManager tx;
    private String TEST_1 = "test 1";
    private String TEST_2 = "test 2";

    private volatile boolean failed = false;

    private ThreadPoolExecutor executor;

    private void initExecutor() {
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
    }

    @Before
    public void before() {
        InMemoryAccountDAO inMemoryAccountDAO = new InMemoryAccountDAO();
        this.tx = new SimpleTxManager(inMemoryAccountDAO, 0L); // No waiting, fail fast
        this.failed = false;

        this.tx.deposit(TEST_1, BigDecimal.valueOf(100.0));
        this.tx.deposit(TEST_2, BigDecimal.valueOf(100.0));

        initExecutor();
    }

    private interface TestRunnable {
        void run() throws InterruptedException;
    }

    private void runAsync(String name, TestRunnable task, TxManager.DepositResult expected) {
        executor.submit(() -> {
            TxManager.DepositResult got = tx.runTx(name, () -> {
                try {
                    task.run();
                } catch (InterruptedException e) {
                    throw new RuntimeException("InterruptedException", e);
                }
                return TxManager.DepositResult.SUCCESS;
            });

            if (!got.equals(expected)) {
                System.out.println("Got " + got + ", expected=" + expected);
                failed = true;
            }
        });
    }

    private void runAsync(String from, String to, TestRunnable task, TxManager.TxResult expected) {
        executor.submit(() -> {
            TxManager.TxResult got = tx.runTx(from, to, () -> {
                try {
                    task.run();
                } catch (InterruptedException e) {
                    throw new RuntimeException("InterruptedException", e);
                }
                return TxManager.TxResult.SUCCESS;
            });

            if (!got.equals(expected)) {
                System.out.println("Got " + got + ", expected=" + expected);
                failed = true;
            }
        });
    }

    @Test
    public void testDeposit_1() throws InterruptedException {
        runAsync(TEST_1, noOp, TxManager.DepositResult.SUCCESS);

        executor.shutdown();

        assertEquals(true, executor.awaitTermination(1, TimeUnit.SECONDS));
        assertEquals(false, failed);
    }

    @Test
    public void testDeposit_2() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ReentrantLock lock = new ReentrantLock();

        lock.lock();

        runAsync(TEST_1, () -> {
            latch.countDown();
            latch.await();
            lock.lock();
        }, TxManager.DepositResult.SUCCESS);  // OK

        latch.await();
        runAsync(TEST_1, latch::await, TxManager.DepositResult.ERROR_TIMEOUT);

        executor.shutdown();
        while (executor.getActiveCount() != 1) {
            assertEquals(false, executor.awaitTermination(1, TimeUnit.MILLISECONDS));
        }
        lock.unlock();

        assertEquals(true, executor.awaitTermination(1, TimeUnit.SECONDS));
        assertEquals(false, failed);
    }

    @Test
    public void testDeposit_3() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        runAsync(TEST_1, latch::await, TxManager.DepositResult.SUCCESS); // OK
        runAsync(TEST_2, latch::await, TxManager.DepositResult.SUCCESS); // OK, because we are locking TEST_2

        executor.shutdown();
        assertEquals(false, executor.awaitTermination(1, TimeUnit.MILLISECONDS));
        latch.countDown();

        assertEquals(true, executor.awaitTermination(1, TimeUnit.SECONDS));
        assertEquals(false, failed);
    }

    @Test
    public void testDeposit_ExceptionForcesUnlock() throws InterruptedException {
        runAsync(TEST_1, () -> {
            throw new RuntimeException();
        }, TxManager.DepositResult.ERROR_UNKNOWN);  // exception

        executor.shutdown();
        assertEquals(true, executor.awaitTermination(1, TimeUnit.SECONDS));

        initExecutor();

        runAsync(TEST_1, noOp, TxManager.DepositResult.SUCCESS); //OK
        runAsync(TEST_2, noOp, TxManager.DepositResult.SUCCESS); //OK

        executor.shutdown();

        assertEquals(true, executor.awaitTermination(1, TimeUnit.SECONDS));
        assertEquals(false, failed);
    }

    @Test
    public void testTransfer_1() throws InterruptedException {
        runAsync(TEST_1, TEST_2, noOp, TxManager.TxResult.SUCCESS);

        executor.shutdown();

        assertEquals(true, executor.awaitTermination(1, TimeUnit.SECONDS));
        assertEquals(false, failed);
    }

    @Test
    public void testTransfer_2() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ReentrantLock lock = new ReentrantLock();

        lock.lock();

        runAsync(TEST_1, TEST_2, () -> {
            latch.countDown();
            latch.await();
            lock.lock();

        }, TxManager.TxResult.SUCCESS);
        latch.await();

        //both TEST_1 and TEST_2 are locked

        runAsync(TEST_1, noOp, TxManager.DepositResult.ERROR_TIMEOUT);
        runAsync(TEST_2, noOp, TxManager.DepositResult.ERROR_TIMEOUT);
        runAsync(TEST_1, TEST_2, noOp, TxManager.TxResult.ERROR_TIMEOUT);
        runAsync(TEST_2, TEST_1, noOp, TxManager.TxResult.ERROR_TIMEOUT);

        executor.shutdown();
        while (executor.getActiveCount() != 1) {
            assertEquals(false, executor.awaitTermination(1, TimeUnit.MILLISECONDS));
        }
        lock.unlock();

        assertEquals(true, executor.awaitTermination(1, TimeUnit.SECONDS));
        assertEquals(false, failed);
    }

    @Test
    public void testTransfer_ExceptionForcesUnlock() throws InterruptedException {
        runAsync(TEST_1, TEST_2, () -> {
            throw new RuntimeException();
        }, TxManager.TxResult.ERROR_UNKNOWN);  // exception

        executor.shutdown();
        assertEquals(true, executor.awaitTermination(1, TimeUnit.SECONDS));

        initExecutor();

        runAsync(TEST_1, noOp, TxManager.DepositResult.SUCCESS); //OK
        runAsync(TEST_2, noOp, TxManager.DepositResult.SUCCESS); //OK

        executor.shutdown();

        assertEquals(true, executor.awaitTermination(1, TimeUnit.SECONDS));
        assertEquals(false, failed);
    }
}
