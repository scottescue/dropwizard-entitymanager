package com.scottescue.dropwizard.entitymanager;

import io.dropwizard.hibernate.UnitOfWork;
import org.glassfish.jersey.server.internal.process.MappableException;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.hibernate.jpa.HibernateEntityManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


/**
 * An application event listener that listens for Jersey application initialization to
 * be finished, then creates a map of resource methods that have UnitOfWork annotations.
 *
 * Finally, it returns a {@link RequestEventListener} that listens for method events,
 * The RequestEventListener ensures an EntityManager is made available to the execution
 * context at method start, and ensures the EntityManager is removed at method completion.
 * The RequestEventListener creates and manages a transaction if required by the UnitOfWork
 * annotation.
 */
@Provider
public class UnitOfWorkApplicationListener implements ApplicationEventListener {

    final private Map<Method, UnitOfWork> methodMap = new HashMap<>();
    final private Map<String, EntityManagerFactory> entityManagerFactories = new HashMap<>();

    public UnitOfWorkApplicationListener() {
    }

    /**
     * Construct an application event listener using the given name and EntityManagerFactory.
     *
     * <p/>
     * When using this constructor, the {@link UnitOfWorkApplicationListener}
     * should be added to a Jersey {@code ResourceConfig} as a singleton.
     *
     * @param name a name of a EntityManager bundle
     * @param entityManagerFactory a {@link EntityManagerFactory}
     */
    public UnitOfWorkApplicationListener(String name, EntityManagerFactory entityManagerFactory) {
        registerEntityManagerFactory(name, entityManagerFactory);
    }

    /**
     * Register a EntityManagerFactory with the given name.
     *
     * @param name a name of an EntityManager bundle
     * @param entityManagerFactory a {@link EntityManagerFactory}
     */
    public void registerEntityManagerFactory(String name, EntityManagerFactory entityManagerFactory) {
        entityManagerFactories.put(name, entityManagerFactory);
    }

    private static class UnitOfWorkEventListener implements RequestEventListener {
        private final Map<Method, UnitOfWork> methodMap;
        private final Map<String, EntityManagerFactory> entityManagerFactories;

        private UnitOfWork unitOfWork;
        private EntityManager entityManager;
        private EntityManagerFactory entityManagerFactory;

        public UnitOfWorkEventListener(Map<Method, UnitOfWork> methodMap,
                                       Map<String, EntityManagerFactory> entityManagerFactories) {
            this.methodMap = methodMap;
            this.entityManagerFactories = entityManagerFactories;
        }

        @Override
        public void onEvent(RequestEvent event) {
            if (event.getType() == RequestEvent.Type.RESOURCE_METHOD_START) {
                this.unitOfWork = this.methodMap.get(event.getUriInfo()
                        .getMatchedResourceMethod().getInvocable().getDefinitionMethod());
                if (unitOfWork != null) {
                    entityManagerFactory = entityManagerFactories.get(unitOfWork.value());
                    if (entityManagerFactory == null) {
                        // If the user didn't specify the name of a entityManager factory,
                        // and we have only one registered, we can assume that it's the right one.
                        if (unitOfWork.value().equals(EntityManagerBundle.DEFAULT_NAME) && entityManagerFactories.size() == 1) {
                            entityManagerFactory = entityManagerFactories.values().iterator().next();
                        } else {
                            throw new IllegalArgumentException("Unregistered EntityManager bundle: '" +
                                    unitOfWork.value() + "'");
                        }
                    }
                    this.entityManager = this.entityManagerFactory.createEntityManager();
                    try {
                        configureEntityManager();
                        EntityManagerContext.bind(this.entityManager);
                        beginTransaction();
                    } catch (Throwable th) {
                        this.entityManager.close();
                        this.entityManager = null;
                        EntityManagerContext.unbind(this.entityManagerFactory);
                        throw th;
                    }
                }
            } else if (event.getType() == RequestEvent.Type.RESP_FILTERS_START) {
                if (this.entityManager != null) {
                    try {
                        commitTransaction();
                    } catch (Exception e) {
                        rollbackTransaction();
                        throw new MappableException(e);
                    } finally {
                        this.entityManager.close();
                        this.entityManager = null;
                        EntityManagerContext.unbind(this.entityManagerFactory);
                    }
                }
            } else if (event.getType() == RequestEvent.Type.ON_EXCEPTION) {
                if (this.entityManager != null) {
                    try {
                        rollbackTransaction();
                    } finally {
                        this.entityManager.close();
                        this.entityManager = null;
                        EntityManagerContext.unbind(this.entityManagerFactory);
                    }
                }
            }
        }

        private void beginTransaction() {
            if (this.unitOfWork.transactional()) {
                this.entityManager.getTransaction().begin();
            }
        }

        private void configureEntityManager() {
            HibernateEntityManager em = (HibernateEntityManager) this.entityManager;
            em.getSession().setDefaultReadOnly(this.unitOfWork.readOnly());
            em.getSession().setCacheMode(this.unitOfWork.cacheMode());
            em.getSession().setFlushMode(this.unitOfWork.flushMode());
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

    @Override
    public void onEvent(ApplicationEvent event) {
        if (event.getType() == ApplicationEvent.Type.INITIALIZATION_APP_FINISHED) {
            for (Resource resource : event.getResourceModel().getResources()) {
                for (ResourceMethod method : resource.getAllMethods()) {
                    registerUnitOfWorkAnnotations(method);
                }

                for (Resource childResource : resource.getChildResources()) {
                    for (ResourceMethod method : childResource.getAllMethods()) {
                        registerUnitOfWorkAnnotations(method);
                    }
                }
            }
        }
    }

    @Override
    public RequestEventListener onRequest(RequestEvent event) {
        return new UnitOfWorkEventListener(methodMap, entityManagerFactories);
    }

    private void registerUnitOfWorkAnnotations(ResourceMethod method) {
        UnitOfWork annotation = method.getInvocable().getDefinitionMethod().getAnnotation(UnitOfWork.class);

        if (annotation == null) {
            annotation = method.getInvocable().getHandlingMethod().getAnnotation(UnitOfWork.class);
        }

        if (annotation != null) {
            this.methodMap.put(method.getInvocable().getDefinitionMethod(), annotation);
        }

    }
}
