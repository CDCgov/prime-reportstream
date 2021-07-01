# gradle_build

This directory contains a [GraphViz](https://en.wikipedia.org/wiki/Graphviz) representation of the gradle build script, showing the dependencies between tasks.

Generate a visual representation of [dependencies.gv](dependencies.gv) by running any of the GraphViz tools on it (dot, neato, circo, ...). If you have the GraphViz toolchain installed, you can just run the [./generate.sh](generate.sh) script to generate a `dependencies.svg` file for you which you can then open in your image viewer or browser of choice.

# Prerequisites

* Install GraphViz: https://graphviz.org/download/