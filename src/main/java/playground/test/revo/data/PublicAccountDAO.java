package playground.test.revo.data;

import java.math.BigDecimal;
import java.util.Collection;

public interface PublicAccountDAO {
    /**
     * Get a lsit of all available accounts
     *
     * @return a lsit of all available accounts
     */
    Collection<String> allAccounts();

    /**
     * Get balance of specific account
     *
     * @param id account id
     * @return balance
     */
    BigDecimal balance(String id);
}
