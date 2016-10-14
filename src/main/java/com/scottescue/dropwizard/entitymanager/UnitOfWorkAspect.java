package com.scottescue.dropwizard.entitymanager;

import org.hibernate.jpa.HibernateEntityManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.util.Map;

/**
 * An aspect providing operations around a method with the {@link UnitOfWork} annotation.
 * It makes an EntityManager available and optionally creates a transaction.
 * <p>An aspect should be created for every invocation of the method.</p>
 */
class UnitOfWorkAspect {

    final private Map<String, EntityManagerFactory> entityManagerFactories;

    UnitOfWorkAspect(Map<String, EntityManagerFactory> entityManagerFactories) {
        this.entityManagerFactories = entityManagerFactories;
    }

    // Context variables
    private UnitOfWork unitOfWork;
    private EntityManager entityManager;

    public void beforeStart(UnitOfWork unitOfWork) {
        if (unitOfWork == null) {
            return;
        }
        this.unitOfWork = unitOfWork;

        EntityManagerFactory entityManagerFactory = entityManagerFactories.get(unitOfWork.value());
        if (entityManagerFactory == null) {
            // If the user didn't specify the name of a entityManager factory,
            // and we have only one registered, we can assume that it's the right one.
            if (unitOfWork.value().equals(EntityManagerBundle.DEFAULT_NAME) && entityManagerFactories.size() == 1) {
                entityManagerFactory = entityManagerFactories.values().iterator().next();
            } else {
                throw new IllegalArgumentException("Unregistered EntityManager bundle: '" + unitOfWork.value() + "'");
            }
        }
        entityManager = entityManagerFactory.createEntityManager();
        try {
            configureSession();
            EntityManagerContext.bind(entityManager);
            beginTransaction();
        } catch (Throwable th) {
            entityManager = null;
            throw th;
        }
    }

    public void afterEnd() {
        if (entityManager == null) {
            return;
        }

        try {
            commitTransaction();
        } catch (Exception e) {
            rollbackTransaction();
            throw e;
        }
        // The entityManager should not be closed to let lazy loading work when serializing a response to the client.
        // If the response is successfully serialized, then the entityManager will be closed by the `onFinish` method
    }

    public void onError() {
        if (entityManager == null) {
            return;
        }

        try {
            rollbackTransaction();
        } finally {
            entityManager = null;
        }
    }

    private void configureSession() {
        HibernateEntityManager em = (HibernateEntityManager) this.entityManager;
        em.getSession().setDefaultReadOnly(this.unitOfWork.readOnly());
        em.getSession().setCacheMode(this.unitOfWork.cacheMode());
        em.getSession().setFlushMode(this.unitOfWork.flushMode());
    }

    private void beginTransaction() {
        if (this.unitOfWork.transactional()) {
            this.entityManager.getTransaction().begin();
        }
    }

    private void rollbackTransaction() {
        if (this.unitOfWork.transactional()) {
            final EntityTransaction txn = this.entityManager.getTransaction();
            if (txn != null && txn.isActive()) {
                txn.rollback();
            }
        }
    }

    private void commitTransaction() {
        if (this.unitOfWork.transactional()) {
            final EntityTransaction txn = this.entityManager.getTransaction();
            if (txn != null && txn.isActive()) {
                txn.commit();
            }
        }
    }
}
