/*
 * Copyright © 2014 Antoine Comte
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import spray.http._
import spray.httpx.unmarshalling._

import spray.httpx.encoding.Gzip
import spray.testkit.ScalatestRouteTest
import ui.UIServerRoute

/**
 *
 */
class UiServerSpec extends FunSpec with ScalatestRouteTest with UIServerRoute with MustMatchers {
  def actorRefFactory = system


  val `text/html(UTF8)` = ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`)

  describe("the UI server") {
    it("should serve the index html file") {
      Get() ~>  routes ~> check {
        contentType must equal(`text/html(UTF8)`)
        Gzip.decode(response).as[String].right.get must include("<!doctype html>")
      }

    }
  }

}
