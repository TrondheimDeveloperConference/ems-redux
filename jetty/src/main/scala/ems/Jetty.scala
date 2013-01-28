package ems

import java.io.File
import no.java.ems.Resources
import no.java.ems.security.JAASAuthenticator
import scala.util.Properties

object Jetty extends App {
  val port = Properties.envOrElse("PORT", "8081").toInt

  if (System.getProperties.contains("ems-server")) {
    System.setProperty("ems-server", "/server/")
  }

  unfiltered.jetty.Http(port).
  context("/cake") {
    _.filter(new ems.Application).filter(EmsProxy).resources(new File(getRoot, "cake/src/main/webapp").toURI.toURL)
  }.context("/server"){
    _.filter(Resources(JAASAuthenticator))
  } run()

  def getRoot: File = {
    var parent = new File(getClass.getProtectionDomain.getCodeSource.getLocation.getFile)
    while (!new File(parent, "jetty").exists()) {
      parent = parent.getAbsoluteFile.getParentFile
    }
    parent
  }
}
