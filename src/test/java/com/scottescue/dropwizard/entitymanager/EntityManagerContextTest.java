package com.scottescue.dropwizard.entitymanager;

import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EntityManagerContextTest {
    final private ExecutorService executor = Executors.newCachedThreadPool();

    @Test
    public void exposesEntityManagerToThread() {
        EntityManager em = createEntityManager();
        EntityManagerContext.bind(em);

        Map<EntityManagerFactory, EntityManager> map = EntityManagerContext.entityManagerMap(false);
        assertThat(map).isNotNull();
        assertThat(map.get(em.getEntityManagerFactory())).isSameAs(em);
    }

    @Test
    public void exposesCurrentEntityManagerToThread() {
        EntityManager em = createEntityManager();
        Map<EntityManagerFactory, EntityManager> map = EntityManagerContext.entityManagerMap(true);
        map.put(em.getEntityManagerFactory(), em);

        EntityManagerContext entityManagerContext = new EntityManagerContext(em.getEntityManagerFactory());
        assertThat(entityManagerContext.currentEntityManager()).isSameAs(em);
    }

    @Test
    public void hidesEntityManagerOutsideThread() {
        final EntityManager em = createEntityManager();
        EntityManagerContext.bind(em);

        Boolean hasEntityManager = invoke(() -> {
            Map<EntityManagerFactory, EntityManager> map = EntityManagerContext.entityManagerMap(false);
            return map != null && map.get(em.getEntityManagerFactory()) == em;
        });

        assertThat(hasEntityManager.booleanValue()).isFalse();
    }

    @Test
    public void hidesCurrentEntityManagerOutsideThread() {
        final EntityManager em = createEntityManager();
        Map<EntityManagerFactory, EntityManager> map = EntityManagerContext.entityManagerMap(true);
        map.put(em.getEntityManagerFactory(), em);

        Throwable throwable = invoke((Callable<Throwable>) () -> {
            try {
                EntityManagerContext entityManagerContext = new EntityManagerContext(em.getEntityManagerFactory());
                entityManagerContext.currentEntityManager();
            } catch(PersistenceException e) {
                return e;
            }
            return null;
        });

        assertThat(throwable).isInstanceOf(PersistenceException.class);
    }

    @Test
    public void removesEntityManagerFromThread() {
        EntityManager em = createEntityManager();
        Map<EntityManagerFactory, EntityManager> map = EntityManagerContext.entityManagerMap(true);
        map.put(em.getEntityManagerFactory(), em);
        assertThat(EntityManagerContext.hasBind(em.getEntityManagerFactory())).isTrue();

        EntityManagerContext.unbind(em.getEntityManagerFactory());
        assertThat(map.get(em.getEntityManagerFactory())).isNull();
    }

    private EntityManager createEntityManager() {
        EntityManagerFactory emf = mock(EntityManagerFactory.class);
        EntityManager em = mock(EntityManager.class);
        when(em.getEntityManagerFactory()).thenReturn(emf);

        return em;
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(final Callable<T> callable) {
        try {
            List<Future<T>> results = executor.invokeAll(new ArrayList(1){{
                add(callable);
            }});
            return results.get(0).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}