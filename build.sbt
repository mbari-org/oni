import Dependencies._

Global / onChangedBuildSource := ReloadOnSourceChanges
Compile / doc / scalacOptions ++= Seq(
    "-groups",
    "-project-footer",
    "Monterey Bay Aquarium Research Institute",
    "-siteroot",
    "src/docs",
    "-doc-root-content",
    "./src/docs/index.md"
)

ThisBuild / javacOptions ++= Seq("-target", "21", "-source", "21")
ThisBuild / licenses         := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / organization     := "org.mbari"
ThisBuild / organizationName := "Monterey Bay Aquarium Research Institute"
ThisBuild / resolvers ++= Seq(Resolver.githubPackages("mbari-org", "maven"))
ThisBuild / scalaVersion     := "3.3.3"
// ThisBuild / scalaVersion     := "3.3.1" // Fails. See https://github.com/lampepfl/dotty/issues/17069#issuecomment-1763053572
ThisBuild / scalacOptions ++= Seq(
    "-deprecation",  // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "UTF-8",         // yes, this is 2 args. Specify character encoding used by source files.
    "-explaintypes", // Explain type errors in more detail.
    "-feature",      // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked"
)
ThisBuild / startYear        := Some(2024)
//ThisBuild / updateOptions    := updateOptions.value.withCachedResolution(true)
ThisBuild / versionScheme    := Some("semver-spec")

ThisBuild / Test / fork              := true
ThisBuild / Test / parallelExecution := false
ThisBuild / Test / testOptions += Tests.Argument(TestFrameworks.MUnit, "-b")
ThisBuild / Test / javaOptions ++= Seq(
    "-Duser.timezone=UTC"
)

lazy val oni = project
  .in(file("oni"))
  .enablePlugins(
    AutomateHeaderPlugin, 
    GitBranchPrompt, 
    GitVersioning, 
    JavaAppPackaging
  )
  .settings(
    // Set version based on git tag. I use "0.0.0" format (no leading "v", which is the default)
    // Use `show gitCurrentTags` in sbt to update/see the tags
    git.gitTagToVersionNumber := { tag: String =>
      if(tag matches "[0-9]+\\..*") Some(tag)
      else None
    },
    git.useGitDescribe := true,
    bashScriptExtraDefines ++= Seq(
            """addJava "-Dconfig.file=${app_home}/../conf/application.conf"""",
            """addJava "-Dlogback.configurationFile=${app_home}/../conf/logback.xml""""
        ),
    batScriptExtraDefines ++= Seq(
        """call :add_java "-Dconfig.file=%APP_HOME%\conf\application.conf"""",
        """call :add_java "-Dlogback.configurationFile=%APP_HOME%\conf\logback.xml""""
    ),
    // sbt-header
    headerLicense := Some(
      HeaderLicense.Custom(
        """Copyright (c) Monterey Bay Aquarium Research Institute 2024
        |
        |oni code is non-public software. Unauthorized copying of this file,
        |via any medium is strictly prohibited. Proprietary and confidential. 
        |""".stripMargin
      )
    ),
    libraryDependencies ++= Seq(
      auth0,
            circeCore,
            circeGeneric,
            circeParser,
            commonsCodec,
            hibernateCore,
            hibernateEnvers,
            hibernateHikari,
            hikariCp,
            jansi             % Runtime,
            jaspyt,
            junit             % Test,
            logback,
            mssqlserver,
            munit             % Test,
            oracle,
            postgresql,
            slf4jSystem,
            tapirCirce,
            tapirHelidon,
            tapirPrometheus,
            tapirServerStub   % Test,
            tapirSttpCirce,
            tapirSwagger,
            typesafeConfig
    )
  )


lazy val integrationTests = (project in file("it"))
    .dependsOn(oni)
    .enablePlugins(
        AutomateHeaderPlugin
    )
    .settings(
        libraryDependencies ++= Seq(
            junit,
            munit,
            tapirServerStub,
            testcontainersCore
        )
    )

lazy val itPostgres = (project in file("it-postgres"))
  .dependsOn(integrationTests)
  .enablePlugins(
    AutomateHeaderPlugin
  )
  .settings(
    libraryDependencies ++= Seq(
      testcontainersPostgres
    )
  )

lazy val itSqlserver = (project in file("it-sqlserver"))
  .dependsOn(integrationTests)
  .enablePlugins(
    AutomateHeaderPlugin
  )
  .settings(
    libraryDependencies ++= Seq(
      testcontainersSqlserver
    )
  )