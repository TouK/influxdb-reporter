import sbt.Keys._
import com.banno.license.Plugin.LicenseKeys._
import com.banno.license.Licenses._

crossScalaVersions := Seq("2.10.6", "2.11.7")

val commonSettings =
  licenseSettings ++
    Seq(
      organization := "pl.touk.influxdb-reporter",
      javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
      scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-target:jvm-1.7"),
      removeExistingHeaderBlock := true,
      license := apache2("Copyright 2015"),
      licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
      isSnapshot := true,
      publishTo := {
        if (isSnapshot.value)
          Some("Sonatype Nexus" at "https://nexus.touk.pl/nexus/content/repositories/snapshots")
        else
          Some("Sonatype Nexus" at "https://nexus.touk.pl/nexus/content/repositories/releases")
      },
      credentials += Credentials(Path.userHome / ".ivy2" / ".nexus_touk_pl_credentials"),
      parallelExecution in Test := false
    )

lazy val core = project.in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "influxdb-reporter-core",
    libraryDependencies ++= {
      val dropwizardMetricsV  = "3.1.2"
      val scalaLogging        = "2.1.2"
      val scalaTestV          = "2.2.6"
      val scalaMockV          = "3.2.2"

      Seq(
        "io.dropwizard.metrics"        % "metrics-core"                 % dropwizardMetricsV,
        "com.typesafe.scala-logging"  %% "scala-logging-slf4j"          % scalaLogging,
        "org.scalatest"               %% "scalatest"                    % scalaTestV            % "test",
        "org.scalamock"               %% "scalamock-scalatest-support"  % scalaMockV            % "test"
      )
    })

lazy val httpClient = project.in(file("http-client"))
  .settings(commonSettings)
  .settings(
    name := "influxdb-reporter-http-client",
    libraryDependencies ++= {
      val typesafeConfigV     = "1.3.0"
      val dispatchV           = "0.11.2"

      Seq(
        "com.typesafe"                 % "config"               % typesafeConfigV,
        "net.databinder.dispatch"     %% "dispatch-core"        % dispatchV
      )
    }
  )
  .dependsOn(core)

lazy val httpClientJavaWrapper = project.in(file("http-client-java-wrapper"))
  .settings(commonSettings)
  .settings(
    name := "influxdb-reporter-http-client-java-wrapper",
    javacOptions in doc := Seq("-source", "1.7"),
    libraryDependencies ++= {
      val findbugsV = "3.0.1"

      Seq(
        "com.google.code.findbugs"      % "jsr305"              % findbugsV
      )
    }
  )
  .dependsOn(httpClient)

publish := {}
publishLocal := {}