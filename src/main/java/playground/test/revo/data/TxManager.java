package playground.test.revo.data;

import java.math.BigDecimal;

public interface TxManager {
    enum TxResult {
        SUCCESS,

        ERROR_TIMEOUT,
        ERROR_INCORRECT_AMOUNT,
        ERROR_FROM_NOT_FOUND,
        ERROR_SAME_FROM_TO,
        ERROR_TO_NOT_FOUND,
        ERROR_INSUFFICIENT_AMOUNT,

        ERROR_UNKNOWN
    }

    TxResult transfer(String from, String to, BigDecimal amount);

    /**
     * Result of the account deposit
     */
    enum DepositResult {
        SUCCESS,

        ERROR_TIMEOUT,
        ERROR_NOT_FOUND,
        ERROR_INCORRECT_AMOUNT,

        ERROR_UNKNOWN
    }

    /**
     * Creates an account (if needed) and deposits money
     * @param id account id to update or create
     * @param amount amount of money to deposit
     */
    DepositResult deposit(String id, BigDecimal amount);
}
