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

import gj.shared.api.DashBoardAPI
import gj.ui.API.{ AvailableGraphs, Dispatcher, Event }
import org.scalajs.dom.{ MessageEvent, EventSource }

import scala.scalajs.js
import scala.scalajs.js.{ JSON, UndefOr }
import scala.util.Success

object GraphStores {

  case class Graph(name: String, active: Boolean)

  API.Dispatcher.subscribe {
    case API.AddActiveGraph(name) =>
      changeGraphStatus(name, true)
      buildES()
    case API.RemoveActiveGraph(name) =>
      changeGraphStatus(name, false)
      buildES()
    case API.RefreshAvailableGraphs => refreshAvailableGraphs()
  }
  def refreshAvailableGraphs(): Unit = {
    import autowire._
    import scalajs.concurrent.JSExecutionContext.Implicits.runNow
    val f = API.Client[DashBoardAPI].listBuckets().call()
    f.onComplete {
      case Success(l) => {
        val ag = activeGraphs()
        graphs = l.map(b => Graph(b.name, ag.contains(b.name)))
        Dispatcher.dispatch(AvailableGraphs(graphs))
      }
      case _ =>
    }
  }

  def activeGraphs(): Set[String] = (graphs filter (_.active) map (_.name)).toSet

  private var graphs = List.empty[Graph]

  private def changeGraphStatus(name: String, active: Boolean) = {
    graphs = graphs.map(g => if (g.name == name) Graph(name, active) else g)
    Dispatcher.dispatch(AvailableGraphs(graphs))
  }

  private def buildES() = {
    def dispatchEvent(e: MessageEvent): Unit = {
      org.scalajs.dom.console.info(e)
      val parsed = JSON.parse(e.data.toString)
      Dispatcher.dispatch(API.GraphValue(parsed.metric.asInstanceOf[String],
        parsed.ts.asInstanceOf[Double].toLong,
        parsed.value.asInstanceOf[Double].toLong))
    }

    if (es.isDefined) {
      es.get.close()
    }
    es = new EventSource("/values/" + activeGraphs().mkString("/"))
    es.get.onmessage = dispatchEvent _
  }
  private var es: UndefOr[EventSource] = js.undefined
}
