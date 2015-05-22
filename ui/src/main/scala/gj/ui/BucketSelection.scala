package gj.ui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object BucketSelection {
  case class Bucket(name: String, active: Boolean)
  type State = List[Bucket]

  val component = ReactComponentB[Unit]("BucketSelection")
    .initialState(List.empty[Bucket])
    .render((_, state) =>
      <.div(^.className := "col-sm-3 col-md-2 sidebar",
        <.ul(^.className := "list-group",
          state map { b =>
            <.li(^.key := b.name, ^.className := "list-group-item",
              b.name + " ",
              <.i(b.active ?= (^.className := "fa fa-bar-chart-o")),
              <.span(^.className := "pull-right",
                <.button(^.tpe := "button", ^.className := "btn btn-default btn-xs", ^.disabled := b.active,
                  <.span(^.className := "glyphicon glyphicon-plus")),
                <.button(^.tpe := "button", ^.className := "btn btn-default btn-xs", ^.disabled := !b.active,
                  <.span(^.className := "glyphicon glyphicon-minus"))))
          }
        )))
    .componentDidMount(scope => scope.modState(s => List(Bucket("aa", true))))
    .buildU

}
