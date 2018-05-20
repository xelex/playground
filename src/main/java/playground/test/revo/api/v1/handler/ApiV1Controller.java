package playground.test.revo.api.v1.handler;

import com.google.gson.Gson;
import com.google.inject.Inject;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import playground.test.revo.api.v1.dto.AccountDTO;
import playground.test.revo.api.v1.dto.ErrorDTO;
import playground.test.revo.api.v1.dto.TransactionDTO;
import playground.test.revo.data.PublicAccountDAO;
import playground.test.revo.data.TxManager;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;

public class ApiV1Controller {
    private static Gson gson = new Gson();

    private TxManager txManager;

    private PublicAccountDAO publicAccountDAO;

    @Inject
    public ApiV1Controller(TxManager txManager, PublicAccountDAO publicAccountDAO) {
        this.txManager = txManager;
        this.publicAccountDAO = publicAccountDAO;
    }

    public void listAccounts(HttpServerExchange exchange) {
        Collection<String> accounts = publicAccountDAO.allAccounts();
        sendJson(exchange, accounts);
    }

    public void showBalance(HttpServerExchange exchange) {
        String id = parseParameter(exchange, "id");

        BigDecimal balance = publicAccountDAO.balance(id);
        if (balance == null) {
            sendError(exchange, ErrorDTO.builder()
                    .responseCode(StatusCodes.NOT_FOUND)
                    .accountId(id)
                    .cause("Account not found")
                    .build());
        } else {
            sendJson(exchange, AccountDTO.builder()
                    .id(id)
                    .balance(balance)
                    .build());
        }
    }

    public void makeDeposit(HttpServerExchange exchange) {
        String id = parseParameter(exchange, "id");
        BigDecimal amount = parseAmountParameter(exchange);

        TxManager.DepositResult result = txManager.deposit(id, amount);
        BigDecimal balance = publicAccountDAO.balance(id);

        if (result == null || !result.equals(TxManager.DepositResult.SUCCESS)) {
            sendError(exchange, ErrorDTO.builder()
                    .responseCode(StatusCodes.CONFLICT)
                    .code(result == null ? null : result.toString())
                    .cause("Deposit error")
                    .build());
        } else if (balance == null) {
            sendError(exchange, ErrorDTO.builder()
                    .responseCode(StatusCodes.CONFLICT)
                    .cause("Account balance retrieval error")
                    .build());
        } else {
            sendJson(exchange, AccountDTO.builder()
                    .id(id)
                    .balance(balance)
                    .build());
        }
    }

    public void makeTransfer(HttpServerExchange exchange) {
        String from = parseParameter(exchange, "from");
        String to = parseParameter(exchange, "to");
        BigDecimal amount = parseAmountParameter(exchange);

        TxManager.TxResult result = txManager.transfer(from, to, amount);

        if (result == null || !result.equals(TxManager.TxResult.SUCCESS)) {
            sendError(exchange, ErrorDTO.builder()
                    .responseCode(StatusCodes.CONFLICT)
                    .code(result == null ? null : result.toString())
                    .cause("Transfer error")
                    .build());
        } else {
            sendJson(exchange, TransactionDTO.builder()
                    .from(from)
                    .to(to)
                    .amount(amount)
                    .result(result)
                    .build());
        }
    }

    private BigDecimal parseAmountParameter(HttpServerExchange exchange) {
        String amountString = parseParameter(exchange, "amount");
        BigDecimal amount = null;
        try {
            amount = new BigDecimal(amountString);
        } catch (NumberFormatException e) {
            // Ignore
        }
        return amount;
    }

    public static void notFound(HttpServerExchange exchange) {
        sendError(exchange, ErrorDTO.builder()
                .responseCode(StatusCodes.NOT_FOUND)
                .cause("API v1: page not found").build());
    }

    private static void sendJson(HttpServerExchange exchange, Object response) {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(gson.toJson(response));
    }

    private static void sendError(HttpServerExchange exchange, ErrorDTO response) {
        exchange.setStatusCode(response.getResponseCode());
        sendJson(exchange, response);
    }

    private static String parseParameter(HttpServerExchange exchange, String key) {
        Map<String, Deque<String>> params = exchange.getQueryParameters();
        Deque<String> tmp = params.get(key);
        String id = null;
        if (tmp != null && tmp.size() == 1) {
            id = tmp.getFirst();
        }
        return id;
    }
}
