package playground.test.revo.core;

import com.google.inject.*;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.PathHandler;
import playground.test.revo.api.v1.ApiV1;
import playground.test.revo.api.v1.handler.ApiV1Controller;
import playground.test.revo.data.PrivateAccountDAO;
import playground.test.revo.data.PublicAccountDAO;
import playground.test.revo.data.TxManager;
import playground.test.revo.data.impl.InMemoryAccountDAO;
import playground.test.revo.data.impl.SimpleTxManager;
import playground.test.revo.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class ApiServer extends AbstractModule {

    private String hostname;
    private int port;
    private String baseApiPath = "";
    private Map<String, RoutingHandler> apis = new HashMap<>();

    @Inject
    private ApiV1 v1;

    private volatile Undertow undertow = null;

    @Override
    protected void configure() {
        bind(InMemoryAccountDAO.class).in(Scopes.SINGLETON);
        bind(PublicAccountDAO.class).to(InMemoryAccountDAO.class);
        bind(PrivateAccountDAO.class).to(InMemoryAccountDAO.class);

        bind(TxManager.class).to(SimpleTxManager.class).asEagerSingleton();

        bind(ApiV1Controller.class).in(Scopes.SINGLETON);
    }

    public static ApiServer server() {
        Injector injector = Guice.createInjector(new ApiServer());
        return injector.getInstance(ApiServer.class);
    }

    public ApiServer bind(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        return this;
    }

    public ApiServer enableApiV1(final String path) {
        this.apis.put(path, v1.routes());
        return this;
    }

    public ApiServer baseApiPath(final String baseApiPath) {
        this.baseApiPath = baseApiPath;
        return this;
    }

    private PathHandler routes() {
        PathHandler rootApi = Handlers.path();

        apis.forEach((prefix, handler) ->
                rootApi.addPrefixPath(prefix, Handlers.disableCache(handler)));

        if (StringUtils.isNullOrEmpty(baseApiPath)) {
            return rootApi;
        }

        return Handlers.path()
                .addPrefixPath(baseApiPath, rootApi);
    }

    public void start() {
        undertow = Undertow.builder()
                .addHttpListener(port, hostname)
                .setHandler(ErrorHandler.build(routes()))
                .build();

        undertow.start();
    }

    public void stop() {
        if (undertow != null) {
            undertow.stop();
        }
    }
}
