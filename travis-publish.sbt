import scala.util.Properties

credentials ++= { 
  (for {
  username <- Properties.envOrNone("NEXUS_USERNAME")
  password <- Properties.envOrNone("NEXUS_PASSWORD")
} yield 
  Credentials(
    "Sonatype Nexus Repository Manager", 
    "oss.sonatype.org", 
    username, 
    password
  )).toSeq
}
