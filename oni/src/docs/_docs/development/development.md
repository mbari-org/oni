# Development

## Usage

This is a normal [sbt](https://www.scala-sbt.org) project. You will need Docker installed to run the full test suite. Docker allows this project to start database servers for integration testing.

### Useful SBT Commands

1. `stage` - Builds a runnable project in `target/oni/universal/stage`
2. `Docker/stage` - Builds a Dockerfile for Oni at `target/oni/docker/stage`
3. `doc` - Build documentation, including API docs to `target/docs/site`
4. `compile` then `scalafmtAll` - Will convert all syntax to new-style, indent based Scala 3.
5. `test` run all tests
6. `itPostgres/test` or `itSqlserver/test` to only run tests against one of the databases.
7. `itPostgres/testOnly <testname>` or `itSqlserver/testOnly <testname>` to run a single test.

## Notes

Documentation can be added as markdown files in `oni/src/docs/_docs` and will be included automatically when you run `scaladoc`.


