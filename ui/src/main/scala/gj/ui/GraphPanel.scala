package gj.ui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object GraphPanel {
  val component = ReactComponentB[Unit]("GraphPanel")
    .render(_ => <.div())
    .buildU
}
