package uk.gov.justice.services.example.cakeshop.it.helpers;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;

public class RestEasyClientFactory {

    public ResteasyClient createResteasyClient() {
        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        final CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();
        cm.setMaxTotal(200); // Increase max total connection to 200
        cm.setDefaultMaxPerRoute(20); // Increase default max connection per route to 20
        final ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(httpClient);
        return new ResteasyClientBuilderImpl().httpEngine(engine).build();
    }
}
