import sbt.*
object Dependencies {

    lazy val auth0 = "com.auth0" % "java-jwt" % "4.5.0"

    val caffeineVersion     = "3.2.1"
    lazy val caffeine       = "com.github.ben-manes.caffeine" % "caffeine" % caffeineVersion
    lazy val caffeineJCache = "com.github.ben-manes.caffeine" % "jcache" % caffeineVersion

    val circeVersion      = "0.14.14"
    lazy val circeCore    = "io.circe" %% "circe-core"    % circeVersion
    lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
    lazy val circeParser  = "io.circe" %% "circe-parser"  % circeVersion

    lazy val commonsCodec = "commons-codec" % "commons-codec" % "1.18.0"
    lazy val gson = "com.google.code.gson" % "gson" % "2.13.1"

    // THis needs to match the version used by tapirHelidon.
    // Just including these in the build allows Helidon to use them for content encoding.
    val helidonVersion              = "4.0.0"
    lazy val helidonEncodingDeflate = "io.helidon.http.encoding" % "helidon-http-encoding-deflate" % helidonVersion
    lazy val helidonEncodingGzip    = "io.helidon.http.encoding" % "helidon-http-encoding-gzip" % helidonVersion

    // val hibernateVersion      = "6.6.17.Final"
     val hibernateVersion      = "7.0.3.Final"
    lazy val hibernateCore    = "org.hibernate.orm" % "hibernate-core"     % hibernateVersion
    lazy val hibernateJCache  = "org.hibernate"     % "hibernate-jcache"     % hibernateVersion
    lazy val hibernateEnvers  = "org.hibernate.orm" % "hibernate-envers"   % hibernateVersion
    lazy val hibernateHikari  = "org.hibernate.orm" % "hibernate-hikaricp" % hibernateVersion

    lazy val hikariCp    = "com.zaxxer"              % "HikariCP"                   % "6.3.0"
    lazy val jansi       = "org.fusesource.jansi"    % "jansi"                      % "2.4.2"
    lazy val jaspyt      = "org.jasypt"              % "jasypt"                     % "1.9.3"
    lazy val junit       = "junit"                   % "junit"                      % "4.13.2"
    lazy val logback     = "ch.qos.logback"          % "logback-classic"            % "1.5.18"
    lazy val mssqlserver = "com.microsoft.sqlserver" % "mssql-jdbc"                 % "12.10.0.jre11"
    lazy val munit       = "org.scalameta"          %% "munit"                      % "1.1.1"
    lazy val oracle      = "com.oracle.ojdbc"        % "ojdbc8"                     % "19.3.0.0"
    lazy val postgresql  = "org.postgresql"          % "postgresql"                 % "42.7.7"
//    lazy val scilube     = "org.mbari.scilube"      %% "scilube"                    % "3.0.1"

    val slf4jVersion = "2.0.17"
    lazy val slf4jJulBridge = "org.slf4j" % "jul-to-slf4j"               % slf4jVersion
    lazy val slf4jSystem    = "org.slf4j" % "slf4j-jdk-platform-logging" % slf4jVersion

    private val tapirVersion = "1.11.35"
    lazy val tapirCirce      = "com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % tapirVersion
    lazy val tapirHelidon    = "com.softwaremill.sttp.tapir" %% "tapir-nima-server"        % tapirVersion
    lazy val tapirPrometheus = "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % tapirVersion
    lazy val tapirServerStub = "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server"   % tapirVersion
    lazy val tapirSwagger    = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle"  % tapirVersion
    lazy val tapirVertex     = "com.softwaremill.sttp.tapir" %% "tapir-vertx-server"       % tapirVersion

    lazy val tapirSttpCirce          = "com.softwaremill.sttp.client3" %% "circe"          % "3.11.0"

    val testcontainersVersion        = "1.21.2"
    lazy val testcontainersCore      = "org.testcontainers"             % "testcontainers" % testcontainersVersion
    lazy val testcontainersJdbc      = "org.testcontainers"             % "jdbc"           % testcontainersVersion
    lazy val testcontainersSqlserver = "org.testcontainers"             % "mssqlserver"    % testcontainersVersion
    lazy val testcontainersOracle    = "org.testcontainers"             % "oracle-xe"      % testcontainersVersion
    lazy val testcontainersPostgres  = "org.testcontainers"             % "postgresql"     % testcontainersVersion

    lazy val typesafeConfig = "com.typesafe"    % "config"     % "1.4.3"
//    lazy val uuidgen        = "org.mbari.uuid"  % "uuid-gen"   % "0.1.4"
//    lazy val vcr4jCore      = "org.mbari.vcr4j" % "vcr4j-core" % "5.2.0"
//    lazy val zeromq         = "org.zeromq"      % "jeromq"     % "0.6.0"

}