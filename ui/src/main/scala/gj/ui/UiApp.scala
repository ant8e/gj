package gj.ui

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

/**
 * Created by ant on 04.05.15.
 */
object UiApp extends js.JSApp {
  @JSExport
  override def main(): Unit = org.scalajs.dom.document.getElementById("ui-app").innerHTML = "<strong>It works!</strong>"
}
