package com.scottescue.dropwizard.entitymanager;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.google.common.collect.ImmutableList;
import com.scottescue.dropwizard.entitymanager.entity.Person;
import com.scottescue.dropwizard.entitymanager.entity.fake.entities.pckg.FakeEntity1;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class EntityManagerBundleTest {
    private final DataSourceFactory dbConfig = new DataSourceFactory();
    private final ImmutableList<Class<?>> entities = ImmutableList.<Class<?>>of(Person.class);
    private final EntityManagerFactoryFactory factory = mock(EntityManagerFactoryFactory.class);
    private final EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
    private final SharedEntityManagerFactory sharedEntityManagerFactory = mock(SharedEntityManagerFactory.class);
    private final EntityManager sharedEntityManager = mock(EntityManager.class);
    private final HealthCheckRegistry healthChecks = mock(HealthCheckRegistry.class);
    private final Configuration configuration = mock(Configuration.class);
    private final JerseyEnvironment jerseyEnvironment = mock(JerseyEnvironment.class);
    private final Environment environment = mock(Environment.class);
    private final EntityManagerBundle<Configuration> bundle = new EntityManagerBundle<Configuration>(entities, factory, sharedEntityManagerFactory) {
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
        when(environment.healthChecks()).thenReturn(healthChecks);

        when(factory.build(eq(bundle),
                any(Environment.class),
                any(DataSourceFactory.class),
                anyList(),
                eq("hibernate-entitymanager"))).thenReturn(entityManagerFactory);

        when(sharedEntityManagerFactory.build(any(EntityManagerContext.class)))
                .thenReturn(sharedEntityManager);
    }

    @Test
    public void addsAllEntities() {
        EntityManagerBundle<Configuration> customBundle = new EntityManagerBundle<Configuration>(Person.class, FakeEntity1.class) {
            @Override
            public PooledDataSourceFactory getDataSourceFactory(Configuration configuration) {
                return dbConfig;
            }
        };
        assertThat(customBundle.getEntities()).containsExactly(Person.class, FakeEntity1.class);
    }

    @Test
    public void addsHibernateSupportToJackson() throws Exception {
        final ObjectMapper objectMapperFactory = mock(ObjectMapper.class);

        final Bootstrap<?> bootstrap = mock(Bootstrap.class);
        when(bootstrap.getObjectMapper()).thenReturn(objectMapperFactory);

        bundle.initialize(bootstrap);

        final ArgumentCaptor<Module> captor = ArgumentCaptor.forClass(Module.class);
        verify(objectMapperFactory).registerModule(captor.capture());

        assertThat(captor.getValue()).isInstanceOf(Hibernate5Module.class);
    }

    @Test
    public void buildsAnEntityManagerFactory() throws Exception {
        bundle.run(configuration, environment);

        verify(factory).build(bundle, environment, dbConfig, entities, "hibernate-entitymanager");
    }

    @Test
    public void hasAnEntityManagerFactory() throws Exception {
        bundle.run(configuration, environment);

        assertThat(bundle.getEntityManagerFactory()).isEqualTo(entityManagerFactory);
    }

    @Test
    public void buildsASharedEntityManagerFactory() throws Exception {
        bundle.run(configuration, environment);

        verify(sharedEntityManagerFactory).build(bundle.getEntityManagerContext());
    }

    @Test
    public void hasASharedEntityManagerFactory() throws Exception {
        bundle.run(configuration, environment);

        assertThat(bundle.getSharedEntityManager()).isEqualTo(sharedEntityManager);
    }

    @Test
    public void registersATransactionalListener() throws Exception {
        bundle.run(configuration, environment);

        final ArgumentCaptor<UnitOfWorkApplicationListener> captor =
                ArgumentCaptor.forClass(UnitOfWorkApplicationListener.class);
        verify(jerseyEnvironment).register(captor.capture());
    }

    @Test
    public void registersASessionFactoryHealthCheck() throws Exception {
        dbConfig.setValidationQuery("SELECT something");

        bundle.run(configuration, environment);

        final ArgumentCaptor<EntityManagerFactoryHealthCheck> captor =
                ArgumentCaptor.forClass(EntityManagerFactoryHealthCheck.class);
        verify(healthChecks).register(eq("hibernate-entitymanager"), captor.capture());

        assertThat(captor.getValue().getEntityManagerFactory()).isEqualTo(entityManagerFactory);

        assertThat(captor.getValue().getValidationQuery()).isEqualTo("SELECT something");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void registersACustomNameOfHealthCheckAndDBPoolMetrics() throws Exception {
        final EntityManagerBundle<Configuration> customBundle = new EntityManagerBundle<Configuration>(entities,
                factory, sharedEntityManagerFactory) {
            @Override
            public DataSourceFactory getDataSourceFactory(Configuration configuration) {
                return dbConfig;
            }

            @Override
            protected String name() {
                return "custom-hibernate";
            }
        };
        when(factory.build(eq(customBundle),
                any(Environment.class),
                any(DataSourceFactory.class),
                anyList(),
                eq("custom-hibernate"))).thenReturn(entityManagerFactory);

        customBundle.run(configuration, environment);

        final ArgumentCaptor<EntityManagerFactoryHealthCheck> captor =
                ArgumentCaptor.forClass(EntityManagerFactoryHealthCheck.class);
        verify(healthChecks).register(eq("custom-hibernate"), captor.capture());
    }

    @Test
    public void serializingLazyLoadedEntitiesConfigChanges() {
        bundle.setSerializeLazyLoadedEntitiesEnabled(false);

        // Ensure the value IS changed since the bundle has not been initialized
        assertThat(bundle.isSerializeLazyLoadedEntitiesEnabled()).isFalse();
    }

    @Test
    public void ignoresSerializingLazyLoadedEntitiesConfigChangeAfterInit() {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        Bootstrap<?> bootstrap = mock(Bootstrap.class);
        when(bootstrap.getObjectMapper()).thenReturn(objectMapper);

        bundle.initialize(bootstrap);
        bundle.setSerializeLazyLoadedEntitiesEnabled(false);

        // Ensure the value IS NOT changed since the bundle was already initialized
        assertThat(bundle.isSerializeLazyLoadedEntitiesEnabled()).isTrue();
    }
}
