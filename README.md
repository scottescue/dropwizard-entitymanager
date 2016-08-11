Dropwizard EntityManager
========================
[![Build Status](https://travis-ci.org/scottescue/dropwizard-entitymanager.svg?branch=master)](https://travis-ci.org/scottescue/dropwizard-entitymanager)
[![Coverage Status](https://coveralls.io/repos/scottescue/dropwizard-entitymanager/badge.svg?branch=master&service=github)](https://coveralls.io/github/scottescue/dropwizard-entitymanager?branch=master)

An add-on module providing managed access to a Hibernate JPA `EntityManagerFactory` and a shareable, thread-safe 
`EntityManager` that works with Dropwizard Hibernate's `@UnitOfWork` annotation.


Getting Started
-------

See [scottescue.com/dropwizard-entitymanager](http://www.scottescue.com/dropwizard-entitymanager) for details.

Support
-------

Please file bug reports and feature requests in [GitHub issues](https://github.com/scottescue/dropwizard-entitymanager/issues).


Credits
-------

This module is heavily derived from Dropwizard Hibernate.  Those who have contributed to Dropwizard Hibernate deserve 
much of the credit for this project.  I've essentially adapted their work to create and expose the `EntityManager` and 
`EntityManagerFactory` objects.

Dropwizard is developed by
Coda Hale; Yammer, Inc.; and the Dropwizard Team, licensed
under the Apache 2.0 license.


License
-------

Copyright (c) 2015-2016 Scott Escue 

This library is licensed under the Apache License, Version 2.0.  See the [LICENSE](LICENSE) file in this repository for the full license text.


