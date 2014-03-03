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

package ui

import gj.metric.{ MetricValueAt, Metric }
import gj.{ MetricProvider, ActorSystemProvider }
import spray.routing.{ Route, PathMatchers, HttpService, SimpleRoutingApp }
import akka.actor.{ ActorRef, Actor, ActorRefFactory, Props }
import ServerSideEventsDirectives._
import spray.http.StatusCodes

/**
 *
 */

trait UIServerRoute extends HttpService {
  self: MetricProvider ⇒

  def staticRoutes = get {
    decompressRequest()
    compressResponse() {
      pathSingleSlash {
        getFromResource("web/index.html")
      } ~
        getFromResourceDirectory("web")
    }
  }

  def apiRoutes = pathPrefix("api") {
    get {
      _.complete("Todo")
    }
  }

  def valueRoute = path("bucket" / PathMatchers.RestPath) {
    b ⇒
      dynamic {
        implicit val ex = actorRefFactory.dispatcher
        onSuccess(findMetric(b.toString())) {
          _ match {
            case Some(m) ⇒ metricValueStream(m)
            case _ ⇒ complete(StatusCodes.NotFound)
          }
        }
      }
  }

  def metricValueStream(m: Metric): Route = {

    val buildStreamActor = (sseChannel: ActorRef, lastEvent: Option[String]) ⇒ {
      val valueActor = implicitly[ActorRefFactory].actorOf(Props(new ValueActor(sseChannel, m)))
      subscribe(m, valueActor)
    }

    sse {
      buildStreamActor
    }
  }

  def routes = apiRoutes ~ valueRoute ~ staticRoutes

}

trait UiServer extends SimpleRoutingApp with UIServerRoute {
  self: ActorSystemProvider with UiServerConfiguration with MetricProvider ⇒

  implicit val sprayActorSystem = this.actorSystem
  startServer("", UiServerPort)(routes)

}

trait UiServerConfiguration {
  def UiServerPort: Int
}

class ValueActor(channel: ActorRef, metric: Metric) extends Actor {

  import scala.concurrent.duration._
  import context._

  system.scheduler.schedule(1.second, 1.second, self, "tick")

  channel ! RegisterClosedHandler(() ⇒ context.stop(self))

  override def receive: Actor.Receive = {
    case v: MetricValueAt[_] ⇒ channel ! Message(toJson(v.asInstanceOf[MetricValueAt[metric.type]]))
  }

  def toJson(mv: MetricValueAt[metric.type]) = s"""{"value" : ${mv.value}, "ts":${mv.timestamp}}"""
}
