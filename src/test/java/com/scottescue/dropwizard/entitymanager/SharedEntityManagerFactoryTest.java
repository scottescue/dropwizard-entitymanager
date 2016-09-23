package com.scottescue.dropwizard.entitymanager;

import org.hibernate.jpa.HibernateEntityManager;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TransactionRequiredException;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


public class SharedEntityManagerFactoryTest {
    final private EntityManager entityManager = mock(EntityManager.class);
    final private EntityManagerContext entityManagerContext = mock(EntityManagerContext.class);
    final private SharedEntityManagerFactory sharedEntityManagerFactory = new SharedEntityManagerFactory();

    @Test(expected = TransactionRequiredException.class)
    public void requiresTransactionForPersist() {
        createProxy().persist(new Object());
    }

    @Test(expected = TransactionRequiredException.class)
    public void requiresTransactionForMerge() {
        createProxy().merge(new Object());
    }

    @Test(expected = TransactionRequiredException.class)
    public void requiresTransactionForRemove() {
        createProxy().remove(new Object());
    }

    @Test(expected = TransactionRequiredException.class)
    public void requiresTransactionForFlush() {
        createProxy().flush();
    }

    @Test(expected = TransactionRequiredException.class)
    public void requiresTransactionForRefresh() {
        createProxy().refresh(new Object());
    }

    @Test(expected = TransactionRequiredException.class)
    public void requiresTransactionForRefreshWithProperties() {
        createProxy().refresh(new Object(), new HashMap<>());
    }

    @Test(expected = TransactionRequiredException.class)
    public void requiresTransactionForRefreshWithLockMode() {
        createProxy().refresh(new Object(), LockModeType.NONE);
    }

    @Test(expected = TransactionRequiredException.class)
    public void requiresTransactionForRefreshWithPropertiesAndLockMode() {
        createProxy().refresh(new Object(), LockModeType.NONE, new HashMap<>());
    }

    @Test(expected = TransactionRequiredException.class)
    public void requiresTransactionForJoinTransaction() {
        createProxy().joinTransaction();
    }

    @Test
    public void unwrappingProxyReturnsProxy() {
        EntityManager entityManagerProxy = createProxy();
        EntityManager foundProxy = entityManagerProxy.unwrap(entityManagerProxy.getClass());
        assertThat(foundProxy).isSameAs(entityManagerProxy);
        verifyNoMoreInteractions(entityManager);
    }

    @Test
    public void unwrappingNullReturnsEntityManager() {
        EntityManager em = createProxy().unwrap(null);
        assertThat(em).isSameAs(entityManager);
    }

    @Test
    public void delegatesToUnwrapForNonProxyClass() {
        HibernateEntityManager mockHibernateEm = mock(HibernateEntityManager.class);
        when(entityManager.unwrap(HibernateEntityManager.class)).thenReturn(mockHibernateEm);

        HibernateEntityManager foundHibernateEm = createProxy().unwrap(HibernateEntityManager.class);
        verify(entityManager).unwrap(HibernateEntityManager.class);
        assertThat(foundHibernateEm).isSameAs(mockHibernateEm);
    }

    @Test
    public void doesNotDelegateToClose() {
        createProxy().close();
        verify(entityManager, never()).close();
    }

    @Test
    public void doesNotDelegateToIsOpen() {
        createProxy().isOpen();
        verify(entityManager, never()).isOpen();
    }

    @Test
    public void isAlwaysOpen() {
        assertThat(createProxy().isOpen()).isTrue();
    }

    @Test(expected = IllegalStateException.class)
    public void getTransactionThrowsException() {
        createProxy().getTransaction();
    }

    @Test
    public void delegatesToFind() {
        Class type = Class.class;
        Object primaryKey = new Object();
        createProxy().find(type, primaryKey);
        verify(entityManager).find(type, primaryKey);
    }

    private EntityManager createProxy() {
        when(entityManagerContext.currentEntityManager()).thenReturn(entityManager);
        return sharedEntityManagerFactory.build(entityManagerContext);
    }
}