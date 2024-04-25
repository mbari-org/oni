# oni

![Build](https://github.com/mbari-org/oni/actions/workflows/test.yml/badge.svg)

![MBARI logo](src/docs/images/logo-mbari-3b.png)

MBARI [sbt](https://www.scala-sbt.org) project compiled with [Scala 3](https://www.scala-lang.org)

## Usage

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

## Useful Commands

1. `stage` - Build runnable project in `target/universal`
2. `universal:packageBin` - Build zip files of runnable project in `target/universal`
3. `scaladoc` - Build documentation, including API docs to `target/docs/site`
4. `compile` then `scalafmtAll` - Will convert all syntax to new-style, indent based Scala 3.

## Libraries

- [circe](https://circe.github.io/circe/) for JSON handling
- [Methanol](https://github.com/mizosoft/methanol) with [Java's HttpClient](https://docs.oracle.com/en/java/javase/17/docs/api/java.net.http/java/net/http/HttpClient.html) for HTTP client
- [munit](https://github.com/scalameta/munit) for testing
- [picocli](https://picocli.info/) for command line arg parsing
- [slf4j](http://www.slf4j.org/) with [logback](http://logback.qos.ch/) for logging. Use java.lang.System.Logger
- [ZIO](https://zio.dev/) for effects

## Notes

Documentation can be added as markdown files in `docs` and will be included automatically when you run `scaladoc`.

When updating SBT version, make sure to update the devcontainer image in [devcontainer.json](.devcontainer/devcontainer.json). It's versions are `eclipse-temurin-<java.version>_<sbt.version>_<scala.version>`
