package playground.test.revo.core;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;

import java.io.IOException;

public class ErrorHandler implements HttpHandler {

    private HttpHandler next;

    private ErrorHandler(HttpHandler next) {
        this.next = next;
    }

    static ErrorHandler build(HttpHandler next) {
        return new ErrorHandler(next);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        try {
            next.handleRequest(exchange);
        } catch (Throwable e) {
            exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
        }
    }
}
