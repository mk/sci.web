# sci.web

An online playground for the [Small Clojure Interpreter](https://github.com/borkdude/sci)
hosted [here](https://borkdude.github.io/sci.web).

## Development

To get an interactive development environment run:

    clojure -A:fig:dev

This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    rm -rf target/public out

## Build

Build a production version of sci.web:

    script/build

## License

Copyright © 2019 Michiel Borkent

Distributed under the EPL License, same as Clojure. See LICENSE.
