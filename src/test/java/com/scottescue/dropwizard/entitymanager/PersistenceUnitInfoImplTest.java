package com.scottescue.dropwizard.entitymanager;

import com.scottescue.dropwizard.entitymanager.entity.Person;
import com.scottescue.dropwizard.entitymanager.entity.PersonType;
import com.scottescue.dropwizard.entitymanager.entity.Personable;
import org.hibernate.jpa.AvailableSettings;
import org.junit.Test;
import org.slf4j.Logger;

import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.sql.DataSource;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PersistenceUnitInfoImplTest {

    private final DataSource dataSource = mock(DataSource.class);
    private final Logger logger = mock(Logger.class);
    private final PersistenceUnitInfoImpl persistenceUnitInfo =
            new PersistenceUnitInfoImpl(getClass().getSimpleName(), dataSource, logger);

    @Test
    public void testExcludingUnlistedClasses() throws Exception {
        persistenceUnitInfo.setExcludeUnlistedClasses(false);
        assertThat(persistenceUnitInfo.excludeUnlistedClasses()).isFalse();
    }

    @Test
    public void testSpecifyingSharedCacheMode() throws Exception {
        persistenceUnitInfo.setSharedCacheMode(SharedCacheMode.ENABLE_SELECTIVE);
        assertThat(persistenceUnitInfo.getSharedCacheMode()).isEqualTo(SharedCacheMode.ENABLE_SELECTIVE);
    }

    @Test
    public void testSpecifyingValidationMode() throws Exception {
        persistenceUnitInfo.setValidationMode(ValidationMode.CALLBACK);
        assertThat(persistenceUnitInfo.getValidationMode()).isEqualTo(ValidationMode.CALLBACK);
    }

    @Test
    public void testAddingMappingFileName() throws Exception {
        final String expectedMappingFileName = "orm1.xml";

        persistenceUnitInfo.addMappingFileNames(expectedMappingFileName);
        assertThat(persistenceUnitInfo.getMappingFileNames()).containsOnly("orm1.xml");
    }

    @Test
    public void testAddingMappingFileNames() throws Exception {
        persistenceUnitInfo.addMappingFileNames("orm1.xml", "gorm2.xml", "test-orm.xml");
        assertThat(persistenceUnitInfo.getMappingFileNames()).containsOnly("orm1.xml", "gorm2.xml", "test-orm.xml");
    }

    @Test
    public void testAddingManagedClassName() throws Exception {
        final String expectedClassName = "com.acme.Product";

        persistenceUnitInfo.addManagedClassNames(expectedClassName);
        assertThat(persistenceUnitInfo.getManagedClassNames()).containsOnly(expectedClassName);
    }

    @Test
    public void testAddingManagedClassNames() throws Exception {
        persistenceUnitInfo.addManagedClassNames("com.acme.Product", "com.acme.Account", "com.acme.Location");
        assertThat(persistenceUnitInfo.getManagedClassNames()).containsOnly("com.acme.Product", "com.acme.Account", "com.acme.Location");
    }

    @Test
    public void testAddingJarFileUrl() {
        URL url = getResource(Person.class);
        persistenceUnitInfo.addJarFileUrls(url);
        assertThat(persistenceUnitInfo.getJarFileUrls()).containsOnly(url);
    }

    @Test
    public void testAddingJarFileUrls() {
        URL person = getResource(Person.class);
        URL personable = getResource(Personable.class);
        URL personType = getResource(PersonType.class);

        persistenceUnitInfo.addJarFileUrls(person, personable, personType);
        assertThat(persistenceUnitInfo.getJarFileUrls()).containsOnly(person, personable, personType);
    }

    @Test
    public void testSetProperty() throws Exception {
        final String key = "org.hibernate.awesome";
        final String expectedValue = "true";

        persistenceUnitInfo.setProperty(key, expectedValue);
        assertThat(persistenceUnitInfo.getProperties().getProperty(key)).isEqualTo(expectedValue);
    }

    @Test
    public void testSetEnhancerPropertyGeneratesWarning() {
        persistenceUnitInfo.setProperty(AvailableSettings.ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT, "true");
        verify(logger).warn(contains(AvailableSettings.ENHANCER_ENABLE_ASSOCIATION_MANAGEMENT));
    }

    @Test
    public void testAddingClassTransformerGeneratesWarning() {
        ClassTransformer classTransformer = mock(ClassTransformer.class);
        persistenceUnitInfo.addTransformer(classTransformer);
        verify(logger).warn(contains("does not support"));
    }

    @Test
    public void testPersistenceXMLSchemaVersionIsCorrect() {
        assertThat(persistenceUnitInfo.getPersistenceXMLSchemaVersion()).isEqualTo("2.1");
    }

    @Test
    public void testTempClassLoaderIsOfCorrectType() throws Exception {
        ClassLoader tempClassLoader = persistenceUnitInfo.getNewTempClassLoader();

        assertThat(tempClassLoader).isNotNull();
        assertThat(tempClassLoader).isInstanceOf(TemporaryClassLoader.class);
    }

    private URL getResource(Class<?> type) {
        return getClass().getClassLoader()
                .getResource(type.getName().replace(".", "/").concat(".class"));
    }
}