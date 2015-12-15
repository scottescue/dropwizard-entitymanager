package com.scottescue.dropwizard.entitymanager;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module;
import com.google.common.collect.ImmutableList;
import com.scottescue.dropwizard.entitymanager.helper.Person;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class EntityManagerBundleTest {
    private final DataSourceFactory dbConfig = new DataSourceFactory();
    private final ImmutableList<Class<?>> entities = ImmutableList.<Class<?>>of(Person.class);
    private final EntityManagerFactoryFactory factory = mock(EntityManagerFactoryFactory.class);
    private final EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
    private final Configuration configuration = mock(Configuration.class);
    private final JerseyEnvironment jerseyEnvironment = mock(JerseyEnvironment.class);
    private final Environment environment = mock(Environment.class);
    private final EntityManagerBundle<Configuration> bundle = new EntityManagerBundle<Configuration>(entities, factory) {
        @Override
        public DataSourceFactory getDataSourceFactory(Configuration configuration) {
            return dbConfig;
        }
    };

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(environment.jersey()).thenReturn(jerseyEnvironment);
        when(jerseyEnvironment.getResourceConfig()).thenReturn(new DropwizardResourceConfig());

        when(factory.build(eq(bundle),
                any(Environment.class),
                any(DataSourceFactory.class),
                anyList(),
                eq("hibernate-jpa"))).thenReturn(entityManagerFactory);
    }

    @Test
    public void addsHibernateSupportToJackson() throws Exception {
        final ObjectMapper objectMapperFactory = mock(ObjectMapper.class);

        final Bootstrap<?> bootstrap = mock(Bootstrap.class);
        when(bootstrap.getObjectMapper()).thenReturn(objectMapperFactory);

        bundle.initialize(bootstrap);

        final ArgumentCaptor<Module> captor = ArgumentCaptor.forClass(Module.class);
        verify(objectMapperFactory).registerModule(captor.capture());

        assertThat(captor.getValue()).isInstanceOf(Hibernate4Module.class);
    }

    @Test
    public void buildsAnEntityManagerFactory() throws Exception {
        bundle.run(configuration, environment);

        verify(factory).build(bundle, environment, dbConfig, entities, "hibernate-jpa");
    }

    @Test
    public void hasAnEntityManagerFactory() throws Exception {
        bundle.run(configuration, environment);

        assertThat(bundle.getEntityManagerFactory()).isEqualTo(entityManagerFactory);
    }
}
