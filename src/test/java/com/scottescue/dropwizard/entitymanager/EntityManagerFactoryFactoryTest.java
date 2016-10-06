package com.scottescue.dropwizard.entitymanager;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.scottescue.dropwizard.entitymanager.entity.Person;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedPooledDataSource;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.logging.BootstrapLogging;
import io.dropwizard.setup.Environment;
import org.hibernate.jpa.AvailableSettings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class EntityManagerFactoryFactoryTest {
    static {
        BootstrapLogging.bootstrap();
    }

    private final EntityManagerFactoryFactory factory = new EntityManagerFactoryFactory();

    private final EntityManagerBundle<?> bundle = mock(EntityManagerBundle.class);
    private final LifecycleEnvironment lifecycleEnvironment = mock(LifecycleEnvironment.class);
    private final Environment environment = mock(Environment.class);
    private final MetricRegistry metricRegistry = new MetricRegistry();

    private DataSourceFactory config;
    private EntityManagerFactory entityManagerFactory;

    @Before
    public void setUp() throws Exception {
        when(bundle.name()).thenReturn(getClass().getSimpleName() + "-bundle");
        when(environment.metrics()).thenReturn(metricRegistry);
        when(environment.lifecycle()).thenReturn(lifecycleEnvironment);

        config = new DataSourceFactory();
        config.setUrl("jdbc:hsqldb:mem:DbTest-" + System.currentTimeMillis());
        config.setUser("sa");
        config.setDriverClass("org.hsqldb.jdbcDriver");
        config.setValidationQuery("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");
    }

    @After
    public void tearDown() throws Exception {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @Test
    public void managesTheSessionFactory() throws Exception {
        build();

        verify(lifecycleEnvironment).manage(any(EntityManagerFactoryManager.class));
    }

    @Test
    public void callsBundleToConfigure() throws Exception {
        build();

        verify(bundle).configure(any(PersistenceUnitConfig.class));
    }

    @Test
    public void setsPoolName() {
        build();

        ArgumentCaptor<EntityManagerFactoryManager> manager = ArgumentCaptor.forClass(EntityManagerFactoryManager.class);
        verify(lifecycleEnvironment).manage(manager.capture());
        ManagedPooledDataSource dataSource = (ManagedPooledDataSource) manager.getValue().getDataSource();
        assertThat(dataSource.getPool().getName()).isEqualTo("hibernate-entitymanager");
    }

    @Test
    public void setsACustomPoolName() {
        this.entityManagerFactory = factory.build(bundle, environment, config,
                ImmutableList.<Class<?>>of(Person.class), "custom-hibernate-db");

        ArgumentCaptor<EntityManagerFactoryManager> manager = ArgumentCaptor.forClass(EntityManagerFactoryManager.class);
        verify(lifecycleEnvironment).manage(manager.capture());
        ManagedPooledDataSource dataSource = (ManagedPooledDataSource) manager.getValue().getDataSource();
        assertThat(dataSource.getPool().getName()).isEqualTo("custom-hibernate-db");
    }

    @Test
    public void buildsAWorkingSessionFactory() throws Exception {
        build();

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            EntityTransaction entityTransaction = entityManager.getTransaction();
            entityTransaction.begin();

            entityManager.createNativeQuery("DROP TABLE people IF EXISTS").executeUpdate();
            entityManager.createNativeQuery("CREATE TABLE people (name varchar(100) primary key, email varchar(100), birthday timestamp)").executeUpdate();
            entityManager.createNativeQuery("INSERT INTO people VALUES ('Scott', 'scott@example.com', '1978-10-26 10:40:00')").executeUpdate();

            entityTransaction.commit();

            final Person entity = entityManager.find(Person.class, "Scott");

            assertThat(entity.getName())
                    .isEqualTo("Scott");

            assertThat(entity.getEmail())
                    .isEqualTo("scott@example.com");

            assertThat(entity.getBirthday().toDateTime(DateTimeZone.UTC))
                    .isEqualTo(new DateTime(1978, 10, 26, 10, 40, DateTimeZone.UTC));
        } finally {
            entityManager.close();
        }
    }

    @Test
    public void configureRunsBeforeSessionFactoryCreation(){
        final String expectedFactoryName = "Dropwizard Hibernate JPA Test Factory";
        final EntityManagerFactoryFactory customFactory = new EntityManagerFactoryFactory() {
            @Override
            protected void configure(PersistenceUnitConfig configuration) {
                super.configure(configuration);
                configuration.setProperty(AvailableSettings.ENTITY_MANAGER_FACTORY_NAME, expectedFactoryName);
            }
        };
        entityManagerFactory = customFactory.build(bundle,
                environment,
                config,
                ImmutableList.<Class<?>>of(Person.class));

        assertThat(entityManagerFactory.getProperties().get(AvailableSettings.ENTITY_MANAGER_FACTORY_NAME)).isSameAs(expectedFactoryName);
    }

    private void build() {
        this.entityManagerFactory = factory.build(bundle,
                environment,
                config,
                ImmutableList.<Class<?>>of(Person.class));
    }
}