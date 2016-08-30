package com.scottescue.dropwizard.entitymanager;

import org.hibernate.jpa.HibernatePersistenceProvider;

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
        this.properties.setProperty(property, value);
        return this;
    }

    @SafeVarargs
    final private <E> void addAll(Collection<E> collection, E element, E...elements) {
        collection.add(element);
        Collections.addAll(collection, elements);
    }
}
