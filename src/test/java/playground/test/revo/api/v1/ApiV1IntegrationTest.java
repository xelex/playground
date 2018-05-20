package playground.test.revo.api.v1;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import org.apache.commons.lang3.RandomUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import playground.test.revo.core.ApiServer;
import playground.test.revo.data.TxManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.equalTo;

public class ApiV1IntegrationTest {

    private static ApiServer apiServer;

    private static int getFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            return 8080;
        }
    }

    @BeforeClass
    public static void before() throws IOException {
        int port = getFreePort();
        RestAssured.port = port;
        RestAssured.baseURI = "http://127.0.0.1:" + port;
        RestAssured.defaultParser = Parser.JSON;

        apiServer = ApiServer.server()
                .bind("localhost", port)
                .baseApiPath("api")
                .enableApiV1(ApiV1.prefix());

        apiServer.start();
    }

    @AfterClass
    public static void after() {
        apiServer.stop();
    }

    @Test
    public void testNotFound() {
        RestAssured.when().get("/").then().statusCode(404);
        RestAssured.when().get("/api").then().statusCode(404);
        RestAssured.when().get("/api/v1").then().statusCode(404);

        RestAssured.when().get("/api/v1/accounts").then().statusCode(200);
        RestAssured.when().get("/api/v2/accounts").then().statusCode(404);
        RestAssured.when().get("/test/v1/accounts").then().statusCode(404);
    }

    @Test
    public void testDepositWorkflow() {
        RestAssured.when()
                .get("/api/v1/accounts")
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body(equalTo("[]"));

        RestAssured.when().get("/api/v1/accounts/test-1").then().statusCode(404);

        RestAssured.when()
                .get("/api/v1/deposit/test-1/1000")
                .then()
                .statusCode(200)
                .body("id", equalTo("test-1"))
                .body("balance", equalTo(1000));

        RestAssured.when()
                .get("/api/v1/deposit/test-2/-1000")
                .then()
                .statusCode(409)
                .body("cause", equalTo("Deposit error"))
                .body("responseCode", equalTo(409))
                .body("code", equalTo(TxManager.DepositResult.ERROR_INCORRECT_AMOUNT.toString()));

        RestAssured.when()
                .get("/api/v1/accounts")
                .then()
                .statusCode(200)
                .body(equalTo("[\"test-1\"]"));

        RestAssured.when()
                .get("/api/v1/accounts/test-1")
                .then()
                .statusCode(200)
                .body("id", equalTo("test-1"))
                .body("balance", equalTo(1000));

        RestAssured.when().get("/api/v1/accounts/test-2").then().statusCode(404);
        RestAssured.when().get("/api/v1/accounts/").then().statusCode(404);
    }

    @Test
    public void testTransferWorkflow() {
        RestAssured.when()
                .get("/api/v1/deposit/test-11/2000")
                .then()
                .statusCode(200)
                .body("id", equalTo("test-11"))
                .body("balance", equalTo(2000));

        RestAssured.when()
                .get("/api/v1/deposit/test-12/1000")
                .then()
                .statusCode(200)
                .body("id", equalTo("test-12"))
                .body("balance", equalTo(1000));

        RestAssured.when()
                .get("/api/v1/transfer/test-11/test-12/-1000")
                .then()
                .statusCode(409)
                .body("cause", equalTo("Transfer error"))
                .body("responseCode", equalTo(409))
                .body("code", equalTo(TxManager.TxResult.ERROR_INCORRECT_AMOUNT.toString()));

        RestAssured.when()
                .get("/api/v1/transfer/test-12/test-22/1000")
                .then()
                .statusCode(409)
                .body("cause", equalTo("Transfer error"))
                .body("responseCode", equalTo(409))
                .body("code", equalTo(TxManager.TxResult.ERROR_TO_NOT_FOUND.toString()));

        RestAssured.when()
                .get("/api/v1/transfer/test-12/test-12/1000")
                .then()
                .statusCode(409)
                .body("cause", equalTo("Transfer error"))
                .body("responseCode", equalTo(409))
                .body("code", equalTo(TxManager.TxResult.ERROR_SAME_FROM_TO.toString()));

        RestAssured.when()
                .get("/api/v1/transfer/test-22/test-12/1000")
                .then()
                .statusCode(409)
                .body("cause", equalTo("Transfer error"))
                .body("responseCode", equalTo(409))
                .body("code", equalTo(TxManager.TxResult.ERROR_FROM_NOT_FOUND.toString()));

        RestAssured.when()
                .get("/api/v1/transfer/test-11/test-12/5000")
                .then()
                .statusCode(409)
                .body("cause", equalTo("Transfer error"))
                .body("responseCode", equalTo(409))
                .body("code", equalTo(TxManager.TxResult.ERROR_INSUFFICIENT_AMOUNT.toString()));

        RestAssured.when()
                .get("/api/v1/transfer/test-11/test-12/1000")
                .then()
                .statusCode(200)
                .body("from", equalTo("test-11"))
                .body("to", equalTo("test-12"))
                .body("amount", equalTo(1000));

        RestAssured.when()
                .get("/api/v1/transfer/test-12/test-11/2000")
                .then()
                .statusCode(200)
                .body("from", equalTo("test-12"))
                .body("to", equalTo("test-11"))
                .body("amount", equalTo(2000));

        RestAssured.when()
                .get("/api/v1/accounts/test-11")
                .then()
                .statusCode(200)
                .body("id", equalTo("test-11"))
                .body("balance", equalTo(3000));

        RestAssured.when()
                .get("/api/v1/accounts/test-12")
                .then()
                .statusCode(200)
                .body("id", equalTo("test-12"))
                .body("balance", equalTo(0));
    }

    @Test
    public void stressTransfer() throws InterruptedException {
        long initialAmount = 1_000_000;
        long iterations = 200;
        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(cores);
        AtomicBoolean isOk = new AtomicBoolean(true);

        for (int i = 0; i < cores; i++) {
            RestAssured.when()
                    .get("/api/v1/deposit/stress-"+i+"/"+initialAmount)
                    .then()
                    .statusCode(200)
                    .body("id", equalTo("stress-"+i))
                    .body("balance", equalTo((int) initialAmount));
        }

        for (int i = 0; i < cores; i++) {
            service.execute(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        int from = j % cores;
                        int to = (j + 1) % cores;
                        int amount = RandomUtils.nextInt(10, 1000);

                        RestAssured.when()
                                .get("/api/v1/transfer/stress-" + from + "/stress-" + to + "/" + amount)
                                .then()
                                .statusCode(200)
                                .body("from", equalTo("stress-" + from))
                                .body("to", equalTo("stress-" + to))
                                .body("amount", equalTo(amount));
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    isOk.set(false);
                }
            });
        }

        service.shutdown();
        service.awaitTermination(20, TimeUnit.SECONDS);
        Assert.assertTrue(isOk.get());
    }
}