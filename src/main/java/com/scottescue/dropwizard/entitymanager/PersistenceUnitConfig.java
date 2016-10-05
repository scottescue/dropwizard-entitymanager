package com.scottescue.dropwizard.entitymanager;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.net.URL;
import java.util.*;

/**
 * Provides a means to configure {@link javax.persistence.PersistenceUnit} options.
 */
public abstract class PersistenceUnitConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceUnitConfig.class);

    @SuppressWarnings("deprecation")
    private static final Set<String> UNSUPPORTED_ENHANCER_PROPERTIES = new HashSet<String>() {{
        add( org.hibernate.jpa.AvailableSettings.ENHANCER_ENABLE_DIRTY_TRACKING );
        add( org.hibernate.jpa.AvailableSettings.ENHANCER_ENABLE_LAZY_INITIALIZATION );
        add( org.hibernate.jpa.AvailableSettings.ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT );
        // The USE_CLASS_ENHANCER property is deprecated, but we still need to check whether a value is passed for it
        add( org.hibernate.jpa.AvailableSettings.USE_CLASS_ENHANCER );
    }};

    protected final String persistenceUnitName;
    protected final String persistenceProviderClassName = HibernatePersistenceProvider.class.getName();
    protected final PersistenceUnitTransactionType transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
    protected final DataSource jtaDataSource = null;
    protected final DataSource nonJtaDataSource;
    protected final Set<String> mappingFileNames = new HashSet<>();
    protected final Set<URL> jarFileUrls = new HashSet<>();
    protected final URL persistenceUnitRootUrl = getClass().getClassLoader().getResource("");
    protected final List<String> managedClassNames = new ArrayList<>();
    protected boolean excludeUnlistedClasses = true;
    protected SharedCacheMode sharedCacheMode = SharedCacheMode.UNSPECIFIED;
    protected ValidationMode validationMode = ValidationMode.AUTO;
    protected final Properties properties = new Properties();
    protected final String persistenceXmlSchemaVersion = "2.1";

    PersistenceUnitConfig(String persistenceUnitName, DataSource nonJtaDataSource) {
        this.persistenceUnitName = persistenceUnitName;
        this.nonJtaDataSource = nonJtaDataSource;
    }

    public PersistenceUnitConfig setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
        this.excludeUnlistedClasses = excludeUnlistedClasses;
        return this;
    }

    public PersistenceUnitConfig setSharedCacheMode(SharedCacheMode sharedCacheMode) {
        this.sharedCacheMode = sharedCacheMode;
        return this;
    }

    public PersistenceUnitConfig setValidationMode(ValidationMode validationMode) {
        this.validationMode = validationMode;
        return this;
    }

    public PersistenceUnitConfig addMappingFileNames(String fileName, String...fileNames) {
        addAll(this.mappingFileNames, fileName, fileNames);
        return this;
    }

    public PersistenceUnitConfig addManagedClassNames(String className, String...classNames) {
        addAll(this.managedClassNames, className, classNames);
        return this;
    }

    public PersistenceUnitConfig setProperty(String property, String value) {
        if (UNSUPPORTED_ENHANCER_PROPERTIES.contains(property) && Boolean.valueOf(value)) {
            LOGGER.warn("Dropwizard EntityManager does not support Hibernate's bytecode enhancer, however the " + property + " property is set to true.  Hibernate's bytecode enhancer will be ignored.");
        }
        this.properties.setProperty(property, value);
        return this;
    }

    @SafeVarargs
    final private <E> void addAll(Collection<E> collection, E element, E...elements) {
        collection.add(element);
        Collections.addAll(collection, elements);
    }
}
