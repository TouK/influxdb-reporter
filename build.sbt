import sbt.Keys._
import sbtrelease.Version

val defaultScalaVersion = "2.12.7"
val scalaVersions = Seq("2.11.12", defaultScalaVersion)

val asyncHttpClientV    = "2.4.8"
val dropwizardMetricsV  = "4.0.2"
val findbugsV           = "3.0.1"
val hikariCPV           = "2.5.1"
val junitV              = "4.12"
val logbackV            = "1.2.3"
val mockitoV            = "2.13.0"
val scalaLoggingV       = "3.7.2"
val scalaTestV          = "3.0.4"
val typesafeConfigV     = "1.3.2"
val wiremockV           = "2.14.0"

val commonSettings =
  Seq(
    crossScalaVersions := scalaVersions,
    scalaVersion := defaultScalaVersion,
    organization := "pl.touk.influxdb-reporter",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions := Seq(
      "-unchecked", "-deprecation", "-encoding", "utf8", "-Xcheckinit", "-Xfatal-warnings", "-feature"
    ),
    licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    parallelExecution in Test := false
  )

val sonatypePublishSettings = Seq(
  isSnapshot := version(Version(_).get.qualifier.exists(_ == "-SNAPSHOT")).value,
  publishMavenStyle := true,
  publishTo in ThisBuild := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credential"),
  publishArtifact in Test := false,
  publishArtifact in(Compile, packageDoc) := false,
  pomExtra in Global := {
    <scm>
      <connection>scm:git:github.com/touk/influxdb-reporter.git</connection>
      <developerConnection>scm:git:git@github.com:touk/influxdb-reporter.git</developerConnection>
      <url>github.com/touk/influxdb-reporter</url>
    </scm>
      <developers>
        <developer>
          <id>coutoPL</id>
          <name>Mateusz Kołodziejczyk</name>
          <url>https://github.com/coutoPL</url>
        </developer>
      </developers>
  }
)

import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
)

lazy val core = project.in(file("core"))
  .settings(commonSettings)
  .settings(sonatypePublishSettings)
  .settings(
    name := "influxdb-reporter-core",
    libraryDependencies ++= {
      Seq(
        "io.dropwizard.metrics"        % "metrics-core"                 % dropwizardMetricsV,
        "com.typesafe.scala-logging"  %% "scala-logging"                % scalaLoggingV,

        "ch.qos.logback"               % "logback-classic"              % logbackV              % Test,
        "org.scalatest"               %% "scalatest"                    % scalaTestV            % Test,
        "org.mockito"                  % "mockito-core"                 % mockitoV              % Test
      )
    })

lazy val httpClient = project.in(file("http-client"))
  .settings(commonSettings)
  .settings(sonatypePublishSettings)
  .settings(
    name := "influxdb-reporter-http-client",
    libraryDependencies ++= {
      Seq(
        "com.typesafe"                 % "config"                       % typesafeConfigV,
        "org.asynchttpclient"          % "async-http-client"            % asyncHttpClientV,

        "ch.qos.logback"               % "logback-classic"              % logbackV              % Test,
        "org.scalatest"               %% "scalatest"                    % scalaTestV            % Test,
        "com.github.tomakehurst"       % "wiremock"                     % wiremockV             % Test
      )
    }
  )
  .dependsOn(core)

lazy val httpClientJavaWrapper = project.in(file("http-client-java-wrapper"))
  .settings(commonSettings)
  .settings(sonatypePublishSettings)
  .settings(
    name := "influxdb-reporter-http-client-java-wrapper",
    javacOptions in doc := Seq("-source", "1.8"),
    libraryDependencies ++= {
      Seq(
        "com.google.code.findbugs"    % "jsr305"                        % findbugsV,
        "junit"                       % "junit"                         % junitV      % Test
      )
    }
  )
  .dependsOn(httpClient)

lazy val hikariCPTracker = project.in(file("hikariCP-tracker"))
  .settings(commonSettings)
  .settings(sonatypePublishSettings)
  .settings(
    name := "influxdb-reporter-hikariCP-tracker",
    javacOptions in doc := Seq("-source", "1.8"),
    libraryDependencies ++= {
      Seq(
        "com.zaxxer"                  % "HikariCP"                      % hikariCPV
      )
    }
  )
  .dependsOn(httpClientJavaWrapper)
