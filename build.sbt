import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

lazy val formattingPreferences = FormattingPreferences().
  setPreference(AlignParameters, true).
  setPreference(DoubleIndentClassDeclaration, true)

lazy val formattingSettings = SbtScalariform.scalariformSettings ++ Seq(
  ScalariformKeys.preferences in Compile := formattingPreferences,
  ScalariformKeys.preferences in Test := formattingPreferences)

lazy val commonSettings = Seq(
  organization := "eu.shiftforward",
  version := "0.0.1",
  scalaVersion := "2.11.8",
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8"),
  shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
) ++ formattingSettings ++ Revolver.settings

resolvers ++= Seq(
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

lazy val framework = (project in file("framework")).
  settings(commonSettings: _*).
  settings(
    name := "Manchester",
    libraryDependencies ++= Seq(
      "org.scalanlp" %% "breeze" % "0.12",
      "org.scalanlp" %% "breeze-natives" % "0.12",
      "org.scalanlp" %% "breeze-viz" % "0.12",
      "org.mongodb.scala" %% "mongo-scala-driver" % "1.1.0",
      "com.typesafe" % "config" % "1.3.0",
      "org.json4s" %% "json4s-native" % "3.3.0"
    )
  )

lazy val frontend = (project in file("frontend")).enablePlugins(PlayScala).
  settings(commonSettings: _*).
  settings(
    name := "frontend",
    routesGenerator := InjectedRoutesGenerator,
    libraryDependencies ++= Seq(
      jdbc,
      cache,
      ws,
      "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % Test,
      "com.github.jeroenr" %% "tepkin" % "0.7"
    )
  )

lazy val KKParser = (project in file("playground/KKParser")).
  settings(commonSettings: _*).
  settings(
    name := "KKParser",
    libraryDependencies ++= Seq(
      "org.json4s" %% "json4s-native" % "3.3.0"
    )
  )

lazy val RecommendationMLlib = (project in file("playground/RecommendationMLlib")).
  settings(commonSettings: _*).
  settings(
    name := "RecommendationMLlib",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core"  % "1.5.0",
      "org.apache.spark" %% "spark-mllib" % "1.5.0"
    )
  )

lazy val GephiWebsiteGraph = (project in file("playground/GephiWebsiteGraph")).
  settings(commonSettings: _*).
  settings(
    name := "GephiWebsiteGraph",
    libraryDependencies ++= Seq(
      "org.mongodb.scala" %% "mongo-scala-driver" % "1.1.0"
    )
  )

lazy val SmallSimulationExample = (project in file("examples/SmallSimulation")).
  settings(commonSettings: _*).
  settings(
    name := "SmallSimulation",
    fork in run := true, // required for GraphStream visualizer http://stackoverflow.com/questions/21464673/sbt-trapexitsecurityexception-thrown-at-sbt-run
    libraryDependencies ++= Seq(
      "org.graphstream" % "gs-core" % "1.3",
      "org.graphstream" % "gs-ui" % "1.3"
    )
  ).dependsOn(framework)

lazy val root = (project in file(".")).aggregate(framework, frontend,
  KKParser, RecommendationMLlib, GephiWebsiteGraph)
