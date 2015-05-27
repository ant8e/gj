/*
 *  Copyright © 2015 Antoine Comte
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

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

    val store = GraphStores

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

