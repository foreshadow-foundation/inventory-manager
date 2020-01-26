val v = new {
  val scala = "2.12.10"
  val scalaJSDom = "0.9.8"
  val scalaJSReact = "1.6.0-SNAPSHOT"
  val scalaCss = "0.6.0"
  val reactJS = "16.12.0"
  val hammock = "0.10.0"
  val http4s = "0.21.0-M6"
  val gsheets4s = "0.2.0+60-e87d4ff3-SNAPSHOT" //"0.2.0"
  val log4j2 = "2.13.0"
  val catsTagless = "0.11"
  val circe = "0.12.1"
}

lazy val commonSettings = Seq(
  organization := "org.foreshadow",
  homepage := Some(url("https://github.com/foreshadow-foundation/inventory-manager")),
  licenses += ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0")),
  startYear := Option(2020),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
  resolvers += Resolver.bintrayRepo("bpholt", "maven"),
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),
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

lazy val core = sbtcrossproject.CrossPlugin.autoImport.crossProject(JVMPlatform, JSPlatform)
  .crossType(sbtcrossproject.CrossType.Full)
  .in(file("core"))
  .settings(commonSettings ++ bintraySettings ++ releaseSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.3.3",
      "io.circe" %%% "circe-core" % v.circe,
      "io.circe" %%% "circe-generic" % v.circe,
      "io.circe" %%% "circe-literal" % v.circe,
    ),
  )

lazy val ui = (project in file("ui"))
  .settings(commonSettings ++ bintraySettings ++ releaseSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.3.3",
      "com.pepegar" %%% "hammock-core" % v.hammock,
      "com.pepegar" %%% "hammock-circe" % v.hammock,
      "org.typelevel" %%% "cats-effect" % "2.0.0",
      "org.scala-js" %%% "scalajs-dom" % v.scalaJSDom,
      "com.github.japgolly.scalajs-react" %%% "core" % v.scalaJSReact,
      "com.github.japgolly.scalajs-react" %%% "extra" % v.scalaJSReact,
      "com.github.japgolly.scalajs-react" %%% "ext-cats" % v.scalaJSReact,
      "com.github.japgolly.scalajs-react" %%% "ext-cats-effect" % v.scalaJSReact,
      "com.github.japgolly.scalajs-react" %%% "ext-monocle-cats" % v.scalaJSReact,
      "com.github.japgolly.scalacss" %%% "core" % v.scalaCss,
      "com.github.japgolly.scalacss" %%% "ext-react" % v.scalaCss,
      "com.github.julien-truffaut" %%% "monocle-macro" % "2.0.0",
    ),
    (scalaJSUseMainModuleInitializer in Compile) := true,
    skip in packageJSDependencies := false, // creates single js resource file for easy integration in html page
    crossTarget in (Compile, fullOptJS) := file("js"),
    crossTarget in (Compile, fastOptJS) := file("js"),
    crossTarget in (Compile, packageJSDependencies) := file("js"),
    crossTarget in (Compile, packageMinifiedJSDependencies) := file("js"),
    artifactPath in (Compile, fastOptJS) := ((crossTarget in (Compile, fastOptJS)).value / ((moduleName in fastOptJS).value + "-opt.js")),
    jsDependencies ++= Seq(
      "org.webjars.npm" % "react" % v.reactJS
        /        "umd/react.development.js"
        minified "umd/react.production.min.js"
        commonJSName "React",
      "org.webjars.npm" % "react-dom" % v.reactJS
        /         "umd/react-dom.development.js"
        minified  "umd/react-dom.production.min.js"
        dependsOn "umd/react.development.js"
        commonJSName "ReactDOM",
      "org.webjars.npm" % "react-dom" % v.reactJS
        /         "umd/react-dom-server.browser.development.js"
        minified  "umd/react-dom-server.browser.production.min.js"
        dependsOn "umd/react-dom.development.js"
        commonJSName "ReactDOMServer"
    ),
    dependencyOverrides ++= Seq( // fixes unresolved deps issue: https://github.com/webjars/webjars/issues/1789
      "org.webjars.npm" % "js-tokens" % "4.0.0",
      "org.webjars.npm" % "scheduler" % "0.14.0"
    ),
    workbenchCompression := true,
    workbenchStartMode := WorkbenchStartModes.OnCompile,
  )
  .dependsOn(core.js)
  .enablePlugins(ScalaJSPlugin, JSDependenciesPlugin, WorkbenchSplicePlugin)

lazy val backend = (project in file("backend"))
  .settings(commonSettings ++ bintraySettings ++ releaseSettings: _*)
  .settings(
    libraryDependencies ++= {
      Seq(
        "org.http4s" %% "http4s-blaze-server" % v.http4s,
        "org.http4s" %% "http4s-circe" % v.http4s,
        "org.http4s" %% "http4s-dsl" % v.http4s,
        "org.http4s" %% "http4s-client" % v.http4s,
        "org.http4s" %% "http4s-blaze-client" % v.http4s,
        "com.github.benfradet" %% "gsheets4s" % v.gsheets4s,
        "org.apache.logging.log4j" % "log4j-core" % v.log4j2,
        "org.apache.logging.log4j" % "log4j-slf4j-impl" % v.log4j2,
        "org.slf4j" % "jul-to-slf4j" % "1.7.30",
        "org.typelevel" %% "cats-tagless-core" % v.catsTagless,
        "org.typelevel" %% "cats-tagless-macros" % v.catsTagless,
        "org.scalatest" %% "scalatest" % "3.1.0" % Test,
      )
    },
  )
  .dependsOn(core.jvm)

lazy val `foreshadow-data-entry` = (project in file("."))
  .settings(Seq(
    description := "React site to facilitate Foreshadow inventory data entry",
  ) ++ commonSettings ++ bintraySettings ++ releaseSettings: _*)
  .aggregate(core.js, core.jvm, ui, backend)
