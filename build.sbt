lazy val commonSettings = Seq(
  organization := "org.foreshadow",
  homepage := Some(url("https://github.com/foreshadow-foundation/inventory-interface")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  startYear := Option(2019),
  libraryDependencies ++= {
    Seq(
      "co.fs2" %%% "fs2-core" % "2.0.1",
      "com.chuusai" %%% "shapeless" % "2.3.3",
      "org.typelevel" %%% "cats-core" % "2.0.0",
      "org.typelevel" %%% "cats-effect" % "2.0.0",
      "org.scala-js" %%% "scalajs-dom" % "0.9.7",
      "com.planetholt" %%% "cats-js-clipboard" % "0.2.0",
      "com.github.japgolly.scalajs-react" %%% "core" % "1.5.0",
      "org.scalatest" %%% "scalatest" % "3.1.0" % Test,
    )
  },
  npmDependencies in Compile ++= Seq(
    "react" -> "16.7.0",
    "react-dom" -> "16.7.0"),
  resolvers += Resolver.bintrayRepo("bpholt", "maven"),
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
)

lazy val bintraySettings = Seq(
  bintrayVcsUrl := homepage.value.map(_.toString),
  bintrayRepository := "maven",
  bintrayOrganization := None,
  pomIncludeRepository := { _ => false }
)

lazy val releaseSettings = {
  import sbtrelease.ReleaseStateTransformations._

  Seq(
    releaseVersionBump := sbtrelease.Version.Bump.Minor,
    releaseCrossBuild := true,
    releaseProcess -= runTest,
  )
}

lazy val `foreshadow-data-entry` = (project in file("."))
  .settings(Seq(
    description := "React site to facilitate Foreshadow inventory data entry",
  ) ++ commonSettings ++ bintraySettings ++ releaseSettings: _*)
  .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
