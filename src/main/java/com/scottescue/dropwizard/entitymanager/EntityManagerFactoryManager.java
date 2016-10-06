package com.scottescue.dropwizard.entitymanager;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.lifecycle.Managed;

import javax.persistence.EntityManagerFactory;

class EntityManagerFactoryManager implements Managed {
    private EntityManagerFactory factory;
    private ManagedDataSource dataSource;

    EntityManagerFactoryManager(EntityManagerFactory factory, ManagedDataSource dataSource) {
        this.factory = factory;
        this.dataSource = dataSource;
    }

    @VisibleForTesting
    ManagedDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void start() throws Exception {
        dataSource.start();
    }

    @Override
    public void stop() throws Exception {
        factory.close();
        dataSource.stop();
    }
}
