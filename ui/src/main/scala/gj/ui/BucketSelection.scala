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

import gj.ui.API.{ AddActiveGraph, Dispatcher, RemoveActiveGraph }
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object BucketSelection {

  type Props = List[GraphStores.Graph]

  class Backend(scope: BackendScope[Props, _]) {
    def activate(name: String) = {
      Dispatcher.dispatch(AddActiveGraph(name))
    }

    def inactivate(name: String) = {
      Dispatcher.dispatch(RemoveActiveGraph(name))
    }
  }

  val component = ReactComponentB[Props]("BucketSelection")
    .stateless
    .backend(new Backend(_))
    .render((props, _, backend) =>
      <.div(
        ^.className := "col-sm-3 col-md-2 sidebar",
        <.ul(
          ^.className := "list-group",
          props map { b =>
            <.li(^.key := b.name, ^.className := "list-group-item",
              b.name + " ",
              <.i(b.active ?= (^.className := "fa fa-bar-chart-o")),
              <.span(
                ^.className := "pull-right",
                <.button(
                  ^.tpe := "button",
                  ^.className := "btn btn-default btn-xs",
                  ^.disabled := b.active,
                  ^.onClick --> backend.activate(b.name),
                  <.span(^.className := "glyphicon glyphicon-plus")),
                <.button(
                  ^.tpe := "button",
                  ^.className := "btn btn-default btn-xs",
                  ^.disabled := !b.active,
                  ^.onClick --> backend.inactivate(b.name),
                  <.span(^.className := "glyphicon glyphicon-minus"))))
          })))
    .build

}
