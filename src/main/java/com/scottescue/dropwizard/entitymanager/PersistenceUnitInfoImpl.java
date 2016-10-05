package com.scottescue.dropwizard.entitymanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

class PersistenceUnitInfoImpl extends PersistenceUnitConfig implements PersistenceUnitInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceUnitInfoImpl.class);

    PersistenceUnitInfoImpl(String persistenceUnitName, DataSource nonJtaDataSource) {
        super(persistenceUnitName, nonJtaDataSource);
    }

    @Override
    public String getPersistenceUnitName() {
        return this.persistenceUnitName;
    }

    @Override
    public String getPersistenceProviderClassName() {
        return this.persistenceProviderClassName;
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return this.transactionType;
    }

    @Override
    public DataSource getJtaDataSource() {
        return this.jtaDataSource;
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
    public List<URL> getJarFileUrls() {
        return toList(this.jarFileUrls);
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        return this.persistenceUnitRootUrl;
    }

    @Override
    public List<String> getManagedClassNames() {
        return this.managedClassNames;
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return this.excludeUnlistedClasses;
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return this.sharedCacheMode;
    }

    @Override
    public ValidationMode getValidationMode() {
        return this.validationMode;
    }

    @Override
    public Properties getProperties() {
        return this.properties;
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return this.persistenceXmlSchemaVersion;
    }

    @Override
    public ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void addTransformer(ClassTransformer transformer) {
        if (transformer != null) {
            LOGGER.info("Dropwizard EntityManager does not support JPA class transformation.  The " + transformer.getClass().getName() + " class transformer will be ignored.");
        }
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return new TemporaryClassLoader(getClassLoader());
    }

    /*
     * Copy the given Set to a List of the same type
     */
    private <E> List<E> toList(Set<E> set) {
        List<E> list = new ArrayList<>(set.size());
        list.addAll(set);
        return list;
    }
}
