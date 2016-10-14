package com.scottescue.dropwizard.entitymanager;

import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.google.common.annotations.VisibleForTesting;
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

/**
 * A bundle for easily creating a JPA persistence unit.  When creating an instance of the bundle you simply
 * need to provide a list of the JPA entities for the persistence unit and provide an implementation of
 * {@link DatabaseConfiguration#getDataSourceFactory(Configuration)}.
 *
 * @param <T> the {@link Configuration} type expected by this bundle
 */
public abstract class EntityManagerBundle<T extends Configuration> implements ConfiguredBundle<T>, DatabaseConfiguration<T> {
    public static final String DEFAULT_NAME = "hibernate-entitymanager";

    private EntityManagerFactory entityManagerFactory;
    private EntityManagerContext entityManagerContext;
    private EntityManager sharedEntityManager;
    private boolean serializeLazyLoadedEntitiesEnabled = true;
    private boolean initialized = false;

    private final ImmutableList<Class<?>> entities;
    private final EntityManagerFactoryFactory entityManagerFactoryFactory;
    private final SharedEntityManagerFactory sharedEntityManagerFactory;

    protected EntityManagerBundle(Class<?> entity, Class<?>... entities) {
        this(ImmutableList.<Class<?>>builder().add(entity).add(entities).build(),
                new EntityManagerFactoryFactory(),
                new SharedEntityManagerFactory());
    }

    @VisibleForTesting
    EntityManagerBundle(ImmutableList<Class<?>> entities,
                                  EntityManagerFactoryFactory entityManagerFactoryFactory,
                                  SharedEntityManagerFactory sharedEntityManagerFactory) {
        this.entities = entities;
        this.entityManagerFactoryFactory = entityManagerFactoryFactory;
        this.sharedEntityManagerFactory = sharedEntityManagerFactory;
    }

    @Override
    public final void run(T configuration, Environment environment) throws Exception {
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

    @Override
    public final void initialize(Bootstrap<?> bootstrap) {
        Hibernate5Module module = createHibernate5Module();
        configure(module);
        bootstrap.getObjectMapper().registerModule(module);
        initialized = true;
    }

    /**
     * Returns the {@link EntityManagerFactory} built and configured when this bundle is bootstrapped.  This
     * EntityManagerFactory can be used to create new {@link EntityManager} instances.  Each EntityManager
     * created from this factory will have its own persistence context, which your application must manage.
     *
     * @see com.scottescue.dropwizard.entitymanager.EntityManagerBundle#configure(PersistenceUnitConfig)
     *
     * @return the EntityManagerFactory
     */
    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    /**
     * Returns the managed, thread-safe {@link EntityManager} built and configured when this bundle is bootstrapped.
     * This EntityManager can be safely injected into your application code, but must be used with
     * {@link UnitOfWork} annotations to declaratively scope persistence context and transaction
     * boundaries.
     *
     * @see com.scottescue.dropwizard.entitymanager.UnitOfWork
     * @see com.scottescue.dropwizard.entitymanager.UnitOfWorkAwareProxyFactory
     *
     * @return the managed, thread-safe EntityManager
     */
    public EntityManager getSharedEntityManager() {
        return sharedEntityManager;
    }

    /**
     * Returns a boolean value indicating whether or not serializing lazy loaded entity associations is enabled.
     * Serializing Lazy loaded entity associations is enabled by default.
     *
     * @return the value indicating whether serializing lazy loaded entity associations is enabled or not
     */
    public boolean isSerializeLazyLoadedEntitiesEnabled() {
        return serializeLazyLoadedEntitiesEnabled;
    }

    /**
     * Enables or disables serializing lazy loaded entity associations as determined by the given value.
     *
     * <br/><br/><i><strong>Note: </strong>This method should be called before the EntityManagerBundle is added
     * to the application's {@link Bootstrap}, which initializes the bundle.  Once the bundle is initialized,
     * any changes to the lazy loading property are ignored.</i>
     *
     * @param serializeLazyLoadedEntitiesEnabled the value indicating whether lazy loading is enabled or not
     */
    public void setSerializeLazyLoadedEntitiesEnabled(boolean serializeLazyLoadedEntitiesEnabled) {
        // If the module is already initialized/bootstrapped there's no point in updating this property,
        // an ObjectMapper has already been created and updating the property value could make the property
        // out-of-sync with how the ObjectMapper is configured
        if (initialized) {
            return;
        }
        this.serializeLazyLoadedEntitiesEnabled = serializeLazyLoadedEntitiesEnabled;

    }

    /**
     * Override to configure the name of the bundle
     * (It's used for the bundle health check and database pool metrics)
     */
    protected String name() {
        return DEFAULT_NAME;
    }

    /**
     * Override to configure Jackson's {@link Hibernate5Module}.
     *
     * @param module the Hibernate5Module object
     */
    protected void configure(Hibernate5Module module) {
    }

    /**
     * Override to configure the JPA persistence unit.
     *
     * @param configuration the configuration object used to tune persistence unit configuration
     */
    protected void configure(PersistenceUnitConfig configuration) {
    }

    ImmutableList<Class<?>> getEntities() {
        return entities;
    }

    EntityManagerContext getEntityManagerContext() {
        return this.entityManagerContext;
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

    private Hibernate5Module createHibernate5Module() {
        Hibernate5Module module = new Hibernate5Module();
        if (serializeLazyLoadedEntitiesEnabled) {
            module.enable(Hibernate5Module.Feature.FORCE_LAZY_LOADING);
        }
        return module;
    }
}
