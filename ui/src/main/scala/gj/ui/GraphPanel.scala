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

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object GraphPanel {
  type Props = List[String]
  val component = ReactComponentB[Props]("GraphPanel")
    .render(props =>
      <.div(
        ^.cls := "col-sm-9 col-sm-offset-3 col-md-10 col-md-offset-2 main",
        <.h3(^.cls := "page-header", "Dashboard"),
        <.div(
          ^.cls := "row placeholders",
          props.map(b => Graph.component.withKey(b)(b)))))
    //    .configure(extra.LogLifecycle.verbose)
    .build

}
