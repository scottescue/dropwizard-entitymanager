Dropwizard EntityManager
========================
[![Build Status](https://travis-ci.org/scottescue/dropwizard-entitymanager.svg?branch=master)](https://travis-ci.org/scottescue/dropwizard-entitymanager)
[![Coverage Status](https://coveralls.io/repos/scottescue/dropwizard-entitymanager/badge.svg?branch=master&service=github)](https://coveralls.io/github/scottescue/dropwizard-entitymanager?branch=master)

An add-on module providing managed access to a Hibernate JPA `EntityManagerFactory` and a shareable, thread-safe 
`EntityManager` that works with `dropwizard-hibernate`'s `@UnitOfWork` annotation.

This module is derived from Dropwizard's own [dropwizard-hibernate](http://www.dropwizard.io/0.9.2/docs/manual/hibernate.html)
module.  The configuration and usage of this module should look very similar to that of `dropwizard-hibernate`.

See [scottescue.com/dropwizard-entitymanager](http://www.scottescue.com/dropwizard-entitymanager) for usage and configuration.

Support
-------

Please file bug reports and feature requests in [GitHub issues](https://github.com/scottescue/dropwizard-entitymanager/issues).


License and Credits
-------

Copyright (c) 2015-2016 Scott Escue 

This library is licensed under the Apache License, Version 2.0.

See the LICENSE file in this repository for the full license text.

<br />
The usage and implementation of this module is heavily derived from the Dropwizard Hibernate module.  Much of the credit
belongs to those who have contributed to Dropwizard Hibernate.  I've essentially adapted their work to create and expose 
the EntityManager and EntityManagerFactory objects.

Dropwizard is developed by
Coda Hale; Yammer, Inc.; and the Dropwizard Team, licensed
under the Apache 2.0 license.