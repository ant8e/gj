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

import akka.actor.{ ActorRef, ActorSystem }
import gj.metric.{ LongCounter, SimpleBucket, _ }
import gj.{ ActorSystemProvider, MetricProvider }
import org.scalatest.{ FunSpec, MustMatchers }
import spray.http._
import spray.httpx.encoding.Gzip
import spray.httpx.unmarshalling._
import spray.testkit.ScalatestRouteTest

import scala.concurrent.Future

class UiServerSpec extends FunSpec with ScalatestRouteTest with UIService with MustMatchers with MetricProvider with ActorSystemProvider {
  def actorRefFactory = system

  val `text/html(UTF8)` = ContentType(MediaTypes.`text/html`, HttpCharsets.`UTF-8`)

  describe("the UI server") {
    it("should serve the index html file") {
      Get() ~> routes ~> check {
        contentType must equal(`text/html(UTF8)`)
        Gzip.decode(response).as[String].right.get must include("<!doctype html>")
      }
    }
    it("should serve the list of active buckets") {
      pending
    }
  }

  override def unSubscribe(metric: Metric, receiver: ActorRef): Unit = {}
  override def subscribe(metric: Metric, receiver: ActorRef): Unit = {}
  override def listMetrics: Future[Seq[Metric]] = Future.successful(List(LongCounter(SimpleBucket("test.bucket"))))
  override def actorSystem: ActorSystem = system
  override def metricProvider: MetricProvider = this
}
