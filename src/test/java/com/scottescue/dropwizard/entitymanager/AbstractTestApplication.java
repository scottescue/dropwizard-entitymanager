package com.scottescue.dropwizard.entitymanager;

import com.google.common.collect.ImmutableList;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

public abstract class AbstractTestApplication extends io.dropwizard.Application<AbstractIntegrationTest.TestConfiguration> {
    final EntityManagerBundle<AbstractIntegrationTest.TestConfiguration> entityManagerBundle = new EntityManagerBundle<AbstractIntegrationTest.TestConfiguration>(
            supportedEntities(),
            new EntityManagerFactoryFactory(),
            new SharedEntityManagerFactory()) {
        @Override
        public PooledDataSourceFactory getDataSourceFactory(AbstractIntegrationTest.TestConfiguration configuration) {
            return configuration.getDataSource();
        }
    };

    @Override
    public void initialize(Bootstrap<AbstractIntegrationTest.TestConfiguration> bootstrap) {
        bootstrap.addBundle(entityManagerBundle);
        onInitialize(bootstrap);
    }

    @Override
    public void run(AbstractIntegrationTest.TestConfiguration configuration, Environment environment) throws Exception {
        final EntityManagerFactory entityManagerFactory = entityManagerBundle.getEntityManagerFactory();
        initDatabase(entityManagerFactory);

        environment.jersey().register(new UnitOfWorkApplicationListener("hr-db", entityManagerFactory));
        onRun(configuration, environment);
    }

    protected void initDatabase(EntityManagerFactory entityManagerFactory) {
        final EntityManager entityManager = entityManagerFactory.createEntityManager();
        final EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();

            onInitDatabase(entityManager);

            transaction.commit();
        } catch (PersistenceException e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
        } finally {
            entityManager.close();
        }
    }

    protected abstract ImmutableList<Class<?>> supportedEntities();

    protected void onInitialize(Bootstrap<AbstractIntegrationTest.TestConfiguration> bootstrap) {}

    protected void onRun(AbstractIntegrationTest.TestConfiguration configuration, Environment environment) throws Exception {}

    protected void onInitDatabase(EntityManager entityManager) {}

}
