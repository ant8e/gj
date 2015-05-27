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

import gj.ui.API.{ RefreshAvailableGraphs, Dispatcher }
import gj.ui.API.Dispatcher.EventReceiver
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import ScalazReact._

object DashBoard {
  type State = List[GraphStores.Graph]
  val st = ReactS.Fix[State]

  val component = ReactComponentB[Unit]("DashBoard")
    .initialState(List.empty[GraphStores.Graph])
    .backend(new Backend(_))
    .render((_, state, backend) =>
      <.div(^.cls := "col-md-12",
        BucketSelection.component(state),
        <.div(^.cls := "col-md-10",
          GraphPanel.component(state filter (_.active) map (_.name)))))
    .componentDidMount(scope => {
      Dispatcher.subscribe(scope.backend.eventHandler)
      Dispatcher.dispatch(RefreshAvailableGraphs)
    })
    .componentWillUnmount(scope =>
      Dispatcher.unSubscribe(scope.backend.eventHandler))
    .configure(extra.LogLifecycle.short)
    .buildU

  class Backend(scope: BackendScope[Unit, State]) {
    def eventHandler: EventReceiver = {
      case API.AvailableGraphs(l) => scope.modState(_ => l)
    }

  }
}
