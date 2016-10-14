package com.scottescue.dropwizard.entitymanager;

import com.google.common.collect.ImmutableList;
import com.scottescue.dropwizard.entitymanager.entity.Dog;
import com.scottescue.dropwizard.entitymanager.entity.Person;
import io.dropwizard.jersey.errors.ErrorMessage;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceException;
import javax.ws.rs.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class LazyLoadingUnitOfWorkApplicationListenerTest extends AbstractIntegrationTest {

    public static class TestApplication extends AbstractTestApplication {
        @Override
        protected ImmutableList<Class<?>> supportedEntities() {
            return ImmutableList.of(Person.class, Dog.class);
        }

        @Override
        protected void onRun(TestConfiguration configuration, Environment environment) throws Exception {
            environment.jersey().register(new DogResource(new DogService(entityManagerBundle.getSharedEntityManager())));
            environment.jersey().register(new PersistenceExceptionMapper());
        }

        @Override
        protected void onInitDatabase(EntityManager entityManager) {
            entityManager.createNativeQuery(
                    "CREATE TABLE people (name varchar(100) primary key, email varchar(16), birthday timestamp with time zone)")
                    .executeUpdate();
            entityManager.createNativeQuery(
                    "INSERT INTO people VALUES ('Coda', 'coda@example.com', '1979-01-02 00:22:00+0:00')")
                    .executeUpdate();
            entityManager.createNativeQuery(
                    "CREATE TABLE dogs (name varchar(100) primary key, owner varchar(100), CONSTRAINT fk_owner FOREIGN KEY (owner) REFERENCES people(name))")
                    .executeUpdate();
            entityManager.createNativeQuery(
                    "INSERT INTO dogs VALUES ('Raf', 'Coda')")
                    .executeUpdate();
        }
    }

    public static class TestApplicationWithDisabledLazyLoading extends TestApplication {
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

        Optional<Dog> findByName(String name) {
            return Optional.ofNullable(entityManager.find(Dog.class, name));
        }

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
        @UnitOfWork(readOnly = true)
        public Optional<Dog> find(@PathParam("name") String name) {
            return service.findByName(name);
        }

        @PUT
        @UnitOfWork
        public void create(Dog dog) {
            service.create(dog);
        }
    }

    public static class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {
        @Override
        public Response toResponse(PersistenceException e) {
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
            Throwable throwable = unwrapThrowable(SQLIntegrityConstraintViolationException.class, e);
            String message = throwable == null ? "" : throwable.getMessage();

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(), message))
                    .build();
        }
    }

    @Test
    public void serialisesLazyObjectWhenEnabled() throws Exception {
        setupSerialisesLazyObjectWhenEnabled();

        final Dog raf = client.target(getUrlPrefix() + "/dogs/Raf").request(MediaType.APPLICATION_JSON).get(Dog.class);

        assertThat(raf.getName())
                .isEqualTo("Raf");

        assertThat(raf.getOwner())
                .isNotNull();

        assertThat(raf.getOwner().getName())
                .isEqualTo("Coda");
    }

    @Test
    public void sendsNullWhenDisabled() throws Exception {
        setupSendsNullWhenDisabled();

        final Dog raf = client.target(getUrlPrefix() + "/dogs/Raf").request(MediaType.APPLICATION_JSON).get(Dog.class);

        assertThat(raf.getName())
                .isEqualTo("Raf");

        assertThat(raf.getOwner())
                .isNull();
    }

    @Test
    public void returnsErrorsWhenEnabled() throws Exception {
        setupReturnsErrorsWhenEnabled();

        final Dog raf = new Dog();
        raf.setName("Raf");

        // Raf already exists so this should cause a primary key constraint violation
        final Response response = client.target(getUrlPrefix() + "/dogs/Raf").request().put(Entity.entity(raf, MediaType.APPLICATION_JSON));
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.BAD_REQUEST);
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.readEntity(ErrorMessage.class).getMessage()).contains("unique constraint", "table: DOGS");
    }

    protected void setupSerialisesLazyObjectWhenEnabled() {
        setup(TestApplication.class);
    }

    protected void setupSendsNullWhenDisabled() {
        setup(TestApplicationWithDisabledLazyLoading.class);
    }

    protected void setupReturnsErrorsWhenEnabled() {
        setup(TestApplication.class);
    }
}
