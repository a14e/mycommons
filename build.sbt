
name := "mycommons"

version := "0.2.03"


organization := "com.github.a14e"

scalaVersion := "2.12.6"


val akkaHttpVersion = "10.1.3"
val akkaStreamsVersion = "2.5.14"


libraryDependencies ++= Seq(

  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion, // для http
  "com.typesafe.akka" %% "akka-stream" % akkaStreamsVersion,

  "com.softwaremill.macwire" %% "macros" % "2.3.1" % "provided",
  "com.softwaremill.macwire" %% "macrosakka" % "2.3.1" % "provided",
  "com.softwaremill.macwire" %% "util" % "2.3.1",
  "com.softwaremill.macwire" %% "proxy" % "2.3.1",


  "com.google.guava" % "guava" % "25.1-jre", // для полезных утилит (пока только кэш)

  /** чтобы гуава не жаловалась */
  "com.google.code.findbugs" % "jsr305" % "3.0.1",


  // для конфигов
  "com.iheart" %% "ficus" % "1.4.3",

  // для крутых фьюч
  "org.scala-lang.modules" %% "scala-async" % "0.9.7",


  "ch.qos.logback" % "logback-classic" % "1.2.3", // для логов
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0", //упрощенные логи

  "org.scala-lang.modules" %% "scala-async" % "0.9.6",

  // для сваггера
  "com.github.swagger-akka-http" %% "swagger-akka-http" % "0.14.0",
  "org.webjars" % "swagger-ui" % "3.0.10",
  "org.webjars" % "webjars-locator" % "0.32",

  "de.heikoseeberger" %% "akka-http-circe" % "1.21.0",


  "org.scalatest" %% "scalatest" % "3.0.5" % "test", // для тестов
  "junit" % "junit" % "4.12" % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test",

  "org.mindrot" % "jbcrypt" % "0.4",

  // для корсов
  "ch.megard" %% "akka-http-cors" % "0.2.2"


)

val circeVersion = "0.9.3"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-java8",
  "io.circe" %% "circe-generic-extras"
).map(_ % circeVersion)

javacOptions in(Compile, compile) ++= {
  val javaVersion = "1.8"
  Seq("-source", javaVersion, "-target", javaVersion)
}



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

pgpReadOnly := false
//useGpg := true