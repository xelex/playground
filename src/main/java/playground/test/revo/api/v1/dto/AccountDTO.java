package playground.test.revo.api.v1.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AccountDTO {
    String id;
    BigDecimal balance;
}
