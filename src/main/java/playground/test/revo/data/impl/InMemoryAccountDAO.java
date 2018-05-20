package playground.test.revo.data.impl;

import playground.test.revo.data.PrivateAccountDAO;
import playground.test.revo.data.PublicAccountDAO;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NOTE: it is not reallt thread safe
 */
public class InMemoryAccountDAO implements PublicAccountDAO, PrivateAccountDAO {
    private Map<String, BigDecimal> balances = new ConcurrentHashMap<>();

    @Override
    public BigDecimal reduce(String id, BigDecimal amount) {
        checkPositive(amount);

        BigDecimal initialBalance = balances.get(id);

        if (initialBalance == null) {
            throw new IllegalArgumentException("Account does not exist: " + id);
        }

        BigDecimal newBalance = initialBalance.add(amount.negate());

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            return null;
        }

        balances.put(id, newBalance);
        return newBalance;
    }

    @Override
    public BigDecimal deposit(String id, BigDecimal amount) {
        checkPositive(amount);

        return balances.computeIfPresent(id, (i, balance) ->
                balance.add(amount));
    }

    @Override
    public BigDecimal createAndDeposit(String id, BigDecimal amount) {
        checkPositive(amount);

        return balances.compute(id, (i, balance) ->
                balance == null ? amount : balance.add(amount));
    }

    @Override
    public boolean exists(String id) {
        return balances.containsKey(id);
    }

    @Override
    public Collection<String> allAccounts() {
        return balances.keySet();
    }

    @Override
    public BigDecimal balance(String id) {
        return balances.get(id);
    }

    private static void checkPositive(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount can not be negative");
        }
    }
}
