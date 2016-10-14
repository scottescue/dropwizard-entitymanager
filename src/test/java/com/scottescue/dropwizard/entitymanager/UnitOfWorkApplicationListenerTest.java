package com.scottescue.dropwizard.entitymanager;

import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.jpa.HibernateEntityManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class UnitOfWorkApplicationListenerTest {
    private final EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
    private final EntityManagerFactory analyticsEntityManagerFactory = mock(EntityManagerFactory.class);
    private final UnitOfWorkApplicationListener listener = new UnitOfWorkApplicationListener();
    private final ApplicationEvent appEvent = mock(ApplicationEvent.class);
    private final ExtendedUriInfo uriInfo = mock(ExtendedUriInfo.class);

    private final RequestEvent requestStartEvent = mock(RequestEvent.class);
    private final RequestEvent requestMethodStartEvent = mock(RequestEvent.class);
    private final RequestEvent responseFiltersStartEvent = mock(RequestEvent.class);
    private final RequestEvent responseFinishedEvent = mock(RequestEvent.class);
    private final RequestEvent requestMethodExceptionEvent = mock(RequestEvent.class);
    private final HibernateEntityManager entityManager = mock(HibernateEntityManager.class);
    private final HibernateEntityManager analyticsEntityManager = mock(HibernateEntityManager.class);
    private final Session session = mock(Session.class);
    private final Session analyticsSession = mock(Session.class);
    private final EntityTransaction transaction = mock(EntityTransaction.class);
    private final EntityTransaction analyticsTransaction = mock(EntityTransaction.class);

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        listener.registerEntityManagerFactory(EntityManagerBundle.DEFAULT_NAME, entityManagerFactory);
        listener.registerEntityManagerFactory("analytics", analyticsEntityManagerFactory);

        when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
        when(entityManager.getEntityManagerFactory()).thenReturn(entityManagerFactory);
        when(entityManager.getSession()).thenReturn(session);
        when(entityManager.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(true);

        when(analyticsEntityManagerFactory.createEntityManager()).thenReturn(analyticsEntityManager);
        when(analyticsEntityManager.getEntityManagerFactory()).thenReturn(analyticsEntityManagerFactory);
        when(analyticsEntityManager.getSession()).thenReturn(analyticsSession);
        when(analyticsEntityManager.getTransaction()).thenReturn(analyticsTransaction);
        when(analyticsTransaction.isActive()).thenReturn(true);

        when(appEvent.getType()).thenReturn(ApplicationEvent.Type.INITIALIZATION_APP_FINISHED);
        when(requestMethodStartEvent.getType()).thenReturn(RequestEvent.Type.RESOURCE_METHOD_START);
        when(responseFiltersStartEvent.getType()).thenReturn(RequestEvent.Type.RESP_FILTERS_START);
        when(responseFinishedEvent.getType()).thenReturn(RequestEvent.Type.FINISHED);
        when(requestMethodExceptionEvent.getType()).thenReturn(RequestEvent.Type.ON_EXCEPTION);
        when(requestMethodStartEvent.getUriInfo()).thenReturn(uriInfo);
        when(responseFiltersStartEvent.getUriInfo()).thenReturn(uriInfo);
        when(responseFinishedEvent.getUriInfo()).thenReturn(uriInfo);
        when(requestMethodExceptionEvent.getUriInfo()).thenReturn(uriInfo);

        prepareAppEvent("methodWithDefaultAnnotation");
    }

    @Test
    public void opensAndClosesAnEntityManager() throws Exception {
        execute();

        final InOrder inOrder = inOrder(entityManagerFactory, entityManager);
        inOrder.verify(entityManagerFactory).createEntityManager();
        inOrder.verify(entityManager).close();
    }

    @Test
    public void bindsAndUnbindsTheEntityManagerToTheContext() throws Exception {
        doAnswer(invocation -> {
            assertThat(EntityManagerContext.hasBind(entityManagerFactory))
                    .isTrue();
            return null;
        }).when(transaction).begin();

        execute();

        assertThat(EntityManagerContext.hasBind(entityManagerFactory)).isFalse();
    }

    @Test
    public void closesAnyEntityManagerBoundToTheContext() throws Exception {
        final EntityManager otherEntityManager = mock(EntityManager.class);
        final EntityManagerFactory otherEntityManagerFactory = mock(EntityManagerFactory.class);
        when(otherEntityManagerFactory.createEntityManager()).thenReturn(otherEntityManager);
        when(otherEntityManager.getEntityManagerFactory()).thenReturn(otherEntityManagerFactory);

        EntityManagerContext.bind(entityManager);
        EntityManagerContext.bind(otherEntityManager);

        doAnswer(invocation -> {
            assertThat(EntityManagerContext.hasBind(entityManagerFactory)).isTrue();
            assertThat(EntityManagerContext.hasBind(otherEntityManagerFactory)).isTrue();
            return RequestEvent.Type.RESOURCE_METHOD_START;
        }).when(requestMethodStartEvent).getType();

        prepareAppEvent("methodNotAnnotated");
        execute();

        verify(entityManager).close();
        verify(otherEntityManager).close();

        assertThat(EntityManagerContext.hasBind(entityManagerFactory)).isFalse();
        assertThat(EntityManagerContext.hasBind(otherEntityManagerFactory)).isFalse();
    }

    @Test
    public void configuresTheEntityManagerReadOnlyDefault() throws Exception {
        prepareAppEvent("methodWithReadOnlyAnnotation");

        execute();

        verify(session).setDefaultReadOnly(true);
    }

    @Test
    public void configuresTheEntityManagersCacheMode() throws Exception {
        prepareAppEvent("methodWithCacheModeIgnoreAnnotation");

        execute();

        verify(session).setCacheMode(CacheMode.IGNORE);
    }

    @Test
    public void configuresTheEntityManagersFlushMode() throws Exception {
        prepareAppEvent("methodWithFlushModeAlwaysAnnotation");

        execute();

        verify(session).setFlushMode(FlushMode.ALWAYS);
    }

    @Test
    public void doesNotBeginATransactionIfNotTransactional() throws Exception {
        final String resourceMethodName = "methodWithTransactionalFalseAnnotation";
        prepareAppEvent(resourceMethodName);

        execute();

        verify(transaction, never()).begin();
        verifyNoMoreInteractions(transaction);
    }

    @Test
    public void detectsAnnotationOnHandlingMethod() throws NoSuchMethodException {
        final String resourceMethodName = "handlingMethodAnnotated";
        prepareAppEvent(resourceMethodName);

        execute();

        verify(session).setDefaultReadOnly(true);
    }

    @Test
    public void detectsAnnotationOnDefinitionMethod() throws NoSuchMethodException {
        final String resourceMethodName = "definitionMethodAnnotated";
        prepareAppEvent(resourceMethodName);

        execute();

        verify(session).setDefaultReadOnly(true);
    }

    @Test
    public void annotationOnDefinitionMethodOverridesHandlingMethod() throws NoSuchMethodException {
        final String resourceMethodName = "bothMethodsAnnotated";
        prepareAppEvent(resourceMethodName);

        execute();

        verify(session).setDefaultReadOnly(true);
    }

    @Test
    public void beginsAndCommitsATransactionIfTransactional() throws Exception {
        execute();

        final InOrder inOrder = inOrder(entityManager, transaction);
        inOrder.verify(entityManager).getTransaction();
        inOrder.verify(transaction).begin();
        inOrder.verify(transaction).commit();
        inOrder.verify(entityManager).close();
    }

    @Test
    public void rollsBackTheTransactionOnException() throws Exception {
        executeWithException();

        final InOrder inOrder = inOrder(entityManager, transaction);
        inOrder.verify(entityManager).getTransaction();
        inOrder.verify(transaction).begin();
        inOrder.verify(transaction).rollback();
        inOrder.verify(entityManager).close();
    }

    @Test
    public void doesNotCommitAnInactiveTransaction() throws Exception {
        when(transaction.isActive()).thenReturn(false);

        execute();

        verify(transaction, never()).commit();
    }

    @Test
    public void doesNotRollbackAnInactiveTransaction() throws Exception {
        when(transaction.isActive()).thenReturn(false);

        executeWithException();

        verify(transaction, never()).rollback();
    }

    @Test
    public void beginsAndCommitsATransactionForAnalytics() throws Exception {
        prepareAppEvent("methodWithUnitOfWorkOnAnalyticsDatabase");
        execute();

        final InOrder inOrder = inOrder(analyticsEntityManager, analyticsTransaction);
        inOrder.verify(analyticsEntityManager).getTransaction();
        inOrder.verify(analyticsTransaction).begin();
        inOrder.verify(analyticsTransaction).commit();
        inOrder.verify(analyticsEntityManager).close();
    }

    @Test
    public void throwsExceptionOnNotRegisteredDatabase() throws Exception {
        try {
            prepareAppEvent("methodWithUnitOfWorkOnNotRegisteredDatabase");
            execute();
            Assert.fail();
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(e.getMessage(), "Unregistered EntityManager bundle: 'warehouse'");
        }
    }

    private void prepareAppEvent(String resourceMethodName) throws NoSuchMethodException {
        final Resource.Builder builder = Resource.builder();
        final MockResource mockResource = new MockResource();
        final Method handlingMethod = mockResource.getClass().getMethod(resourceMethodName);

        Method definitionMethod = handlingMethod;
        Class<?> interfaceClass = mockResource.getClass().getInterfaces()[0];
        if (methodDefinedOnInterface(resourceMethodName, interfaceClass.getMethods())) {
            definitionMethod = interfaceClass.getMethod(resourceMethodName);
        }

        final ResourceMethod resourceMethod = builder.addMethod()
                .handlingMethod(handlingMethod)
                .handledBy(mockResource, definitionMethod).build();
        final Resource resource = builder.build();
        final ResourceModel model = new ResourceModel.Builder(false).addResource(resource).build();

        when(appEvent.getResourceModel()).thenReturn(model);
        when(uriInfo.getMatchedResourceMethod()).thenReturn(resourceMethod);
    }

    private boolean methodDefinedOnInterface(String methodName, Method[] methods) {
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return true;
            }
        }
        return false;
    }

    private void execute() {
        listener.onEvent(appEvent);
        RequestEventListener requestListener = listener.onRequest(requestStartEvent);
        requestListener.onEvent(requestMethodStartEvent);
        requestListener.onEvent(responseFiltersStartEvent);
        requestListener.onEvent(responseFinishedEvent);
    }

    private void executeWithException() {
        listener.onEvent(appEvent);
        RequestEventListener requestListener = listener.onRequest(requestStartEvent);
        requestListener.onEvent(requestMethodStartEvent);
        requestListener.onEvent(requestMethodExceptionEvent);
        requestListener.onEvent(responseFiltersStartEvent);
        requestListener.onEvent(responseFinishedEvent);
    }

    public static class MockResource implements MockResourceInterface {

        @UnitOfWork(readOnly = false, cacheMode = CacheMode.NORMAL, transactional = true, flushMode = FlushMode.AUTO)
        public void methodWithDefaultAnnotation() {
        }

        @UnitOfWork(readOnly = true, cacheMode = CacheMode.NORMAL, transactional = true, flushMode = FlushMode.AUTO)
        public void methodWithReadOnlyAnnotation() {
        }

        @UnitOfWork(readOnly = false, cacheMode = CacheMode.IGNORE, transactional = true, flushMode = FlushMode.AUTO)
        public void methodWithCacheModeIgnoreAnnotation() {
        }

        @UnitOfWork(readOnly = false, cacheMode = CacheMode.NORMAL, transactional = true, flushMode = FlushMode.ALWAYS)
        public void methodWithFlushModeAlwaysAnnotation() {
        }

        @UnitOfWork(readOnly = false, cacheMode = CacheMode.NORMAL, transactional = false, flushMode = FlushMode.AUTO)
        public void methodWithTransactionalFalseAnnotation() {
        }

        @UnitOfWork(readOnly = true)
        @Override
        public void handlingMethodAnnotated() {
        }

        @Override
        public void definitionMethodAnnotated() {
        }

        @UnitOfWork(readOnly = false)
        @Override
        public void bothMethodsAnnotated() {
        }

        @UnitOfWork("analytics")
        public void methodWithUnitOfWorkOnAnalyticsDatabase() {
        }

        @UnitOfWork("warehouse")
        public void methodWithUnitOfWorkOnNotRegisteredDatabase() {
        }

        public void methodNotAnnotated() {
        }
    }

    public interface MockResourceInterface {

        void handlingMethodAnnotated();

        @UnitOfWork(readOnly = true)
        void definitionMethodAnnotated();

        @UnitOfWork(readOnly = true)
        void bothMethodsAnnotated();
    }
}