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

Then, add an <a href="{{ site.baseurl }}/$[release_version]/api/com/scottescue/dropwizard/entitymanager/EntityManagerBundle.html">EntityManagerBundle</a> instance to your application class, specifying your entity classes and how to get a 
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

If you don't want to explicitly specify your entity classes, you can instead create a 
<a href="{{ site.baseurl }}/$[release_version]/api/com/scottescue/dropwizard/entitymanager/ScanningEntityManagerBundle.html">ScanningEntityManagerBundle</a> 
instance specifying a package in your application to recursively scan for entity classes:

```java
private final EntityManagerBundle<ExampleConfiguration> entityManagerBundle = 
        new ScanningEntityManagerBundle<ExampleConfiguration>("com.myapp") {
    @Override
    public DataSourceFactory getDataSourceFactory(ExampleConfiguration configuration) {
        return configuration.getDataSourceFactory();
    }
};
```

Creating an instance of either type of bundle will create a new managed connection pool to the database, a health check 
for connectivity to the database, and a new `EntityManagerFactory` as well as a thread-safe `EntityManager` instance 
for you to use in your classes.
