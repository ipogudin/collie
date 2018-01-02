# collie
Admin panel to manage entities based on clojure

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
* `boot watch speak set-environment-for-client-tests test-clj` - To run tests for client side code (you must install phantomjs before running client side tests).
