import sbt.*
object Dependencies {

    lazy val auth0 = "com.auth0" % "java-jwt" % "4.4.0"

    val circeVersion      = "0.14.7"
    lazy val circeCore    = "io.circe" %% "circe-core"    % circeVersion
    lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
    lazy val circeParser  = "io.circe" %% "circe-parser"  % circeVersion

    lazy val commonsCodec = "commons-codec" % "commons-codec" % "1.17.0"
    lazy val gson = "com.google.code.gson" % "gson" % "2.10.1"

    val hibernateVersion     = "6.5.0.Final"
    lazy val hibernateCore   = "org.hibernate.orm" % "hibernate-core"     % hibernateVersion
    lazy val hibernateEnvers = "org.hibernate.orm" % "hibernate-envers"   % hibernateVersion
    lazy val hibernateHikari = "org.hibernate.orm" % "hibernate-hikaricp" % hibernateVersion

    lazy val hikariCp    = "com.zaxxer"              % "HikariCP"                   % "5.1.0"
    lazy val jansi       = "org.fusesource.jansi"    % "jansi"                      % "2.4.1"
    lazy val jaspyt      = "org.jasypt"              % "jasypt"                     % "1.9.3"
    lazy val junit       = "junit"                   % "junit"                      % "4.13.2"
    lazy val logback     = "ch.qos.logback"          % "logback-classic"            % "1.5.6"
    lazy val mssqlserver = "com.microsoft.sqlserver" % "mssql-jdbc"                 % "12.6.1.jre11"
    lazy val munit       = "org.scalameta"          %% "munit"                      % "1.0.0-RC1"
    lazy val oracle      = "com.oracle.ojdbc"        % "ojdbc8"                     % "19.3.0.0"
    lazy val postgresql  = "org.postgresql"          % "postgresql"                 % "42.7.3"
    lazy val scilube     = "org.mbari.scilube"      %% "scilube"                    % "3.0.1"
    lazy val slf4jSystem = "org.slf4j"               % "slf4j-jdk-platform-logging" % "2.0.13"

    private val tapirVersion = "1.10.6"
    lazy val tapirCirce      = "com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % tapirVersion
    lazy val tapirHelidon    = "com.softwaremill.sttp.tapir" %% "tapir-nima-server"        % tapirVersion
    lazy val tapirPrometheus = "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % tapirVersion
    lazy val tapirServerStub = "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server"   % tapirVersion
    lazy val tapirSwagger    = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle"  % tapirVersion
    lazy val tapirVertex     = "com.softwaremill.sttp.tapir" %% "tapir-vertx-server"       % tapirVersion

    lazy val tapirSttpCirce          = "com.softwaremill.sttp.client3" %% "circe"          % "3.9.5"
    val testcontainersVersion        = "1.19.7"
    lazy val testcontainersCore      = "org.testcontainers"             % "testcontainers" % testcontainersVersion
    lazy val testcontainersSqlserver = "org.testcontainers"             % "mssqlserver"    % testcontainersVersion
    lazy val testcontainersOracle    = "org.testcontainers"             % "oracle-xe"      % testcontainersVersion
    lazy val testcontainersPostgres  = "org.testcontainers"             % "postgresql"     % testcontainersVersion

    lazy val typesafeConfig = "com.typesafe"    % "config"     % "1.4.3"
    lazy val uuidgen        = "org.mbari.uuid"  % "uuid-gen"   % "0.1.4"
    lazy val vcr4jCore      = "org.mbari.vcr4j" % "vcr4j-core" % "5.2.0"
    lazy val zeromq         = "org.zeromq"      % "jeromq"     % "0.6.0"

}