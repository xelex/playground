package playground.test.revo.data.impl;

import com.google.inject.Inject;
import playground.test.revo.data.PrivateAccountDAO;
import playground.test.revo.data.TxManager;
import playground.test.revo.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static playground.test.revo.util.StringUtils.isNullOrEmpty;
import static playground.test.revo.util.StringUtils.notNullOrEmpty;

/**
 * Implementation limitations:
 * 1. TX map is constantly growing, use some eviction strategy to fix it
 * 2. It is in-memory and single-server. So, "CA", without "P".
 */
public class SimpleTxManager implements TxManager {

    private static final long DEFAULT_LOCK_TIMEOUT_MS = 500;

    private final Map<String, ReentrantLock> tx = new ConcurrentHashMap<>();

    private final PrivateAccountDAO accountDAO;

    private final long lockTimeout;

    @Inject
    public SimpleTxManager(PrivateAccountDAO accountDAO) {
        this(accountDAO, DEFAULT_LOCK_TIMEOUT_MS);
    }

    SimpleTxManager(PrivateAccountDAO accountDAO, long lockTimeout) {
        this.accountDAO = accountDAO;
        this.lockTimeout = lockTimeout;
    }

    @Override
    public TxResult transfer(String from, String to, BigDecimal amount) {
        try {
            if (isNullOrEmpty(from) || !accountDAO.exists(from)) {
                return TxResult.ERROR_FROM_NOT_FOUND;
            }

            if (isNullOrEmpty(to) || !accountDAO.exists(to)) {
                return TxResult.ERROR_TO_NOT_FOUND;
            }

            if (to.equals(from)) {
                return TxResult.ERROR_SAME_FROM_TO;
            }

            if (amount == null || isNegative(amount)) {
                return TxResult.ERROR_INCORRECT_AMOUNT;
            }

            return runTx(from, to, () -> {
                BigDecimal reduce = accountDAO.reduce(from, amount);
                if (reduce == null) {
                    return TxResult.ERROR_INSUFFICIENT_AMOUNT;
                }
                accountDAO.deposit(to, amount);
                return TxResult.SUCCESS;
            });
        } catch (Throwable e) {
            return TxResult.ERROR_UNKNOWN;
        }
    }

    @Override
    public DepositResult deposit(String id, BigDecimal amount) {
        try {
            if (isNullOrEmpty(id)) {
                return DepositResult.ERROR_NOT_FOUND;
            }

            if (amount == null || isNegative(amount)) {
                return DepositResult.ERROR_INCORRECT_AMOUNT;
            }

            return runTx(id, () -> {
                accountDAO.createAndDeposit(id, amount);
                return DepositResult.SUCCESS;
            });
        } catch (Throwable e) {
            return DepositResult.ERROR_UNKNOWN;
        }
    }

    DepositResult runTx(String id, Supplier<DepositResult> func) {
        assert notNullOrEmpty(id);

        boolean locked = false;
        try {
            locked = lock(id);
            if (locked) {
                return func.get();
            }
        } catch (Throwable e) {
            return DepositResult.ERROR_UNKNOWN;
        } finally {
            if (locked) {
                unlock(id);
            }
        }

        return DepositResult.ERROR_TIMEOUT;
    }

    TxResult runTx(String from, String to, Supplier<TxResult> func) {
        assert notNullOrEmpty(from);
        assert notNullOrEmpty(to);

        if (from.compareTo(to) > 0) {
            return runTx(to, from, func);
        }

        boolean lock_from = false;
        boolean lock_to = false;
        try {
            lock_from = lock(from);
            if (lock_from) {
                lock_to = lock(to);
                if (lock_to) {
                    return func.get();
                }
            }
        } catch (Exception e) {
            return TxResult.ERROR_UNKNOWN;
        } finally {
            if (lock_from) {
                unlock(from);
            }

            if (lock_to) {
                unlock(to);
            }
        }

        return TxResult.ERROR_TIMEOUT;
    }

    private boolean lock(String id) {
        try {
            ReentrantLock lock = tx.computeIfAbsent(id, i -> new ReentrantLock());
            return lock.tryLock(lockTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    private void unlock(String id) {
        tx.get(id).unlock();
    }

    private static boolean isNegative(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }
}
