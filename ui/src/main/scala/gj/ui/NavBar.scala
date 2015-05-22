package gj.ui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object NavBar {
  case class Props(currentTabId: TabId, changeHandler: (TabId) => Unit)
  val component = ReactComponentB[Props]("NavBar")
    .render { props =>

      def renderViewMenu(caption: String, tabId: TabId) =
        <.li(props.currentTabId == tabId ?= (^.className := "active"),
          <.a(
            ^.onClick --> props.changeHandler(tabId),
            caption))

      <.div(^.cls := "navbar navbar-inverse navbar-fixed-top", ^.role := "navigation",
        <.div(^.cls := "container-fluid",
          <.div(^.cls := "navbar-header",
            <.a(^.cls := "navbar-brand", ^.href := "#", "Graph Junkie")
          ),
          <.div(^.className := "navbar-collapse collapse",
            <.span(^.className := "navbar-form navbar-right",
              <.input(^.tpe := "text", ^.className := "form-control", ^.placeholder := "Search...")),
            <.ul(^.className := "nav navbar-nav ",
              List("DashBoard" -> TabId.DashBoardTabId, "Settings" -> TabId.SettingsTabId) map { case (caption, id) => renderViewMenu(caption, id) }),
            <.ul(^.className := "nav navbar-nav navbar-right",
              <.li(<.a("Help"))))))
    }
    .build
}
