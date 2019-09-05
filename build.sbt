lazy val catsVersion = "1.5.0"
lazy val catsEffectVersion = "1.2.0"
lazy val http4sVersion = "0.20.0-M5"
lazy val fs2Version = "1.0.3"
lazy val circeVersion = "0.11.1"
lazy val log4catsVersion = "0.3.0-M2"
lazy val scalaScraperVersion = "2.1.0"

lazy val scalaTestVersion = "3.0.8"

lazy val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.12.8",
  scalacOptions ++= Seq(
    "-language:higherKinds",
//    "-Ymacro-annotations",
    "-Ypartial-unification",
  ),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % catsVersion,
    "com.github.mpilquist" %% "simulacrum" % "0.15.0",
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test",

  ),
  resolvers += Resolver.sonatypeRepo("releases"),
  addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "1.5.1"),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8"),
  addCompilerPlugin(
    "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
)

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "watcher",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-parser" % circeVersion,
    ),
  )
  .dependsOn(infrastructure, controller, usecase, adapter, plugins)

lazy val adapter = (project in file("adapter"))
  .settings(commonSettings)
  .settings(
    name := "watcher-adapter",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.http4s" %% "http4s-core" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "io.circe" %% "circe-literal" % circeVersion,
      "io.chrisdavenport" %% "log4cats-core" % log4catsVersion,
      "io.chrisdavenport" %% "log4cats-extras" % log4catsVersion,
      "io.chrisdavenport" %% "log4cats-slf4j" % log4catsVersion,
      "org.slf4j" % "slf4j-log4j12" % "1.7.25",
    ),
  )
  .dependsOn(infrastructure, port)

lazy val controller = (project in file("controller"))
  .settings(commonSettings)
  .settings(
    name := "watcher-controller",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-core" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
    )
  )
  .dependsOn(infrastructure, usecase)

lazy val plugins = (project in file("plugins"))
  .settings(commonSettings)
  .settings(
    name := "watcher-plugins",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.http4s" %% "http4s-core" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "io.circe" %% "circe-literal" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.chrisdavenport" %% "log4cats-core" % log4catsVersion,
      "io.chrisdavenport" %% "log4cats-extras" % log4catsVersion,
      "io.chrisdavenport" %% "log4cats-slf4j" % log4catsVersion,
      "org.slf4j" % "slf4j-log4j12" % "1.7.25",
      "net.ruippeixotog" %% "scala-scraper" % "2.1.0",
    ),
  )
  .dependsOn(infrastructure, port)

lazy val port = (project in file("port"))
  .settings(commonSettings)
  .settings(
    name := "watcher-port",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
    )
  )
  .dependsOn(infrastructure, domain)

lazy val usecase = (project in file("usecase"))
  .settings(commonSettings)
  .settings(
    name := "watcher-usecase",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
    )
  )
  .dependsOn(infrastructure, domain, port)

lazy val domain = (project in file("domain"))
  .settings(commonSettings)
  .settings(
    name := "watcher-domain",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
    ),
  )
  .dependsOn(infrastructure)

lazy val infrastructure = (project in file("infrastructure"))
  .settings(commonSettings)
  .settings(
    name := "watcher-infrastructure"
  )
