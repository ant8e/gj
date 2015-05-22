package gj.ui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object MainView {
  val component = ReactComponentB[TabId]("MainView")
    .render(props =>
      <.div(^.className := "container-fluid",
        <.div(^.className := "row",
          props match {
            case TabId.DashBoardTabId => DashBoard.component()
            case TabId.SettingsTabId => Settings.component()
          })))
    .build
}
