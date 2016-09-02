---
priority: 5
---
# Prepended Comments

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