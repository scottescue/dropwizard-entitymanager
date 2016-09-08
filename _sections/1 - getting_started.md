---
---

# Getting Started

If you're using Maven, simply add the `dropwizard-entitymanager` dependency to your POM:

```xml
<dependency>
  <groupId>com.scottescue</groupId>
  <artifactId>dropwizard-entitymanager</artifactId>
  <version>$[release_version]</version>
</dependency>
``` 

<div class="alert alert-info" role="alert"> 
  <div><strong>Note</strong></div> Dropwizard Hibernate's <strong>@UnitOfWork</strong> annotation 
  is bundled within this library for convenience.  There is no need to add a dropwizard-hibernate 
  dependency. 
</div>
