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

package gj

import gj.actor.MetricRepository.{ MetricListResponse, MetricListQuery }
import gj.actor.ValuesProvider.{ UnSubscribe, Subscribe }
import gj.metric._
import java.net.{ InetAddress, InetSocketAddress }
import scala.concurrent.duration._
import akka.pattern.ask
import akka.io.{ Udp, IO }
import akka.util.Timeout
import akka.actor._
import gj.actor._
import scala.concurrent.Future
import ui.{ UiServerConfiguration, UiServer }
import scala.language.postfixOps

trait ComponentConfiguration {
  type Config

  def config: Config
}

trait ActorSystemProvider {
  def actorSystem: ActorSystem
}

trait MetricProvider {
  self: ActorSystemProvider ⇒

  private implicit val ex = actorSystem.dispatcher

  /**
   * List all known metrics
   * @return a Future that will complete with the known metrics
   */
  def listMetrics: Future[Seq[Metric]]

  /**
   * Test if a metric is known by the system
   * @param name name of the metric
   * @return a future that will complete with true if a metric with the supplied name is known by the system
   */
  def hasMetric(name: String): Future[Boolean] = findMetric(name) map (_.isDefined)

  /**
   * Find a metric
   * @param name name of the metric
   * @return a future that will complete with Some(metric) if a metric with the supplied name is known by the system, None otherwise
   */
  def findMetric(name: String): Future[Option[Metric]] = listMetrics map (_ find (m ⇒ m.bucket.name == name))

  def subscribe(metric: Metric, receiver: ActorRef)

  def unSubscribe(metric: Metric, receiver: ActorRef)

}

trait MetricServerConfiguration {
  /**
   * The address of the interface to bind the server to.
   * if not specified will try to bind to all interface.
   *
   */
  def metricLocalAddress: Option[String]

  /**
   * the port number
   */
  def metricServerPort: Int
}

trait MetricServer extends MetricProvider with ComponentConfiguration {
  self: ActorSystemProvider ⇒

  type Config <: MetricServerConfiguration

  // we need an ActorSystem to host our application in
  private implicit val system: ActorSystem = actorSystem

  //Metric Repository
  private val repo = system.actorOf(MetricRepository.props)

  //Metric ValueProvider
  val valueProvider = system.actorOf(ValuesProvider.props(repo))

  // Metric Handler
  private val handler = system.actorOf(RawMetricHandler.props(repo))

  // and our actual server "service" actor
  private val server = system.actorOf(MetricUdpListener.props(handler), name = "metric-server")

  private val endpoint = new InetSocketAddress(config.metricLocalAddress.getOrElse("::"), config.metricServerPort)

  private implicit val bindingTimeout = Timeout(1.second)

  // execution context for the future

  import system.dispatcher

  // we bind the server to a port on the supplied address and hook
  // in a continuation that informs us when bound
  (IO(Udp) ? Udp.Bind(server, endpoint)).onSuccess {
    case Udp.Bound(address) ⇒
      system.log.info("Bound metric-server to " + address)

    case Udp.CommandFailed(c) ⇒ {
      system.log.error("Unable to start " + c.failureMessage.toString)
      system.shutdown()
    }
  }

  def listMetrics: Future[Seq[Metric]] = (repo ? MetricListQuery) map (_.asInstanceOf[MetricListResponse].metrics)

  def subscribe(metric: Metric, receiver: ActorRef) = valueProvider.tell(Subscribe(metric), receiver)

  def unSubscribe(metric: Metric, receiver: ActorRef) = valueProvider.tell(UnSubscribe(metric), receiver)

}

/**
 * Listen to UDP messages and fed them to the decoding actors
 */
class MetricUdpListener(val handler: ActorRef) extends Actor with ActorLogging {

  import RawMetricHandler.MetricRawString

  def receive = {
    // transform the UDP payload to an UTF-8 String and send it to the handler
    case Udp.Received(data, send) ⇒ {
      log.debug("received {} from {}", data.utf8String, send.getAddress.toString)
      handler ! MetricRawString(data.utf8String)
    }
  }
}

object MetricUdpListener {
  def props(ref: ActorRef): Props = Props(new MetricUdpListener(ref))
}

object Main extends App with MetricServer with ActorSystemProvider with UiServer {
  lazy val actorSystem: ActorSystem = ActorSystem("Metric-Server")

  trait Config extends MetricServerConfiguration with UiServerConfiguration

  object config extends Config {
    val metricServerPort: Int = 12344
    val metricLocalAddress = None
    val uiServerPort = 8080
    override val uiServerBindAddress = UiServerConfiguration.allInterfaces
  }

}
