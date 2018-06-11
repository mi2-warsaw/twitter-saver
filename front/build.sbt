enablePlugins(ScalaJSPlugin)

name := "twitter"

version := "0.1"

scalaVersion := "2.12.4"

scalaJSUseMainModuleInitializer := true

libraryDependencies ++= Seq(
  "com.github.japgolly.scalajs-react" %%% "core" % "1.1.0",
  "com.github.japgolly.scalajs-react" %%% "extra" % "1.1.0",
  "io.suzaku" %%% "diode" % "1.1.3",
  "io.suzaku" %%% "diode-react" % "1.1.3",
  "io.circe" %%% "circe-core" % "0.9.1",
  "io.circe" %%% "circe-generic" % "0.9.1",
  "io.circe" %%% "circe-parser" % "0.9.1"
)

jsDependencies ++= Seq(
  "org.webjars.bower" % "react" % "15.6.1" / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
  "org.webjars.bower" % "react" % "15.6.1" / "react-dom.js" minified  "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM"
)
