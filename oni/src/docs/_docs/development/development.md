# Development

## Usage

This is a normal [sbt](https://www.scala-sbt.org) project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

## Useful Commands

1. `stage` - Build runnable project in `target/universal`
2. `universal:packageBin` - Build zip files of runnable project in `target/universal`
3. `scaladoc` - Build documentation, including API docs to `target/docs/site`
4. `compile` then `scalafmtAll` - Will convert all syntax to new-style, indent based Scala 3.


## Notes

Documentation can be added as markdown files in `docs` and will be included automatically when you run `scaladoc`.

When updating SBT version, make sure to update the devcontainer image in [devcontainer.json](.devcontainer/devcontainer.json). It's versions are `eclipse-temurin-<java.version>_<sbt.version>_<scala.version>`

