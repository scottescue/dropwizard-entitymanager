package com.scottescue.dropwizard.entitymanager;

import io.dropwizard.Configuration;
import io.dropwizard.hibernate.ScanningHibernateBundle;

/**
 * Extension of EntityManagerBundle that scans given package for entities instead of giving them by hand.
 */
public abstract class ScanningEntityManagerBundle<T extends Configuration> extends EntityManagerBundle<T> {
    /**
     * @param pckg string with package containing JPA entities (classes annotated with {@code @Entity}
     *             annotation) e. g. {@code com.my.application.directory.entities}
     */
    protected ScanningEntityManagerBundle(String pckg) {
        this(pckg, new EntityManagerFactoryFactory());
    }

    protected ScanningEntityManagerBundle(String pckg, EntityManagerFactoryFactory entityManagerFactoryFactory) {
        super(ScanningHibernateBundle.findEntityClassesFromDirectory(pckg), entityManagerFactoryFactory);
    }
}
