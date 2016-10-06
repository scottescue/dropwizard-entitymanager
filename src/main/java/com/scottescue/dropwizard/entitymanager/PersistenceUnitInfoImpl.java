package com.scottescue.dropwizard.entitymanager;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.net.URL;
import java.util.*;

class PersistenceUnitInfoImpl implements PersistenceUnitConfig, PersistenceUnitInfo {

    private static final String persistenceXmlSchemaVersion = "2.1";
    private static final DataSource jtaDataSource = null;
    private static final String persistenceProviderClassName = HibernatePersistenceProvider.class.getName();
    private static final URL persistenceUnitRootUrl = PersistenceUnitInfoImpl.class.getClassLoader().getResource("");
    private static final PersistenceUnitTransactionType transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;

    @SuppressWarnings("deprecation")
    private static final Set<String> UNSUPPORTED_ENHANCER_PROPERTIES = new HashSet<String>() {{
        add( org.hibernate.jpa.AvailableSettings.ENHANCER_ENABLE_DIRTY_TRACKING );
        add( org.hibernate.jpa.AvailableSettings.ENHANCER_ENABLE_LAZY_INITIALIZATION );
        add( org.hibernate.jpa.AvailableSettings.ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT );
        // The USE_CLASS_ENHANCER property is deprecated, but we still need to check whether a value is passed for it
        add( org.hibernate.jpa.AvailableSettings.USE_CLASS_ENHANCER );
    }};

    private final String persistenceUnitName;
    private final DataSource nonJtaDataSource;
    private final Logger logger;
    private final Set<String> mappingFileNames = new HashSet<>();
    private final Set<URL> jarFileUrls = new HashSet<>();
    private final Set<String> managedClassNames = new HashSet<>();
    private final Properties properties = new Properties();
    private boolean excludeUnlistedClasses = true;
    private SharedCacheMode sharedCacheMode = SharedCacheMode.UNSPECIFIED;
    private ValidationMode validationMode = ValidationMode.AUTO;

    PersistenceUnitInfoImpl(String persistenceUnitName, DataSource nonJtaDataSource) {
        this(persistenceUnitName, nonJtaDataSource, LoggerFactory.getLogger(PersistenceUnitInfoImpl.class));
    }

    PersistenceUnitInfoImpl(String persistenceUnitName, DataSource nonJtaDataSource, Logger logger) {
        this.persistenceUnitName = persistenceUnitName;
        this.nonJtaDataSource = nonJtaDataSource;
        this.logger = logger;
    }

    @Override
    public String getPersistenceUnitName() {
        return this.persistenceUnitName;
    }

    @Override
    public String getPersistenceProviderClassName() {
        return persistenceProviderClassName;
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return transactionType;
    }

    @Override
    public DataSource getJtaDataSource() {
        return jtaDataSource;
    }

    @Override
    public DataSource getNonJtaDataSource() {
        return this.nonJtaDataSource;
    }

    @Override
    public List<String> getMappingFileNames() {
        return toList(this.mappingFileNames);
    }

    @Override
    public PersistenceUnitConfig addMappingFileNames(String fileName, String...fileNames) {
        addAll(this.mappingFileNames, fileName, fileNames);
        return this;
    }

    @Override
    public List<URL> getJarFileUrls() {
        return toList(this.jarFileUrls);
    }

    @Override
    public PersistenceUnitConfig addJarFileUrls(URL url, URL... urls) {
        addAll(this.jarFileUrls, url, urls);
        return this;
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        return persistenceUnitRootUrl;
    }

    @Override
    public List<String> getManagedClassNames() {
        return toList(this.managedClassNames);
    }

    PersistenceUnitConfig addManagedClassNames(String className, String...classNames) {
        addAll(this.managedClassNames, className, classNames);
        return this;
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return this.excludeUnlistedClasses;
    }

    @Override
    public PersistenceUnitConfig setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
        this.excludeUnlistedClasses = excludeUnlistedClasses;
        return this;
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return this.sharedCacheMode;
    }

    @Override
    public PersistenceUnitConfig setSharedCacheMode(SharedCacheMode sharedCacheMode) {
        this.sharedCacheMode = sharedCacheMode;
        return this;
    }

    @Override
    public ValidationMode getValidationMode() {
        return this.validationMode;
    }

    @Override
    public PersistenceUnitConfig setValidationMode(ValidationMode validationMode) {
        this.validationMode = validationMode;
        return this;
    }

    @Override
    public Properties getProperties() {
        return this.properties;
    }

    @Override
    public PersistenceUnitConfig setProperty(String property, String value) {
        if (UNSUPPORTED_ENHANCER_PROPERTIES.contains(property) && Boolean.valueOf(value)) {
            logger.warn("Dropwizard EntityManager does not support Hibernate's bytecode enhancer, " +
                    "however the " + property + " property is set to true.  " +
                    "Hibernate's bytecode enhancer will be ignored.");
        }
        this.properties.setProperty(property, value);
        return this;
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return persistenceXmlSchemaVersion;
    }

    @Override
    public ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void addTransformer(ClassTransformer transformer) {
        logger.warn("Dropwizard EntityManager does not support JPA class transformation.  " +
                "The " + transformer.getClass().getName() + " class transformer will be ignored.");
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return new TemporaryClassLoader(getClassLoader());
    }


    @SafeVarargs
    final private <E> void addAll(Collection<E> collection, E element, E...elements) {
        collection.add(element);
        Collections.addAll(collection, elements);
    }

    private <E> List<E> toList(Set<E> set) {
        List<E> list = new ArrayList<>(set.size());
        list.addAll(set);
        return list;
    }
}
