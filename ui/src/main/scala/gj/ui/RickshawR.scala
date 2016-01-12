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

import gj.ui.API.Dispatcher
import gj.ui.API.Dispatcher.EventReceiver
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.{ UndefOr, Dynamic }
import UndefOr._
import scala.scalajs.js.Dynamic.{ global, newInstance }
import scala.scalajs.js.annotation.JSName

object RickshawR {
  type Props = String
  type State = UndefOr[(RickshawGraph, Serie)]
  val component = ReactComponentB[Props]("RickshawR")
    .initialState(js.undefined: State)
    .backend(new Backend(_))
    .render(props => <.div())
    .componentDidMount { scope =>
      val node = scope.getDOMNode().asInstanceOf[Div]

      val serie = Dynamic.literal(color = "steelblue", name = scope.props, data = js.Array()).asInstanceOf[Serie]

      val conf = Dynamic.literal(
        element = node,
        width = 500,
        heigth = 220,
        renderer = "line",
        series = js.Array(serie))
      val graph = new RickshawGraph(conf)
      val xaxis = newInstance(global.Rickshaw.Graph.Axis.Time)(Dynamic.literal(
        graph = graph,
        ticksTreatment = "glow",
        timeFixture = newInstance(global.Rickshaw.Fixtures.Time)()))

      graph.render()
      scope.setState((graph, serie))
      //      serie.data.push(Data(0, 100))
      //      serie.data.push(Data(1, 200))
      graph.update()
    }

    //    .configure(extra.LogLifecycle.short)
    .componentDidMount(scope => Dispatcher.subscribe(scope.backend.eventHandler))
    .build

  class Backend(scope: BackendScope[Props, State]) {
    val metric = scope.props
    def eventHandler: EventReceiver = {
      case API.GraphValue(`metric`, ts, value) =>
        val (g, s) = scope.state.get
        if (s.data.length > 10)
          s.data.shift()
        s.data.push(Data((ts / 1000).toInt, value.toInt))
        g.update()
    }

  }
}

@JSName("Rickshaw.Graph")
class RickshawGraph(conf: js.Object) extends js.Object {
  def render(): Unit = js.native
  def update(): Unit = js.native
}
trait Serie extends js.Object {
  val data: js.Array[Data] = js.native
}
trait Data extends js.Object {
  val x: Int = js.native
  val y: Int = js.native
}
object Data {
  def apply(x: Int, y: Int) = Dynamic.literal(x = x, y = y).asInstanceOf[Data]
}

trait RickshawGraphConf extends js.Object {
  var element: HTMLElement = js.native
  var width: Int = js.native
}