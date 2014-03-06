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

import gj.metric.{MetricValueAt, Metric}
import gj.{MetricProvider, ActorSystemProvider}
import spray.routing.{Route, PathMatchers, HttpService, SimpleRoutingApp}
import akka.actor.{ActorRef, Actor, ActorRefFactory, Props}
import ServerSideEventsDirectives._
import spray.http.StatusCodes
import ui.ValueStreamBridge.RegStopHandler
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import scala.concurrent.Future

/**
 *
 */

trait UIServerRoute extends HttpService with SprayJsonSupport {
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

  case class BucketResponse(name: String)

  object MyJsonProtocol extends DefaultJsonProtocol {
    implicit val bucketResponseFormat = jsonFormat1(BucketResponse)
  }

  def apiRoutes = pathPrefix("api") {
    get {
      import MyJsonProtocol._
      implicit val ex = actorRefFactory.dispatcher
      path("buckets") {
        complete {
          listMetrics map (_ map (m ⇒ BucketResponse(m.bucket.name)))
        }
      }

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

      val valueActor: ActorRef =
        actorRefFactory.actorOf(Props(new ValueStreamBridge(sseChannel, m)))

      subscribe(m, valueActor)
      valueActor ! RegStopHandler(() ⇒ unSubscribe(m, valueActor))
    }

    sse {
      buildStreamActor
    }
  }

  def routes = apiRoutes ~ valueRoute ~ staticRoutes

}

/**
 * Spray HttpService to serve the UI (css,html,js) + SSE value streams
 */
trait UiServer extends SimpleRoutingApp with UIServerRoute {
  self: ActorSystemProvider with UiServerConfiguration with MetricProvider ⇒

  implicit val sprayActorSystem = this.actorSystem
  startServer("", UiServerPort)(routes)

}

/**
 * UI Server configuration
 */
trait UiServerConfiguration {
  def UiServerPort: Int
}

/**
 * Subscribe to a Metric value stream and push every value to a SSE channel as a JSON representation
 *
 * @param channel  the
 * @param metric
 */
class ValueStreamBridge(channel: ActorRef, metric: Metric) extends Actor {

  var stopHandler: () ⇒ Unit = () ⇒ {}
  channel ! RegisterClosedHandler(() ⇒ {
    stopHandler()
    context.stop(self)
  })

  override def receive: Actor.Receive = {
    case v: MetricValueAt[_] ⇒ channel ! Message(toJson(v.asInstanceOf[MetricValueAt[metric.type]]))
    case RegStopHandler(h) ⇒ stopHandler = h
  }

  def toJson(mv: MetricValueAt[metric.type]) = s"""{"value":${mv.value},"ts":${mv.timestamp}}"""
}

object ValueStreamBridge {

  case class RegStopHandler(hander: () ⇒ Unit)

}
