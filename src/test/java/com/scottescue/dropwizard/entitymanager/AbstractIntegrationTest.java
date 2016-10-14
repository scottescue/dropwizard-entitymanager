package com.scottescue.dropwizard.entitymanager;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.After;

import javax.ws.rs.client.Client;

public abstract class AbstractIntegrationTest {

    public static class TestConfiguration extends Configuration {
        private DataSourceFactory dataSource = new DataSourceFactory();

        TestConfiguration(@JsonProperty("dataSource") DataSourceFactory dataSource) {
            this.dataSource = dataSource;
        }

        public DataSourceFactory getDataSource() {
            return dataSource;
        }
    }

    final protected Client client = new JerseyClientBuilder()
            .register(new JacksonMessageBodyProvider(Jackson.newObjectMapper()))
            .build();

    private DropwizardTestSupport dropwizardTestSupport;

    @After
    public void tearDown() {
        dropwizardTestSupport.after();
        client.close();
    }

    protected void setup(Class<? extends Application<TestConfiguration>> applicationClass) {
        dropwizardTestSupport = new DropwizardTestSupport<>(applicationClass, ResourceHelpers.resourceFilePath("integration-test.yaml"),
                ConfigOverride.config("dataSource.url", "jdbc:hsqldb:mem:DbTest" + System.nanoTime() + "?hsqldb.translate_dti_types=false"));
        dropwizardTestSupport.before();
    }

    protected String getUrlPrefix() {
        return "http://localhost:" + dropwizardTestSupport.getLocalPort();
    }

    protected String getUrl(String path) {
        return getUrlPrefix() + path;
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "unchecked"})
    protected static <T extends Throwable> T unwrapThrowable(Class<T> type, Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause == null) {
            return null;
        }
        return cause.getClass().equals(type) ? (T) cause : unwrapThrowable(type, cause);
    }

}
