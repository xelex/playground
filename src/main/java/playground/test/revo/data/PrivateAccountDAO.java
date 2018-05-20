package playground.test.revo.data;

import java.math.BigDecimal;

public interface PrivateAccountDAO {
    /**
     * Reduces deposit balance by specified amount
     *
     * @param id account id
     * @param amount amount to reduce
     * @return null if current balance is less than specified amount, new balance otherwise
     *
     * @implNote call under TX manager only
     */
    BigDecimal reduce(String id, BigDecimal amount);

    /**
     * Deposit money to account
     *
     * @param id account id
     * @param amount amount to deposit
     * @return null if account is not present, new account balance otherwise
     *
     * @implNote call under TX manager only
     */
    BigDecimal deposit(String id, BigDecimal amount);

    /**
     * Create account (if needed) and deposit money
     *
     * @param id account id
     * @param amount amount to deposit
     * @return new account balance
     *
     * @implNote call under TX manager only
     */
    BigDecimal createAndDeposit(String id, BigDecimal amount);

    /**
     * Returns TRUE if account is available
     *
     * @param id account id
     * @return if account is available
     */
    boolean exists(String id);
}
