name := "app-framework"

version := "dist"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.10.4"

//Uncomment to add parameters to javac in SBT
//javacOptions ++= Seq("-Xlint:deprecation")

// Prevent ScalaDoc generation and packaging
publishArtifact in (Compile, packageDoc) := false

publishArtifact in packageDoc := false

sources in (Compile,doc) := Seq.empty

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs
)
