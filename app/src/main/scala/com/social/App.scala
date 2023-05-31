package com.social

import scala.scalajs.js.annotation.*
import org.scalajs.dom.document

@JSExportTopLevel("App")
class App {
  @JSExport
  def doSomething(containerId: String): Unit =
    document.getElementById(containerId).innerHTML = "very social🦁!"
}
