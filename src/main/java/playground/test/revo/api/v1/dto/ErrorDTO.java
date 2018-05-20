package playground.test.revo.api.v1.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorDTO {
    int responseCode;
    String accountId;
    String code;
    String cause;
}
