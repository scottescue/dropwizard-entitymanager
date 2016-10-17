package com.scottescue.dropwizard.entitymanager;

import com.scottescue.dropwizard.entitymanager.entity.Dog;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class LazyLoadingUnitOfWorkAwareProxyFactoryTest extends LazyLoadingUnitOfWorkApplicationListenerTest {

    public static class ProxyFactoryTestApplication extends LazyLoadingUnitOfWorkApplicationListenerTest.TestApplication {
        @Override
        public void onRun(TestConfiguration configuration, Environment environment) throws Exception {
            UnitOfWorkAwareProxyFactory proxyFactory = new UnitOfWorkAwareProxyFactory(entityManagerBundle);
            DogService dogService = proxyFactory.create(DogService.class,
                    EntityManager.class, entityManagerBundle.getSharedEntityManager());

            environment.jersey().register(new DogResource(dogService));
            environment.jersey().register(new LazyLoadingUnitOfWorkApplicationListenerTest.PersistenceExceptionMapper());
        }
    }

    public static class ProxyFactoryTestApplicationWithLazyLoadingDisabled extends ProxyFactoryTestApplication {
        @Override
        public void initialize(Bootstrap<TestConfiguration> bootstrap) {
            entityManagerBundle.setSerializeLazyLoadedEntitiesEnabled(false);
            bootstrap.addBundle(entityManagerBundle);
        }
    }


    public static class DogService {
        private final EntityManager entityManager;

        DogService(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        @UnitOfWork(transactional = false)
        Optional<Dog> findByName(String name) {
            return Optional.ofNullable(entityManager.find(Dog.class, name));
        }

        @UnitOfWork
        Dog create(Dog dog) throws PersistenceException {
            entityManager.setFlushMode(FlushModeType.COMMIT);
            entityManager.persist(requireNonNull(dog));
            return dog;
        }
    }

    @Path("/dogs/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public static class DogResource {
        private final DogService service;

        DogResource(DogService service) {
            this.service = service;
        }

        @GET
        public Optional<Dog> find(@PathParam("name") String name) {
            return service.findByName(name);
        }

        @PUT
        public void create(Dog dog) {
            service.create(dog);
        }
    }

    protected void setupSerialisesLazyObjectWhenEnabled() {
        setup(ProxyFactoryTestApplication.class);
    }

    protected void setupSendsNullWhenDisabled() {
        setup(ProxyFactoryTestApplicationWithLazyLoadingDisabled.class);
    }

    protected void setupReturnsErrorsWhenEnabled() {
        setup(ProxyFactoryTestApplication.class);
    }
}
