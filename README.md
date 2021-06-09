[![Sputnik](https://sputnik.ci/conf/badge)](https://sputnik.ci/app#/builds/TouK/influxdb-reporter)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/pl.touk.influxdb-reporter/influxdb-reporter-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/pl.touk.influxdb-reporter/influxdb-reporter-core_2.12)

# influxdb-reporter
Reporter to Influxdb 0.9+ implementing (extended) Dropwizard metrics API 

## Running tests

$ sbt tests

## Publishing 

$ sbt release 

### Required configuration

1. File with Sonatype credentials `~/.sbt/1.0/sonatype.sbt`:

```
credentials += Credentials(
        "Sonatype Nexus Repository Manager",
        "oss.sonatype.org",
        "XXXXXXX",
        "YYYYY"
)
```

1. File with GPG sbt plugin configuration `~/.sbt/1.0/gpg.sbt`:

```
usePgpKeyHex("XYXYXYXYXYX")
```
