Dropwizard EntityManager
========================
[![Build Status](https://travis-ci.org/scottescue/dropwizard-entitymanager.svg?branch=master)](https://travis-ci.org/scottescue/dropwizard-entitymanager)
[![Coverage Status](https://coveralls.io/repos/scottescue/dropwizard-entitymanager/badge.svg?branch=master&service=github)](https://coveralls.io/github/scottescue/dropwizard-entitymanager?branch=master)

> **Status:** Unmaintained portfolio/reference project.

Dropwizard EntityManager is an add-on module created for older Dropwizard applications that used JPA `EntityManager` APIs while still wanting managed lifecycle behavior compatible with Dropwizard Hibernate and `@UnitOfWork`.

This repository is preserved as a portfolio artifact and reference implementation. I do not actively maintain it, review dependency updates, or provide support for production use.

Modern Dropwizard applications should start with the official `dropwizard-hibernate` module and current Dropwizard/Hibernate documentation before considering this approach.


Getting Started
-------

See [scottescue.com/dropwizard-entitymanager](http://www.scottescue.com/dropwizard-entitymanager) for details.

Support
-------

This repository is preserved as an unmaintained portfolio/reference project. I am not accepting bug reports, feature requests, dependency updates, or production-support requests.


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


