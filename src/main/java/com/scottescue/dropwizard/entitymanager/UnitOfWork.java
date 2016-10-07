package com.scottescue.dropwizard.entitymanager;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * When annotating a Jersey resource method, makes a Hibernate EntityManager available
 * to the method's execution context (thread).
 *
 * @see com.scottescue.dropwizard.entitymanager.UnitOfWorkAwareProxyFactory
 */
@Target(METHOD)
@Retention(RUNTIME)
@Documented
public @interface UnitOfWork {
    /**
     * If {@code true}, the Hibernate EntityManager will default to loading read-only entities.
     *
     * @see org.hibernate.Session#setDefaultReadOnly(boolean)
     */
    boolean readOnly() default false;

    /**
     * If {@code true}, a transaction will be automatically started before the resource method is
     * invoked, committed if the method returned, and rolled back if an exception was thrown.
     */
    boolean transactional() default true;

    /**
     * The {@link CacheMode} for the Hibernate EntityManager.
     *
     * @see CacheMode
     * @see org.hibernate.Session#setCacheMode(CacheMode)
     */
    CacheMode cacheMode() default CacheMode.NORMAL;

    /**
     * The {@link FlushMode} for the Hibernate EntityManager.
     *
     * @see FlushMode
     * @see org.hibernate.Session#setFlushMode(org.hibernate.FlushMode)
     */
    FlushMode flushMode() default FlushMode.AUTO;

    /**
     * The name of an EntityManager bundle (EntityManagerFactory) that specifies
     * a datasource against which a transaction will be opened.
     */
    String value() default EntityManagerBundle.DEFAULT_NAME;
}

