logLevel := Level.Warn
resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
  url("http://dl.bintray.com/banno/oss"))(
  Resolver.ivyStylePatterns)

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.3")

addSbtPlugin("com.banno" % "sbt-license-plugin" % "0.1.5")

addSbtPlugin("com.eed3si9n" % "sbt-doge" % "0.1.5")