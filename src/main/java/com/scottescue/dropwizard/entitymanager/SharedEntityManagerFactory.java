package com.scottescue.dropwizard.entitymanager;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TransactionRequiredException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

/**
 * Factory for creating a shareable JPA {@link javax.persistence.EntityManager}
 * for a given {@link EntityManagerContext}.
 *
 * <p>A shared EntityManager will behave just like an EntityManager fetched from
 * an application server's JNDI environment, as defined by the JPA specification.
 * It will delegate all calls to the execution context's current EntityManager,
 * if any.
 */
class SharedEntityManagerFactory {

    private static final Set<String> transactionRequiringMethods = new HashSet<String>() {{
        add("joinTransaction");
        add("flush");
        add("persist");
        add("merge");
        add("remove");
        add("refresh");
    }};

    /**
     * Create an EntityManager proxy for the given EntityManagerContext.
     *
     * @param entityManagerContext the EntityManagerContext responsible for fetching the EntityManager delegate
     * @return a shareable EntityManager proxy
     */
    EntityManager build(EntityManagerContext entityManagerContext) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return (EntityManager) Proxy.newProxyInstance(
                classLoader,
                new Class[]{EntityManager.class},
                new SharedEntityManagerInvocationHandler(entityManagerContext));
    }

    /**
     * Invocation handler that delegates all calls to the EntityManager
     * bound to the current execution context, if any; else, an
     * IllegalStateException will be thrown
     */
    private static class SharedEntityManagerInvocationHandler implements InvocationHandler, Serializable {

        private final EntityManagerContext entityManagerContext;

        public SharedEntityManagerInvocationHandler(EntityManagerContext entityManagerContext) {
            this.entityManagerContext = entityManagerContext;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "equals":
                    // Only consider equal when proxies are identical.
                    return (proxy == args[0]);
                case "hashCode":
                    // Use hashCode of EntityManager proxy.
                    return hashCode();
                case "unwrap":
                    // JPA 2.0: handle unwrap method - could be a proxy match.
                    Class<?> targetClass = (Class<?>) args[0];
                    if (targetClass == null) {
                        return currentEntityManager();
                    } else if (targetClass.isInstance(proxy)) {
                        return proxy;
                    }
                    break;
                case "isOpen":
                    // Handle isOpen method: always return true.
                    return true;
                case "close":
                    // Handle close method: suppress, not valid.
                    return null;
                case "getTransaction":
                    throw new IllegalStateException(
                            "Not allowed to create transaction on shared EntityManager - " +
                                    "use @UnitOfWork instead");
            }

            // Retrieve the EntityManager bound to the current execution context
            EntityManager target = currentEntityManager();

            if (transactionRequiringMethods.contains(method.getName())) {
                // We need a transactional target now, according to the JPA spec.
                // Otherwise, the operation would get accepted but remain un-flushed...
                if (!hasActiveTransaction(target)) {
                    throw new TransactionRequiredException("No EntityManager with actual transaction available " +
                            "for current thread - cannot reliably process '" + method.getName() + "' call");
                }
            }

            // Invoke method on current EntityManager.
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }

        private EntityManager currentEntityManager()  {
            // Retrieve the EntityManager bound to the current execution context;
            // A PersistenceException is thrown if no EntityManager is bound
            return entityManagerContext.currentEntityManager();
        }

        private boolean hasActiveTransaction(EntityManager target) {
            EntityTransaction transaction = target.getTransaction();
            return transaction != null && transaction.isActive();
        }
    }

}
