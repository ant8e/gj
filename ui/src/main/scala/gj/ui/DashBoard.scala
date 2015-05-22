package gj.ui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object DashBoard {
  val component = ReactComponentB[Unit]("DashBoard")
    .render(_ => <.div(BucketSelection.component(), GraphPanel.component()))
    .buildU
}
