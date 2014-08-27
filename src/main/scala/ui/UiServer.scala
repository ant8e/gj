

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

import akka.actor._
import gj.metric.{MetricValueAt, _}
import gj.{ComponentConfiguration, MetricProvider, ActorSystemProvider}
import spray.http.CacheDirectives.public
import spray.http.CacheDirectives.`max-age`
import spray.routing._
import akka.actor.{ActorRefFactory, ActorRef, Actor, Props}
import spray.http.{HttpHeaders, StatusCodes}
import spray.http.HttpHeaders.`Cache-Control`
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.can.Http
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.routing.directives.CachingDirectives
import scala.concurrent.duration._
import scala.concurrent.{Future, ExecutionContext}
import scala.Some
import ui.ValueStreamBridge.{CallBack, RegisterStopHandler}

/**
 * UI Server configuration
 */
trait UiServerConfiguration {
  /**
   * UI server listening port
   */
  def uiServerPort: Int

  /**
   * Specific interface address the server tries to bind to.
   * If None, the server will try to bind the default address specified by [[UiServerConfiguration.defaultInterface]]]
   */
  def uiServerBindAddress: Option[String] = None

}

object UiServerConfiguration {

  /**
   * Default interface
   */
  val defaultInterface = "localhost"

  /**
   * represent all interfaces (IPV4 & IPV6)
   * @example uiServerBindAddress = allInterfaces
   */
  val allInterfaces = Some("::")
}

/**
 * Spray HttpService to serve the UI (css,html,js) + SSE value streams
 */
trait UiServer extends ComponentConfiguration {
  self: ActorSystemProvider with MetricProvider ⇒

  type Config <: UiServerConfiguration
  val serviceName = "gj-ui-service"

  //Getting the actor system from the mixed ActorSystemProvider trait and making it implicit
  private implicit val system: ActorSystem = actorSystem

  //Creating the UiServiceActor, passing the mixed in MetricProvider trait
  private val serviceActor = system.actorOf(Props(new UIServiceActor(this)), serviceName)

  private implicit val ec: ExecutionContext = system.dispatcher
  private implicit val bindingTimeout = Timeout(1.second)

  //Binding the service actor to the address and port supplied by the UiServerConfiguration trait
  private val interface: String = config.uiServerBindAddress.getOrElse(UiServerConfiguration.defaultInterface)
  (IO(Http) ? Http.Bind(serviceActor, interface, config.uiServerPort)).onSuccess {
    case Http.Bound(address) ⇒ system.log.info(s"$serviceName bound to ${address.toString}")
    case e: Any ⇒
      system.log.error(s"Unable to bind $serviceName : $e ")
      system.shutdown()
  }

}

/**
 * Service Actor to run our Service
 */

class UIServiceActor(val metricProvider: MetricProvider) extends Actor with UIService {

  override def receive = runRoute(routes)

  override def actorRefFactory: ActorRefFactory = context
}

/**
 * This trait defines the UI service behavior
 */
trait UIService extends HttpService with SprayJsonSupport {
  /**
   * a MetricProvider needs to be supplied by the class mixing us in
   * @return
   */
  def metricProvider: MetricProvider

  /**
   * representation of a Bucket
   * @param name
   */
  case class BucketResponse(name: String)

  /**
   * Json Serialisation protocol
   */
  object MyJsonProtocol extends DefaultJsonProtocol {
    implicit val bucketResponseFormat = jsonFormat1(BucketResponse)
  }

  import MyJsonProtocol._

  /**
   *
   */
  def apiRoutes = pathPrefix("api") {
    get {
      implicit val ex = actorRefFactory.dispatcher
      path("buckets") {
        complete {
          metricProvider.listMetrics map (_ map (m ⇒ BucketResponse(m.bucket.name)))
        }
      }
    }
  }

  /**
   * values route
   */
  def valuesRoute = pathPrefix("values" / Segments) {
    b ⇒
      pathEndOrSingleSlash {
        dynamic {
          implicit val ex = actorRefFactory.dispatcher
          onSuccess(listMetric(b)) {
            lm ⇒
              if (lm.isEmpty)
                complete(StatusCodes.NotFound)
              else metricValueStream(lm: _*)
          }
        }
      }
  }

  def listMetric(names: List[String])(implicit ec: ExecutionContext): Future[List[Metric]] = {
    val l: List[Future[Option[Metric]]] = names map {
      metricProvider findMetric _
    }

    Future.fold(l)(List[Metric]())((acc, f) ⇒ f match {
      case Some(v) ⇒ v :: acc
      case _ ⇒ acc
    })
  }

  /**
   * Build a route that stream the values of the specified metric
   * @param metrics the metrics
   * @return A sse route, streaming an event for each value
   */
  def metricValueStream(metrics: Metric*)(implicit actorRefFactory: ActorRefFactory): Route = {

    import ServerSideEventsDirectives.sse

    sse {
      (sseChannel: ActorRef, lastEvent: Option[String]) ⇒ {
        for (m ← metrics) {
          val valueActor = actorRefFactory.actorOf(Props(new ValueStreamBridge(sseChannel, m)))
          metricProvider subscribe(m, valueActor)
          valueActor ! ValueStreamBridge.RegisterStopHandler(() ⇒ metricProvider unSubscribe(m, valueActor))
        }
      }
    }
  }

  /**
   * Route for the static content (hmtl,css,....)
   */
  def staticRoutes: Route = get {
    // Static content can live forever in cache
    import CachingDirectives._
    cache(routeCache()) {
      // Serving webjars from the wj prefix
      pathPrefix("wj") {
        //Allow clients to cache js librairies for one day
        respondWithHeader(`Cache-Control`(`max-age`(1.day.toSeconds))) {
          getFromResourceDirectory("META-INF/resources/webjars")
        }
      } ~
           //Allow clients to cache js/css/html for one hour
        respondWithHeader(`Cache-Control`(`max-age`(1.hour.toSeconds))) {
        // Then anything form the web directory
        getFromResourceDirectory("web") ~
          // Finaly, fallback to the index
          getFromResource("web/index.html")
      }
    }
  }

  def routes: Route = compressResponseIfRequested() {
    decompressRequest() {
      apiRoutes ~
        valuesRoute ~
        staticRoutes
    }
  }

}

/**
 * Subscribe to a Metric value stream and push every value to a SSE channel as a JSON representation
 *
 * @param sseChannel  the
 * @param metric
 */
class ValueStreamBridge(sseChannel: ActorRef, metric: Metric) extends Actor {

  import ServerSideEventsDirectives.{Message, RegisterClosedHandler}
  import ValueStreamBridge.Stop

  private var stopHandler: List[CallBack] = List()

  private def handler() = {
    self ! Stop
    context.stop(self)
  }

  sseChannel ! RegisterClosedHandler(handler)

  override def receive: Actor.Receive = {
    case v: MetricValueAt[_] ⇒ sseChannel ! Message(toJson(v))
    case RegisterStopHandler(h) ⇒ stopHandler = stopHandler :+ h
    case Stop ⇒ stopHandler foreach (h ⇒ h())
  }

  def toJson(mv: MetricValueAt[_ <: Metric]) = s"""{"metric":"${mv.metric.bucket.name}","value":${mv.value},"ts":${mv.timestamp}}"""
}

object ValueStreamBridge {
  type CallBack = () ⇒ Unit

  object Stop

  case class RegisterStopHandler(handler: CallBack)

}
