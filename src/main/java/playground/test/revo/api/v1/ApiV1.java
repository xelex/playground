package playground.test.revo.api.v1;

import com.google.inject.Inject;
import io.undertow.Handlers;
import io.undertow.server.RoutingHandler;
import playground.test.revo.api.v1.handler.ApiV1Controller;

public class ApiV1 {
    public static String prefix() {
        return "v1";
    }

    private ApiV1Controller handler;

    @Inject
    public ApiV1(ApiV1Controller handler) {
        this.handler = handler;
    }

    /**
     * As far, as we are not running in production, we do not case about:
     * 1. Metric collection
     * 2. Correct exception handling
     * 3. Rate limiting
     * 4. etc
     *
     * @return route configuration for APU V1 handler
     */
    public RoutingHandler routes() {
        return Handlers.routing()
                .get("/accounts", handler::listAccounts)
                .get("/accounts/{id}", handler::showBalance)
                .get("/deposit/{id}/{amount}", handler::makeDeposit)
                .get("/transfer/{from}/{to}/{amount}", handler::makeTransfer)
                .setFallbackHandler(ApiV1Controller::notFound);
    }
}
