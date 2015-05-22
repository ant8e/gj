package gj.ui

import gj.shared.api.GjAPI
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.util.Success

object BucketSelection {

  case class Bucket(name: String, active: Boolean)
  type State = List[Bucket]

  import scalajs.concurrent.JSExecutionContext.runNow
  implicit val ec = runNow
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
    .componentDidMount(scope => loadBuckets onComplete {
      case Success(l) => scope.modState(s => l map (b => Bucket(b.name, false)))
      case _ =>
    })
    .buildU

  def loadBuckets() = {
    import autowire._
    API.Client[GjAPI].listBuckets().call()
  }

}
