package com.scottescue.dropwizard.entitymanager;

import org.junit.Test;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PersistenceUnitConfigTest {
    private final DataSource dataSource = mock(DataSource.class);

    // The 'adder' methods need to be tested to ensure they actually manipulate the internal collections correctly.
    // So instead of mocking PersistenceUnitConfig, an instance of its implementation is created.  Just to make sure
    // the tests stay focused on PersistenceUnitConfig, rather than its implementation, a PersistenceUnitConfig typed
    // handle is created.  Test methods exercise the methods exposed through the PersistenceUnitConfig handle, but assert
    // against the implementation handle.
    private final PersistenceUnitInfoImpl persistenceUnitInfo = new PersistenceUnitInfoImpl("default-test", dataSource);
    private final PersistenceUnitConfig persistenceUnitConfig = persistenceUnitInfo;

    @Test
    public void testAddingMappingFileName() throws Exception {
        final String expectedMappingFileName = "orm1.xml";

        persistenceUnitConfig.addMappingFileNames(expectedMappingFileName);
        assertThat(persistenceUnitInfo.getMappingFileNames()).containsOnly("orm1.xml");
    }

    @Test
    public void testAddingMappingFileNames() throws Exception {
        persistenceUnitConfig.addMappingFileNames("orm1.xml", "gorm2.xml", "test-orm.xml");
        assertThat(persistenceUnitInfo.getMappingFileNames()).containsOnly("orm1.xml", "gorm2.xml", "test-orm.xml");
    }

    @Test
    public void testAddingManagedClassName() throws Exception {
        final String expectedClassName = "com.acme.Product";

        persistenceUnitConfig.addManagedClassNames(expectedClassName);
        assertThat(persistenceUnitInfo.getManagedClassNames()).containsOnly(expectedClassName);
    }

    @Test
    public void testAddingManagedClassNames() throws Exception {
        persistenceUnitConfig.addManagedClassNames("com.acme.Product", "com.acme.Account", "com.acme.Location");
        assertThat(persistenceUnitInfo.getManagedClassNames()).containsOnly("com.acme.Product", "com.acme.Account", "com.acme.Location");
    }

    @Test
    public void testSetProperty() throws Exception {
        final String key = "org.hibernate.awesome";
        final String expectedValue = "true";

        persistenceUnitConfig.setProperty(key, expectedValue);
        assertThat(persistenceUnitInfo.getProperties().getProperty(key)).isEqualTo(expectedValue);
    }
}