package com.scottescue.dropwizard.entitymanager;

import org.glassfish.jersey.server.internal.process.MappableException;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
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
class UnitOfWorkApplicationListener implements ApplicationEventListener {

    final private Map<Method, UnitOfWork> methodMap = new HashMap<>();
    final private Map<String, EntityManagerFactory> entityManagerFactories = new HashMap<>();

    UnitOfWorkApplicationListener() {
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
    UnitOfWorkApplicationListener(String name, EntityManagerFactory entityManagerFactory) {
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
        private final UnitOfWorkAspect unitOfWorkAspect;

        public UnitOfWorkEventListener(Map<Method, UnitOfWork> methodMap,
                                       Map<String, EntityManagerFactory> entityManagerFactories) {
            this.methodMap = methodMap;
            this.unitOfWorkAspect = new UnitOfWorkAspect(entityManagerFactories);
        }

        @Override
        public void onEvent(RequestEvent event) {
            final RequestEvent.Type eventType = event.getType();
            if (eventType == RequestEvent.Type.RESOURCE_METHOD_START) {
                UnitOfWork unitOfWork = methodMap.get(event.getUriInfo()
                        .getMatchedResourceMethod().getInvocable().getDefinitionMethod());
                unitOfWorkAspect.beforeStart(unitOfWork);
            } else if (eventType == RequestEvent.Type.RESP_FILTERS_START) {
                try {
                    unitOfWorkAspect.afterEnd();
                } catch (Exception e) {
                    throw new MappableException(e);
                }
            } else if (eventType == RequestEvent.Type.ON_EXCEPTION) {
                unitOfWorkAspect.onError();
            } else if (eventType == RequestEvent.Type.FINISHED) {
                EntityManagerContext.unBindAll(EntityManager::close);
            }
        }
    }

    @Override
    public void onEvent(ApplicationEvent event) {
        if (event.getType() == ApplicationEvent.Type.INITIALIZATION_APP_FINISHED) {
            for (Resource resource : event.getResourceModel().getResources()) {
                resource.getAllMethods().forEach(this::registerUnitOfWorkAnnotations);

                for (Resource childResource : resource.getChildResources()) {
                    childResource.getAllMethods().forEach(this::registerUnitOfWorkAnnotations);
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
