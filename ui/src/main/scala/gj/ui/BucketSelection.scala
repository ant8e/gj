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
