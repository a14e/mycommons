
name := "mycommons"

version := "0.3.33"


organization := "com.github.a14e"

scalaVersion := "2.13.6"


val akkaStreamsVersion = "2.5.23"
val playJsonVersion = "2.7.4"

resolvers +=
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(

  "com.google.guava" % "guava" % "29.0-jre", // для полезных утилит (пока только кэш)
  "com.github.ben-manes.caffeine" % "caffeine" % "3.0.2",

  /** чтобы гуава не жаловалась */
  "com.google.code.findbugs" % "jsr305" % "3.0.2",


  "ch.qos.logback" % "logback-classic" % "1.2.3", // для логов
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2", //упрощенные логи


  "org.scalatest" %% "scalatest" % "3.0.8" % "test", // для тестов
  "junit" % "junit" % "4.12" % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test",

  "org.reflections" % "reflections" % "0.9.10",

  "org.flywaydb" % "flyway-core" % "4.2.0",


  // для конфигов
  "com.github.pureconfig" %% "pureconfig" % "0.15.0",
)

libraryDependencies += "org.typelevel" %% "cats-effect" % "3.1.1"
libraryDependencies += "com.sun.xml.bind" % "jaxb-impl" % "2.3.3"
libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.3"

libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "6.6"

val circeVersion = "0.13.0"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

javacOptions in(Compile, compile) ++= {
  val javaVersion = "1.8"
  Seq("-source", javaVersion, "-target", javaVersion)
}

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0")

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)

publishArtifact in Test := false

////////////////////
// publishing

pomExtra := {
  <url>https://github.com/a14e/mycommons/</url>
    <licenses>
      <license>
        <name>MIT</name>
        <url>https://github.com/a14e/mycommons/blob/master/LICENSE.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:git@github.com:a14e/mycommons.git</connection>
      <url>https://github.com/a14e/mycommons.git</url>
    </scm>
    <developers>
      <developer>
        <id>AndrewInstance</id>
        <name>Andrew</name>
        <email>m0hct3r@gmail.com</email>
      </developer>
    </developers>
}

publishMavenStyle := true

publishTo := {
  val base = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at base + "content/repositories/snapshots/")
  else
    Some("releases" at base + "service/local/staging/deploy/maven2/")
}
//credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

// только чтобы переписывать фаилы при сборке по http://stackoverflow.com/questions/27530507/sbt-publish-only-when-version-does-not-exist
isSnapshot := true

pomIncludeRepository := { x => false }
//
////pgpReadOnly := false
//useGpg := truea
