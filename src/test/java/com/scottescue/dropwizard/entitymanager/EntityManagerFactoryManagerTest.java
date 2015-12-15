package com.scottescue.dropwizard.entitymanager;

import io.dropwizard.db.ManagedDataSource;
import org.junit.Test;

import javax.persistence.EntityManagerFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EntityManagerFactoryManagerTest {
    private final EntityManagerFactory factory = mock(EntityManagerFactory.class);
    private final ManagedDataSource dataSource = mock(ManagedDataSource.class);
    private final EntityManagerFactoryManager manager = new EntityManagerFactoryManager(factory, dataSource);

    @Test
    public void closesTheFactoryOnStopping() throws Exception {
        manager.stop();

        verify(factory).close();
    }

    @Test
    public void stopsTheDataSourceOnStopping() throws Exception {
        manager.stop();

        verify(dataSource).stop();
    }

    @Test
    public void startsTheDataSourceOnStarting() throws Exception {
        manager.start();

        verify(dataSource).start();
    }
}