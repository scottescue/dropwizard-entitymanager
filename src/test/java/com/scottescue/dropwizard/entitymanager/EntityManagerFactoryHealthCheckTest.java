package com.scottescue.dropwizard.entitymanager;

import com.codahale.metrics.health.HealthCheck;
import org.junit.Test;
import org.mockito.InOrder;

import javax.persistence.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class EntityManagerFactoryHealthCheckTest {
    private final EntityManagerFactory factory = mock(EntityManagerFactory.class);
    private final EntityManagerFactoryHealthCheck healthCheck = new EntityManagerFactoryHealthCheck(factory,
            "SELECT 1");

    @Test
    public void hasAnEntityManagerFactory() throws Exception {
        assertThat(healthCheck.getEntityManagerFactory())
                .isEqualTo(factory);
    }

    @Test
    public void hasAValidationQuery() throws Exception {
        assertThat(healthCheck.getValidationQuery())
                .isEqualTo("SELECT 1");
    }

    @Test
    public void isHealthyIfNoExceptionIsThrown() throws Exception {
        final EntityManager entityManager = mock(EntityManager.class);
        when(factory.createEntityManager()).thenReturn(entityManager);

        final EntityTransaction transaction = mock(EntityTransaction.class);
        when(entityManager.getTransaction()).thenReturn(transaction);

        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);

        assertThat(healthCheck.execute())
                .isEqualTo(HealthCheck.Result.healthy());

        final InOrder inOrder = inOrder(factory, entityManager, transaction, query);
        inOrder.verify(factory).createEntityManager();
        inOrder.verify(entityManager).getTransaction();
        inOrder.verify(entityManager).createNativeQuery("SELECT 1");
        inOrder.verify(query).getResultList();
        inOrder.verify(transaction).commit();
        inOrder.verify(entityManager).close();
    }

    @Test
    public void isUnhealthyIfAnExceptionIsThrown() throws Exception {
        final EntityManager entityManager = mock(EntityManager.class);
        when(factory.createEntityManager()).thenReturn(entityManager);

        final EntityTransaction transaction = mock(EntityTransaction.class);
        when(entityManager.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(true);

        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenThrow(new PersistenceException("OH NOE"));

        assertThat(healthCheck.execute().isHealthy())
                .isFalse();

        final InOrder inOrder = inOrder(factory, entityManager, transaction, query);
        inOrder.verify(factory).createEntityManager();
        inOrder.verify(entityManager).getTransaction();
        inOrder.verify(entityManager).createNativeQuery("SELECT 1");
        inOrder.verify(query).getResultList();
        inOrder.verify(transaction).rollback();
        inOrder.verify(entityManager).close();

        verify(transaction, never()).commit();
    }
}