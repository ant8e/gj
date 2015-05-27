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
import gj.ui.API.{ AvailableGraphs, Dispatcher, Event }

import scala.util.Success

object GraphStores {

  case class Graph(name: String, active: Boolean)

  API.Dispatcher.subscribe {
    case API.AddActiveGraph(name) => changeGraphStatus(name, true)
    case API.RemoveActiveGraph(name) => changeGraphStatus(name, false)
    case API.RefreshAvailableGraphs => refreshAvailableGraphs()
  }
  def refreshAvailableGraphs(): Unit = {
    import autowire._
    import scalajs.concurrent.JSExecutionContext.Implicits.runNow
    val f = API.Client[GjAPI].listBuckets().call()
    f.onComplete {
      case Success(l) => {
        val activeGraphs = (graphs filter (_.active) map (_.name)).toSet
        graphs = l.map(b => Graph(b.name, activeGraphs.contains(b.name)))
        Dispatcher.dispatch(AvailableGraphs(graphs))
      }
      case _ =>
    }
  }

  private var graphs = List.empty[Graph]

  private def changeGraphStatus(name: String, active: Boolean) = {
    graphs = graphs.map(g => if (g.name == name) Graph(name, active) else g)
    Dispatcher.dispatch(AvailableGraphs(graphs))
  }
}
