package com.scottescue.dropwizard.entitymanager;


import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.setup.Environment;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

class EntityManagerFactoryFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityManagerFactoryFactory.class);
    private static final String DEFAULT_NAME = "hibernate-entitymanager";

    EntityManagerFactory build(EntityManagerBundle<?> bundle,
                                Environment environment,
                                PooledDataSourceFactory dbConfig,
                                List<Class<?>> entities) {
        return build(bundle, environment, dbConfig, entities, DEFAULT_NAME);
    }

    EntityManagerFactory build(EntityManagerBundle<?> bundle,
                                Environment environment,
                                PooledDataSourceFactory dbConfig,
                                List<Class<?>> entities,
                                String name) {
        final ManagedDataSource dataSource = dbConfig.build(environment.metrics(), name);
        return build(bundle, environment, dbConfig, dataSource, entities);
    }

    EntityManagerFactory build(EntityManagerBundle<?> bundle,
                                Environment environment,
                                PooledDataSourceFactory dbConfig,
                                ManagedDataSource dataSource,
                                List<Class<?>> entities) {
        final EntityManagerFactory factory = buildSessionFactory(bundle,
                dbConfig,
                dataSource,
                dbConfig.getProperties(),
                entities);
        final EntityManagerFactoryManager managedFactory = new EntityManagerFactoryManager(factory, dataSource);
        environment.lifecycle().manage(managedFactory);
        return factory;
    }

    private EntityManagerFactory buildSessionFactory(EntityManagerBundle<?> bundle,
                                                     PooledDataSourceFactory dbConfig,
                                                     ManagedDataSource dataSource,
                                                     Map<String, String> properties,
                                                     List<Class<?>> entities) {

        PersistenceUnitInfoImpl persistenceUnitInfo = new PersistenceUnitInfoImpl(bundle.name(), dataSource);

        persistenceUnitInfo.setProperty(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "managed");
        persistenceUnitInfo.setProperty(AvailableSettings.USE_SQL_COMMENTS,
                Boolean.toString(dbConfig.isAutoCommentsEnabled()));
        persistenceUnitInfo.setProperty(AvailableSettings.USE_GET_GENERATED_KEYS, "true");
        persistenceUnitInfo.setProperty(AvailableSettings.GENERATE_STATISTICS, "true");
        persistenceUnitInfo.setProperty(AvailableSettings.USE_REFLECTION_OPTIMIZER, "true");
        persistenceUnitInfo.setProperty(AvailableSettings.ORDER_UPDATES, "true");
        persistenceUnitInfo.setProperty(AvailableSettings.ORDER_INSERTS, "true");
        persistenceUnitInfo.setProperty(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true");
        persistenceUnitInfo.setProperty("jadira.usertype.autoRegisterUserTypes", "true");
        for (Map.Entry<String, String> property : properties.entrySet()) {
            persistenceUnitInfo.setProperty(property.getKey(), property.getValue());
        }

        addAnnotatedClasses(persistenceUnitInfo, entities);
        bundle.configure(persistenceUnitInfo);

        configure(persistenceUnitInfo);

        return new HibernatePersistenceProvider().createContainerEntityManagerFactory(persistenceUnitInfo, null);
    }

    protected void configure(PersistenceUnitConfig configuration) {
    }

    private void addAnnotatedClasses(PersistenceUnitInfoImpl persistenceUnitInfo,
                                     Iterable<Class<?>> entities) {
        final SortedSet<String> entityClasses = new TreeSet<>();
        for (Class<?> klass : entities) {
            persistenceUnitInfo.addManagedClassNames(klass.getName());
            entityClasses.add(klass.getCanonicalName());
        }
        LOGGER.info("Entity classes: {}", entityClasses);
    }
}
