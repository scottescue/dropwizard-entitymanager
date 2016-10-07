package com.scottescue.dropwizard.entitymanager;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.scottescue.dropwizard.entitymanager.entity.Person;
import com.scottescue.dropwizard.entitymanager.mapper.DataExceptionMapper;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.errors.ErrorMessage;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.logging.BootstrapLogging;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.ws.rs.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JerseyIntegrationTest extends JerseyTest {
    static {
        BootstrapLogging.bootstrap();
        // Prevent expected Hibernate SQL Exceptions from cluttering unit test logs
        Logger logger = (Logger)LoggerFactory.getLogger(org.hibernate.engine.jdbc.spi.SqlExceptionHelper.class);
        logger.setLevel(Level.OFF);
    }

    public static class PersonService {
        private EntityManager entityManager;
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

    private EntityManagerFactory entityManagerFactory;

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();

        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @Override
    protected Application configure() {
        forceSet(TestProperties.CONTAINER_PORT, "0");

        final MetricRegistry metricRegistry = new MetricRegistry();
        final EntityManagerFactoryFactory factory = new EntityManagerFactoryFactory();
        final DataSourceFactory dbConfig = new DataSourceFactory();
        final EntityManagerBundle<?> bundle = mock(EntityManagerBundle.class);
        final Environment environment = mock(Environment.class);
        final LifecycleEnvironment lifecycleEnvironment = mock(LifecycleEnvironment.class);
        when(bundle.name()).thenReturn(getClass().getSimpleName() + "-bundle");
        when(environment.lifecycle()).thenReturn(lifecycleEnvironment);
        when(environment.metrics()).thenReturn(metricRegistry);

        dbConfig.setUrl("jdbc:hsqldb:mem:DbTest-" + System.nanoTime()+"?hsqldb.translate_dti_types=false");
        dbConfig.setUser("sa");
        dbConfig.setDriverClass("org.hsqldb.jdbcDriver");
        dbConfig.setValidationQuery("SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS");

        this.entityManagerFactory = factory.build(bundle,
                environment,
                dbConfig,
                ImmutableList.<Class<?>>of(Person.class));

        final EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            EntityTransaction tx = entityManager.getTransaction();
            tx.begin();

            entityManager.createNativeQuery("DROP TABLE people IF EXISTS").executeUpdate();
            entityManager.createNativeQuery(
                    "CREATE TABLE people (name varchar(100) primary key, email varchar(16), birthday timestamp with time zone)")
                    .executeUpdate();
            entityManager.createNativeQuery(
                    "INSERT INTO people VALUES ('Coda', 'coda@example.com', '1979-01-02 00:22:00+0:00')")
                    .executeUpdate();

            tx.commit();
        } finally {
            entityManager.close();
        }

        final SharedEntityManagerFactory sharedEntityManagerFactory = new SharedEntityManagerFactory();
        final EntityManagerContext context = new EntityManagerContext(this.entityManagerFactory);
        final EntityManager sharedEntityManager = sharedEntityManagerFactory.build(context);

        final DropwizardResourceConfig config = DropwizardResourceConfig.forTesting(new MetricRegistry());
        config.register(new UnitOfWorkApplicationListener("hr-db", entityManagerFactory));
        config.register(new PersonResource(new PersonService(sharedEntityManager)));
        config.register(new JacksonMessageBodyProvider(Jackson.newObjectMapper()));

        config.register(new DataExceptionMapper());

        return config;
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(new JacksonMessageBodyProvider(Jackson.newObjectMapper()));
    }

    @Test
    public void findsExistingData() throws Exception {
        final Person coda = target("/people/Coda").request(MediaType.APPLICATION_JSON).get(Person.class);

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
            target("/people/Poof").request(MediaType.APPLICATION_JSON)
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

        target("/people/Hank").request().put(Entity.entity(person, MediaType.APPLICATION_JSON));

        final Person hank = target("/people/Hank")
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

            final Response response = target("/people/Jeff").request().
                    put(Entity.entity(person, MediaType.APPLICATION_JSON));

            assertThat(response.getStatusInfo()).isEqualTo(Response.Status.BAD_REQUEST);
            assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE)).isEqualTo(MediaType.APPLICATION_JSON);
            assertThat(response.readEntity(ErrorMessage.class).getMessage()).isEqualTo("Wrong email");
    }
}
