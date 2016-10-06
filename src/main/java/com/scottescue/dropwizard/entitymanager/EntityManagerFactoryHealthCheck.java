package com.scottescue.dropwizard.entitymanager;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.util.concurrent.MoreExecutors;
import io.dropwizard.db.TimeBoundHealthCheck;
import io.dropwizard.util.Duration;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.util.concurrent.ExecutorService;

class EntityManagerFactoryHealthCheck extends HealthCheck {
    private final EntityManagerFactory entityManagerFactory;
    private final String validationQuery;
    private final TimeBoundHealthCheck timeBoundHealthCheck;

    EntityManagerFactoryHealthCheck(EntityManagerFactory entityManagerFactory,
                                           String validationQuery) {
        this(MoreExecutors.newDirectExecutorService(), Duration.seconds(0), entityManagerFactory, validationQuery);
    }

    EntityManagerFactoryHealthCheck(ExecutorService executorService,
                                           Duration duration,
                                           EntityManagerFactory entityManagerFactory,
                                           String validationQuery) {
        this.entityManagerFactory = entityManagerFactory;
        this.validationQuery = validationQuery;
        this.timeBoundHealthCheck = new TimeBoundHealthCheck(executorService, duration);
    }


    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public String getValidationQuery() {
        return validationQuery;
    }

    @Override
    protected HealthCheck.Result check() throws Exception {
        return timeBoundHealthCheck.check(() -> {
            final EntityManager entityManager = entityManagerFactory.createEntityManager();
            try {
                final EntityTransaction entityTransaction = entityManager.getTransaction();
                entityTransaction.begin();
                try {
                    entityManager.createNativeQuery(validationQuery).getResultList();
                    entityTransaction.commit();
                } catch (Exception e) {
                    if (entityTransaction.isActive()) {
                        entityTransaction.rollback();
                    }
                    throw e;
                }
            } finally {
                entityManager.close();
            }
            return Result.healthy();
        });
    }
}
