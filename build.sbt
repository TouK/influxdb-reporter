import sbt.Keys._
import com.banno.license.Plugin.LicenseKeys._
import com.banno.license.Licenses._
import sbtrelease.Version

crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.0")

val commonSettings =
  licenseSettings ++
    Seq(
      organization := "pl.touk.influxdb-reporter",
      javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
      scalacOptions := Seq(
        "-unchecked", "-deprecation", "-encoding", "utf8", "-Xcheckinit", "-Xfatal-warnings", "-feature"
      ),
      removeExistingHeaderBlock := true,
      license := apache2("Copyright 2015"),
      licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
      parallelExecution in Test := false
    )

val publishSettings = Seq(
  isSnapshot := version(Version(_).get.qualifier.exists(_ == "-SNAPSHOT")).value,
  publishTo in ThisBuild := {
    if (isSnapshot.value)
      Some("Sonatype Nexus" at "https://nexus.touk.pl/nexus/content/repositories/snapshots")
    else
      Some("Sonatype Nexus" at "https://nexus.touk.pl/nexus/content/repositories/releases")
  },
  credentials += Credentials(Path.userHome / ".ivy2" / ".nexus_touk_pl_credentials"),
  publishArtifact in(Compile, packageDoc) := false
)

lazy val core = project.in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "influxdb-reporter-core",
    resolvers in ThisBuild ++= Seq(
      "TouK repo releases"                   at "https://philanthropist.touk.pl/nexus/content/repositories/releases",
      "TouK repo snapshots"                  at "https://philanthropist.touk.pl/nexus/content/repositories/snapshots"
    ),
    libraryDependencies <++= scalaVersion { v =>
      val dropwizardMetricsV  = "3.1.2"
      val logbackV            = "1.1.2"
      val scalaLoggingV       = "3.6.0-SNAPSHOT"
      val scalaTestV          = "3.0.0"
      val mockitoV            = "2.2.13"

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
  .settings(publishSettings)
  .settings(
    name := "influxdb-reporter-http-client",
    libraryDependencies ++= {
      val asyncHttpClientV    = "2.0.27"
      val logbackV            = "1.1.2"
      val scalaTestV          = "3.0.0"
      val typesafeConfigV     = "1.3.0"
      val wiremockV           = "2.5.1"

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
  .settings(publishSettings)
  .settings(
    name := "influxdb-reporter-http-client-java-wrapper",
    javacOptions in doc := Seq("-source", "1.8"),
    libraryDependencies ++= {
      val findbugsV           = "3.0.1"
      val junitV              = "4.12"

      Seq(
        "com.google.code.findbugs" % "jsr305" % findbugsV,
        "junit"                    % "junit"  % junitV      % Test
      )
    }
  )
  .dependsOn(httpClient)

lazy val hikariCPTracker = project.in(file("hikariCP-tracker"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    name := "influxdb-reporter-hikariCP-tracker",
    javacOptions in doc := Seq("-source", "1.8"),
    libraryDependencies ++= {
      val hikariCPV           = "2.4.7"

      Seq(
        "com.zaxxer" % "HikariCP" % hikariCPV
      )
    }
  )
  .dependsOn(httpClientJavaWrapper)
