Dropwizard EntityManager
========================
[![Build Status](https://travis-ci.org/scottescue/dropwizard-entitymanager.svg?branch=master)](https://travis-ci.org/scottescue/dropwizard-entitymanager)
[![Coverage Status](https://coveralls.io/repos/scottescue/dropwizard-entitymanager/badge.svg?branch=master&service=github)](https://coveralls.io/github/scottescue/dropwizard-entitymanager?branch=master)

An add-on module providing managed access to a Hibernate JPA `EntityManagerFactory` and a shareable, thread-safe 
`EntityManager` that works with `dropwizard-hibernate`'s `@UnitOfWork` annotation.

This module is derived from Dropwizard's own [dropwizard-hibernate](http://www.dropwizard.io/0.9.2/docs/manual/hibernate.html)
module.  The configuration and usage of this module should look very similar to that of `dropwizard-hibernate`.

Configuration
-----

First, your [configuration class](http://www.dropwizard.io/0.9.1/docs/manual/core.html#man-core-configuration) 
needs a `DataSourceFactory` instance:
```java
public class ExampleConfiguration extends Configuration {
    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }
}
```

Then, add an `EntityManagerBundle` instance to your application class, specifying your entity classes and how to get a 
`DataSourceFactory` from your configuration subclass:

```java
private final EntityManagerBundle<ExampleConfiguration> entityManagerBundle = 
        new EntityManagerBundle<ExampleConfiguration>(Person.class) {
    @Override
    public DataSourceFactory getDataSourceFactory(ExampleConfiguration configuration) {
        return configuration.getDataSourceFactory();
    }
};

@Override
public void initialize(Bootstrap<ExampleConfiguration> bootstrap) {
    bootstrap.addBundle(entityManagerBundle);
}

@Override
public void run(ExampleConfiguration config, Environment environment) {
    final EntityManager entityManager = entityManagerBundle.getSharedEntityManager();
    environment.jersey().register(new UserResource(entityManager));
}
```

This will create a new [managed](http://www.dropwizard.io/0.9.1/docs/manual/core.html#man-core-managed) connection pool 
to the database, a [health check](http://www.dropwizard.io/0.9.1/docs/manual/core.html#man-core-healthchecks) for 
connectivity to the database, and a new `EntityManagerFactory` as well as a thread-safe `EntityManager` instance for 
you to use in your classes.

Usage
-----
### Container Managed PersistentContext
The shared `EntityManager` obtained from your `EntityManagerBundle` works with the `@UnitOfWork` annotation from the 
[dropwizard-hibernate](http://www.dropwizard.io/0.9.2/docs/manual/hibernate.html) module.  The `@UnitOfWork` annotation 
may be applied to resource methods to create a container managed `PersistenceContext`.  This gives you the ability to 
declaratively scope transaction boundaries.  The annotation _must_ be present on any resource method that either 
directly or indirectly uses the shared `EntityManager`.

```java
@POST
@Timed
@UnitOfWork
public Response create(@Valid Person person) {
    entityManager.persist(checkNotNull(person));

    return Response.created(UriBuilder.fromResource(PersonResource.class)
            .build(person.getId()))
            .build();
}
```

This will automatically initialize the `EntityManager`, begin a transaction, call persist, commit the transaction, and 
finally close the `EntityManager`. If an exception is thrown, the transaction is rolled back.

Often you simply need to read data without requiring an actual transaction.
 
```java
@GET
@Timed
@UnitOfWork(transactional = false)
public Person findPerson(@PathParam("id") LongParam id) {
    return entityManager.find(Person.class, id.get());
}
```

This will automatically initialize the `EntityManager`, call find, and finally close the `EntityManager`.

##### Important

    The EntityManager is closed before your resource method’s return value (e.g., the Person from the database), 
    which means your resource method is responsible for initializing all lazily-loaded collections, etc., 
    before returning. Otherwise, you’ll get a `LazyInitializationException` thrown in your template (or null values 
    produced by Jackson).


### Application Managed PersistentContext
There may be times when you need to have more control over the `PersistenceContext` or need to manage a new transaction.  
The `EntityManagerFactory` obtained from your `EntityManagerBundle` allows you to create and manage new 
`EntityManager` instances.  Any `EntityManager` created from the factory will have a new `PersistenceContext` 
independent of any `@UnitOfWork` context or transaction.

```java
public void create(Person person) {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    EntityTransaction transaction = entityManager.getTransaction();
    try {
        transaction.begin();
        entityManager.persist(person);
        transaction.commit();
    } catch (RuntimeException e) {
        if (transaction.isActive()) {
            transaction.rollback();
        }
    } finally {
        entityManager.close();
    }
}
```

Prepended Comments
------------------

By default, `dropwizard-entitymanager` configures Hibernate JPA to prepend a comment describing the context of all 
queries:

```sql
/* load com.example.helloworld.core.Person */
select
    person0_.id as id0_0_,
    person0_.fullName as fullName0_0_,
    person0_.jobTitle as jobTitle0_0_
from people person0_
where person0_.id=?
```

This will allow you to quickly determine the origin of any slow or misbehaving queries.  See the Database - 
autoCommentsEnabled attribute in the [Dropwizard Configuration Reference](http://www.dropwizard.io/0.9.0/docs/manual/configuration.html) 


Maven Artifacts
---------------

This project is not yet available on Maven Central. You'll need to add this project's repository to your `pom.xml` before 
including it as a dependency:

```xml
<repositories>
    <repository>
        <id>dropwizard-entitymanager-mvn-repo</id>
        <url>https://raw.github.com/scottescue/dropwizard-entitymanager/mvn-repo/</url>
        <snapshots>
            <enabled>true</enabled>
            <updatePolicy>always</updatePolicy>
        </snapshots>
    </repository>
</repositories>
```

Now to include this project in your project, simply add the following dependency to your
`pom.xml`:

```xml
<dependency>
  <groupId>com.scottescue</groupId>
  <artifactId>dropwizard-entitymanager</artifactId>
  <version>0.9.0-1-SNAPSHOT</version>
</dependency>
```

There is no need to also include a `dropwizard-hibernate` dependency.  The `@UnitOfWork` annotation is bundled within 
this library for convenience.

Support
-------

Please file bug reports and feature requests in [GitHub issues](https://github.com/scottescue/dropwizard-entitymanager/issues).


License
-------

Copyright (c) 2016 Scott Escue 

This library is licensed under the Apache License, Version 2.0.

See http://www.apache.org/licenses/LICENSE-2.0.html or the LICENSE file in this repository for the full license text.