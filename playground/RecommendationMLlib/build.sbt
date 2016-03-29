name := "RecommendationMLlib"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies  ++= Seq(
  "org.apache.spark" %% "spark-core"  % "1.5.0",
  "org.apache.spark" %% "spark-mllib" % "1.5.0"
)
