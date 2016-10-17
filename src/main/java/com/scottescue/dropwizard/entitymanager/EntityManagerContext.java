package com.scottescue.dropwizard.entitymanager;

import com.google.common.annotations.VisibleForTesting;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents the notion of contextual {@link EntityManager} instances managed by
 * a {@link UnitOfWorkApplicationListener}. {@link UnitOfWorkApplicationListener}
 * is responsible for scoping these contextual entity managers appropriately
 * binding/unbinding them here for exposure to the application through
 * {@link EntityManager} proxy calls.
 * <p/>
 * The underlying storage of the current EntityManagers here is a static
 * {@link ThreadLocal}-based map where the EntityManagers are keyed by the
 * the owning {@link EntityManagerFactory}.
 */
class EntityManagerContext {
    private static final ThreadLocal<Map<EntityManagerFactory,EntityManager>> CONTEXT_TL = new ThreadLocal<>();

    private final EntityManagerFactory factory;

    /**
     * Constructs a new EntityManagerContext
     *
     * @param factory the factory this context will service
     */
    EntityManagerContext(EntityManagerFactory factory) {
        this.factory = factory;
    }

    EntityManager currentEntityManager() throws PersistenceException {
        final EntityManager current = existingEntityManager( this.factory );
        if ( current == null ) {
            throw new PersistenceException( "No EntityManager currently bound to execution context" );
        }
        return current;
    }

    /**
     * Check to see if there is already an EntityManager associated with the current
     * thread for the given EntityManagerFactory.
     *
     * @param factory the factory against which to check for a given EntityManager
     * within the current thread.
     * @return true if there is currently an EntityManager bound.
     */
    static boolean hasBind(EntityManagerFactory factory) {
        return existingEntityManager( factory ) != null;
    }

    /**
     * Binds the given EntityManager to the current context for its EntityManagerFactory.
     *
     * @param entityManager the EntityManager to be bound.
     * @return any previously bound EntityManager (should be null in most cases).
     */
    static EntityManager bind(EntityManager entityManager) {
        return entityManagerMap( true ).put( entityManager.getEntityManagerFactory(), entityManager );
    }

    /**
     * Unbinds the EntityManager, if any, currently associated with the context for the
     * given EntityManagerFactory.
     *
     * @param factory the factory for which to unbind the current EntityManager.
     * @return the bound entity manager, if any; else null.
     */
    static EntityManager unbind(EntityManagerFactory factory) {
        final Map<EntityManagerFactory,EntityManager> entityManagerMap = entityManagerMap(false);
        EntityManager existing = null;
        if ( entityManagerMap != null ) {
            existing = entityManagerMap.remove( factory );
            doCleanup();
        }
        return existing;
    }

    /**
     * Unbinds all EntityManagers, regardless of EntityManagerFactory, currently associated with the context.
     *
     * @param function the function to apply to each EntityManager removed
     */
    static void unBindAll(Consumer<EntityManager> function) {
        final Map<EntityManagerFactory,EntityManager> entityManagerMap = entityManagerMap(false);
        if ( entityManagerMap != null ) {
            Iterator<EntityManager> iterator = entityManagerMap.values().iterator();
            while (iterator.hasNext()) {
                EntityManager entityManager = iterator.next();
                function.accept(entityManager);
                iterator.remove();
            }
            doCleanup();
        }
    }

    @VisibleForTesting
    static synchronized Map<EntityManagerFactory,EntityManager> entityManagerMap(boolean createMap) {
        Map<EntityManagerFactory,EntityManager> entityManagerMap = CONTEXT_TL.get();
        if ( entityManagerMap == null && createMap ) {
            entityManagerMap = new HashMap<>();
            CONTEXT_TL.set( entityManagerMap );
        }
        return entityManagerMap;
    }

    private static EntityManager existingEntityManager(EntityManagerFactory factory) {
        final Map entityManagerMap = entityManagerMap(false);
        if ( entityManagerMap == null ) {
            return null;
        }
        else {
            return (EntityManager) entityManagerMap.get( factory );
        }
    }

    private static synchronized void doCleanup() {
        final Map<EntityManagerFactory,EntityManager> entityManagerMap = entityManagerMap( false );
        if ( entityManagerMap != null ) {
            if ( entityManagerMap.isEmpty() ) {
                CONTEXT_TL.set( null );
            }
        }
    }
}
