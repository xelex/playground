package playground.test.revo.api.v1.dto;

import lombok.Builder;
import lombok.Getter;
import playground.test.revo.data.TxManager;

import java.math.BigDecimal;

@Getter
@Builder
public class TransactionDTO {
    String from;
    String to;
    BigDecimal amount;
    TxManager.TxResult result;
}
