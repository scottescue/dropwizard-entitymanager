package com.scottescue.dropwizard.entitymanager;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.logging.BootstrapLogging;
import io.dropwizard.setup.Environment;
import org.assertj.core.data.MapEntry;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnitOfWorkAwareProxyFactoryTest {

    static {
        BootstrapLogging.bootstrap();
    }

    private static EntityManagerFactory entityManagerFactory;
    private static EntityManager sharedEntityManager;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void setUp() throws Exception {
        final EntityManagerBundle<?> bundle = mock(EntityManagerBundle.class);
        final Environment environment = mock(Environment.class);
        when(bundle.name()).thenReturn("test-bundle");
        when(environment.lifecycle()).thenReturn(mock(LifecycleEnvironment.class));
        when(environment.metrics()).thenReturn(new MetricRegistry());

        final DataSourceFactory dataSourceFactory = new DataSourceFactory();
        dataSourceFactory.setUrl("jdbc:hsqldb:mem:unit-of-work-" + UUID.randomUUID().toString());
        dataSourceFactory.setUser("sa");
        dataSourceFactory.setDriverClass("org.hsqldb.jdbcDriver");
        dataSourceFactory.setValidationQuery("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");
        dataSourceFactory.setProperties(ImmutableMap.of("hibernate.dialect", "org.hibernate.dialect.HSQLDialect"));
        dataSourceFactory.setInitialSize(1);
        dataSourceFactory.setMinSize(1);

        entityManagerFactory = new EntityManagerFactoryFactory()
                .build(bundle, environment, dataSourceFactory, ImmutableList.<Class<?>>of());

        final EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            EntityTransaction entityTransaction = entityManager.getTransaction();
            entityTransaction.begin();

            entityManager.createNativeQuery("create table user_sessions (token varchar(64) primary key, username varchar(16))")
                    .executeUpdate();
            entityManager.createNativeQuery("insert into user_sessions values ('67ab89d', 'jeff_28')")
                    .executeUpdate();

            entityTransaction.commit();
        } finally {
            entityManager.close();
        }

        final EntityManagerContext entityManagerContext = new EntityManagerContext(entityManagerFactory);
        sharedEntityManager = new SharedEntityManagerFactory().build(entityManagerContext);
    }

    @Test
    public void testEntityManagerFactoriesPopulated() {
        EntityManagerFactory adminFactory = mock(EntityManagerFactory.class);
        EntityManagerBundle adminBundle = mock(EntityManagerBundle.class);
        when(adminBundle.getEntityManagerFactory()).thenReturn(adminFactory);
        when(adminBundle.name()).thenReturn("admin");

        EntityManagerFactory appFactory = mock(EntityManagerFactory.class);
        EntityManagerBundle appBundle = mock(EntityManagerBundle.class);
        when(appBundle.getEntityManagerFactory()).thenReturn(appFactory);
        when(appBundle.name()).thenReturn("application");

        final UnitOfWorkAwareProxyFactory unitOfWorkAwareProxyFactory =
                new UnitOfWorkAwareProxyFactory(adminBundle, appBundle);

        assertThat(unitOfWorkAwareProxyFactory.getEntityManagerFactories()).containsExactly(
                        MapEntry.entry("admin", adminFactory),
                        MapEntry.entry("application", appFactory));
    }

    @Test
    public void testProxyWorks() throws Exception {
        final SessionService sessionService = new SessionService(sharedEntityManager);
        final UnitOfWorkAwareProxyFactory unitOfWorkAwareProxyFactory =
                new UnitOfWorkAwareProxyFactory("default", entityManagerFactory);

        final OAuthAuthenticator oAuthAuthenticator = unitOfWorkAwareProxyFactory
                .create(OAuthAuthenticator.class, SessionService.class, sessionService);
        assertThat(oAuthAuthenticator.authenticate("67ab89d")).isTrue();
        assertThat(oAuthAuthenticator.authenticate("bd1e23a")).isFalse();
    }

    @Test
    public void testProxyWorksWithoutUnitOfWork() {
        assertThat(new UnitOfWorkAwareProxyFactory("default", entityManagerFactory)
                .create(PlainAuthenticator.class)
                .authenticate("c82d11e"))
                .isTrue();
    }

    @Test
    public void testProxyHandlesErrors() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Session cluster is down");

        new UnitOfWorkAwareProxyFactory("default", entityManagerFactory)
                .create(BrokenAuthenticator.class)
                .authenticate("b812ae4");
    }

    static class SessionService {

        private EntityManager entityManager;

        public SessionService(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        public boolean isExist(String token) {
            return entityManager
                    .createNativeQuery("select username from user_sessions where token=:token")
                    .setParameter("token", token)
                    .getResultList()
                    .size() > 0;
        }

    }

    static class OAuthAuthenticator {

        private SessionService sessionService;

        public OAuthAuthenticator(SessionService sessionService) {
            this.sessionService = sessionService;
        }

        @UnitOfWork
        public boolean authenticate(String token) {
            return sessionService.isExist(token);
        }
    }

    static class PlainAuthenticator {

        public boolean authenticate(String token) {
            return true;
        }
    }

    static class BrokenAuthenticator {

        @UnitOfWork
        public boolean authenticate(String token) {
            throw new IllegalStateException("Session cluster is down");
        }
    }

}