package com.scottescue.dropwizard.entitymanager;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.scottescue.dropwizard.entitymanager.entity.Person;
import io.dropwizard.jersey.errors.ErrorMessage;
import io.dropwizard.setup.Environment;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.ws.rs.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.sql.SQLDataException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class JerseyIntegrationTest extends AbstractIntegrationTest {
    public static class TestApplication extends AbstractTestApplication {
        @Override
        protected ImmutableList<Class<?>> supportedEntities() {
            return ImmutableList.of(Person.class);
        }

        @Override
        public void onRun(TestConfiguration configuration, Environment environment) throws Exception {
            final EntityManager sharedEntityManager = entityManagerBundle.getSharedEntityManager();
            environment.jersey().register(new PersonResource(new PersonService(sharedEntityManager)));
            environment.jersey().register(new DataExceptionMapper());
        }

        @Override
        protected void onInitDatabase(EntityManager entityManager) {
            entityManager.createNativeQuery("DROP TABLE people IF EXISTS").executeUpdate();
            entityManager.createNativeQuery(
                    "CREATE TABLE people (name varchar(100) primary key, email varchar(16), birthday timestamp with time zone)")
                    .executeUpdate();
            entityManager.createNativeQuery(
                    "INSERT INTO people VALUES ('Coda', 'coda@example.com', '1979-01-02 00:22:00+0:00')")
                    .executeUpdate();
        }
    }

    public static class PersonService {
        private final EntityManager entityManager;
        public PersonService(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        public Optional<Person> findByName(String name) {
            return Optional.fromNullable(entityManager.find(Person.class, name));
        }

        public Person persist(Person entity) {
            entityManager.persist(entity);
            return entity;
        }
    }

    @Path("/people/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public static class PersonResource {
        private final PersonService service;

        public PersonResource(PersonService service) {
            this.service = service;
        }

        @GET
        @UnitOfWork(readOnly = true)
        public Optional<Person> find(@PathParam("name") String name) {
            return service.findByName(name);
        }

        @PUT
        @UnitOfWork
        public void save(Person person) {
            service.persist(person);
        }
    }

    public static class DataExceptionMapper implements ExceptionMapper<PersistenceException> {
        @Override
        public Response toResponse(PersistenceException e) {
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
            SQLDataException sqlException = unwrapThrowable(SQLDataException.class, e);

            String message = (sqlException != null && sqlException.getMessage().contains("EMAIL"))
                    ? "Wrong email"
                    : "Wrong input";

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(), message))
                    .build();
        }
    }

    @Before
    public void setup() {
        setup(TestApplication.class);
    }


    @Test
    public void findsExistingData() throws Exception {
        final Person coda = client.target(getUrl("/people/Coda")).request(MediaType.APPLICATION_JSON).get(Person.class);

        assertThat(coda.getName())
                .isEqualTo("Coda");

        assertThat(coda.getEmail())
                .isEqualTo("coda@example.com");

        assertThat(coda.getBirthday())
                .isEqualTo(new DateTime(1979, 1, 2, 0, 22, DateTimeZone.UTC));
    }

    @Test
    public void doesNotFindMissingData() throws Exception {
        try {
            client.target(getUrl("/people/Poof")).request(MediaType.APPLICATION_JSON)
                    .get(Person.class);
            failBecauseExceptionWasNotThrown(WebApplicationException.class);
        } catch (WebApplicationException e) {
            assertThat(e.getResponse().getStatus())
                    .isEqualTo(404);
        }
    }

    @Test
    public void createsNewData() throws Exception {
        final Person person = new Person();
        person.setName("Hank");
        person.setEmail("hank@example.com");
        person.setBirthday(new DateTime(1971, 3, 14, 19, 12, DateTimeZone.UTC));

        client.target(getUrl("/people/Hank")).request().put(Entity.entity(person, MediaType.APPLICATION_JSON));

        final Person hank = client.target(getUrl("/people/Hank"))
                .request(MediaType.APPLICATION_JSON)
                .get(Person.class);

        assertThat(hank.getName())
                .isEqualTo("Hank");

        assertThat(hank.getEmail())
                .isEqualTo("hank@example.com");

        assertThat(hank.getBirthday())
                .isEqualTo(person.getBirthday());
    }

    @Test
    public void testSqlExceptionIsHandled() throws Exception {
        final Person person = new Person();
        person.setName("Jeff");
        person.setEmail("jeff.hammersmith@targetprocessinc.com");
        person.setBirthday(new DateTime(1984, 2, 11, 0, 0, DateTimeZone.UTC));

        final Response response = client.target(getUrl("/people/Jeff")).request().
                put(Entity.entity(person, MediaType.APPLICATION_JSON));

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.BAD_REQUEST);
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.readEntity(ErrorMessage.class).getMessage()).isEqualTo("Wrong email");
    }
}
