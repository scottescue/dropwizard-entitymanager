package com.scottescue.dropwizard.entitymanager;

import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.google.common.collect.ImmutableList;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.db.DatabaseConfiguration;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;


public abstract class EntityManagerBundle<T extends Configuration> implements ConfiguredBundle<T>, DatabaseConfiguration<T> {
    public static final String DEFAULT_NAME = "hibernate-entitymanager";

    private EntityManagerFactory entityManagerFactory;
    private EntityManagerContext entityManagerContext;
    private EntityManager sharedEntityManager;

    private final ImmutableList<Class<?>> entities;
    private final EntityManagerFactoryFactory entityManagerFactoryFactory;
    private final SharedEntityManagerFactory sharedEntityManagerFactory;

    protected EntityManagerBundle(Class<?> entity, Class<?>... entities) {
        this(ImmutableList.<Class<?>>builder().add(entity).add(entities).build(),
                new EntityManagerFactoryFactory(),
                new SharedEntityManagerFactory());
    }

    protected EntityManagerBundle(ImmutableList<Class<?>> entities,
                                  EntityManagerFactoryFactory entityManagerFactoryFactory,
                                  SharedEntityManagerFactory sharedEntityManagerFactory) {
        this.entities = entities;
        this.entityManagerFactoryFactory = entityManagerFactoryFactory;
        this.sharedEntityManagerFactory = sharedEntityManagerFactory;
    }

    public void run(T configuration, Environment environment) throws Exception {
        final PooledDataSourceFactory dbConfig = getDataSourceFactory(configuration);

        this.entityManagerFactory = entityManagerFactoryFactory.build(this, environment, dbConfig, entities, name());
        this.entityManagerContext = new EntityManagerContext(entityManagerFactory);
        this.sharedEntityManager = sharedEntityManagerFactory.build(entityManagerContext);

        registerUnitOfWorkListerIfAbsent(environment).registerEntityManagerFactory(name(), entityManagerFactory);
        environment.healthChecks().register(name(),
                new EntityManagerFactoryHealthCheck(
                        environment.getHealthCheckExecutorService(),
                        dbConfig.getValidationQueryTimeout().orElse(Duration.seconds(5)),
                        entityManagerFactory,
                        dbConfig.getValidationQuery()));
    }

    private UnitOfWorkApplicationListener registerUnitOfWorkListerIfAbsent(Environment environment) {
        for (Object singleton : environment.jersey().getResourceConfig().getSingletons()) {
            if (singleton instanceof UnitOfWorkApplicationListener) {
                return (UnitOfWorkApplicationListener) singleton;
            }
        }
        final UnitOfWorkApplicationListener listener = new UnitOfWorkApplicationListener();
        environment.jersey().register(listener);
        return listener;
    }

    public void initialize(Bootstrap<?> bootstrap) {
        bootstrap.getObjectMapper().registerModule(createHibernate5Module());
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public EntityManager getSharedEntityManager() {
        return sharedEntityManager;
    }

    /**
     * Override to configure the {@link Hibernate5Module}.
     */
    protected Hibernate5Module createHibernate5Module() {
        return new Hibernate5Module();
    }

    /**
     * Override to configure the name of the bundle
     * (It's used for the bundle health check and database pool metrics)
     */
    protected String name() {
        return DEFAULT_NAME;
    }

    /**
     * Override to configure JPA persistence unit.
     * @param configuration the persistence unit configuration
     */
    protected void configure(PersistenceUnitConfig configuration) {
    }

    ImmutableList<Class<?>> getEntities() {
        return entities;
    }

    EntityManagerContext getEntityManagerContext() {
        return this.entityManagerContext;
    }
}
