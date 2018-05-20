package playground.test.revo;

import playground.test.revo.api.v1.ApiV1;
import playground.test.revo.core.ApiServer;

public class Application {

    public static void main(String[] args) {
        ApiServer.server()
                .bind("localhost", 8080)
                .baseApiPath("api")
                .enableApiV1(ApiV1.prefix())
                .start();

        System.out.println("Server started at http://127.0.0.1:8080/api/v1");
        System.out.println("Hit Ctrl^C to exit");
    }
}
