package gj.ui

import gj.ui.TabId.DashBoardTabId
import gj.ui._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

/**
 */
object UiApp extends js.JSApp {
  @JSExport
  override def main(): Unit = {
    val mountPoint = org.scalajs.dom.document.getElementById("ui-app")
    React.render(App.component(), mountPoint)
  }

  object App {

    val component = ReactComponentB[Unit]("App")
      .initialState(DashBoardTabId: TabId)
      .backend(new Backend(_))
      .render(scope => <.div(
        NavBar.component(NavBar.Props(scope.state, scope.backend.viewChangeHandler)),
        MainView.component(scope.state)
      ))
      .buildU

    class Backend(scope: BackendScope[Unit, TabId]) {
      def viewChangeHandler(tabId: TabId) = scope.modState(_ => tabId)
    }
  }

}

