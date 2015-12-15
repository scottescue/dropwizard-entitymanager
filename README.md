Dropwizard EntityManager
========================

An addon bundle providing managed access to JPA using Hibernate.

Beware - Work in Progress
-----
This project is under active development and is currently a development snapshot.  Not all features described below have 
been implemented.  I don't consider the project ready for public consumption.

Configuration
-----

To create a [managed](http://www.dropwizard.io/0.9.1/docs/manual/core.html#man-core-managed), instrumented 
`EntityManagerFactory` instance, your [configuration class](http://www.dropwizard.io/0.9.1/docs/manual/core.html#man-core-configuration) 
needs a `DataSourceFactory` instance:

    public class ExampleConfiguration extends Configuration {
        @Valid
        @NotNull
        private DataSourceFactory database = new DataSourceFactory();
    
        @JsonProperty("database")
        public DataSourceFactory getDataSourceFactory() {
            return database;
        }
    }

Then, add an `EntityManagerBundle` instance to your application class, specifying your entity classes and how to get a 
`DataSourceFactory` from your configuration subclass:

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
        final EntityManagerFactory entityManagerFactory = entityManagerBundle.getEntityManagerFactory();
        environment.jersey().register(new UserResource(entityManagerFactory));
    }

This will create a new [managed](http://www.dropwizard.io/0.9.1/docs/manual/core.html#man-core-managed) connection pool 
to the database, a [health check](http://www.dropwizard.io/0.9.1/docs/manual/core.html#man-core-healthchecks) for 
connectivity to the database, and a new `EntityManagerFactory` instance for you to use in your classes.


Maven Artifacts
---------------

This project is not yet available on Maven Central. You'll need to add this project's repository to your `pom.xml` before 
including it as a dependency:

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

Now to include this project in your project, simply add the following dependency to your
`pom.xml`:

    <dependency>
      <groupId>com.scottescue</groupId>
      <artifactId>dropwizard-entitymanager</artifactId>
      <version>0.9.0-1-SNAPSHOT</version>
    </dependency>


Support
-------

Please file bug reports and feature requests in [GitHub issues](https://github.com/scottescue/dropwizard-entitymanager/issues).


License
-------

Copyright (c) 2015 Scott Escue and Southern Fired Software, LLC 

This library is licensed under the Apache License, Version 2.0.

See http://www.apache.org/licenses/LICENSE-2.0.html or the LICENSE file in this repository for the full license text.