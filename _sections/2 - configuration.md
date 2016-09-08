---
---
# Configuration

First, your configuration class needs a `DataSourceFactory` instance:

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

This will create a new managed connection pool to the database, a health check for connectivity to the database, and 
a new `EntityManagerFactory` as well as a thread-safe `EntityManager` instance for you to use in your classes.
