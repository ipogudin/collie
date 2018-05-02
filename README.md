# collie
Collie is a management panel to control entities in relational databases. It can be embedded into a web application written on clojure.

## Development environment

### Boot

Collie's building infrastructure is based on Boot (Clojure build framework). To install it, please, follow [the instruction](https://github.com/boot-clj/boot#install).

### PhantomJS

PhantomJS is required to run client side tests. You can install it with `/scripts/install_phantomjs` or manually. You should have root permissions when running a script.

## Boot commands

This section describes boot commands which you can use. It contains only project specific aspects related to boot using.

### Dependencies

* `boot set-full-environment show` - To get a full environment for all sub-modules you can use set-full-environment task.

### Tests

* `boot watch speak set-environment-for-server-tests test` - To run tests for server side code.
* `boot watch speak set-environment-for-client-tests test-cljs` - To run tests for client side code (you must install phantomjs before running client side tests).

### Building

* `boot build-jar install` - To build a jar file and install it into the local maven repo (.m2)
