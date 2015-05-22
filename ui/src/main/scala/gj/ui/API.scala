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

import org.scalajs.dom

import scala.concurrent.Future

import scalajs.concurrent.JSExecutionContext.Implicits.runNow

object API {

  object Client extends autowire.Client[String, upickle.Reader, upickle.Writer] {
    override def doCall(req: Request): Future[String] = {
      dom.ext.Ajax.post(
        url = "/api/" + req.path.mkString("/"),
        data = upickle.write(req.args)
      ).map(_.responseText)
    }

    def read[Result: upickle.Reader](p: String) = upickle.read[Result](p)
    def write[Result: upickle.Writer](r: Result) = upickle.write(r)
  }

}
