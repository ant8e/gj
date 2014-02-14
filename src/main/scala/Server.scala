package gj

import gj.actor.MetricRepository.{ MetricListResponse, MetricListQuery }
import gj.metric.Metric
import java.net.{ InetAddress, InetSocketAddress }
import scala.concurrent.duration._
import akka.pattern.ask
import akka.io.{ Udp, IO }
import akka.util.Timeout
import akka.actor._
import gj.actor._
import scala.concurrent.Future

trait MetricServerConfiguration {
  /**
   * The address of the interface to bind the server to.
   * if not specified will try to bind to all interface.
   *
   */
  def localAddress: Option[String]

  /**
   * the port number
   */
  def port: Int
}

trait ActorSystemProvider {
  def actorSystem: ActorSystem
}

trait MetricServer {
  self: MetricServerConfiguration with ActorSystemProvider ⇒

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

  // we bind the server to a port on the supplied address and hook
  // in a continuation that informs us when bound
  private val endpoint = localAddress match {
    case Some(a) ⇒ new InetSocketAddress(InetAddress.getByName(a), port)
    case _ ⇒ new InetSocketAddress(port)
  }
  private implicit val bindingTimeout = Timeout(1.second)

  // execution context for the future

  import system.dispatcher

  //
  (IO(Udp) ? Udp.Bind(server, endpoint)).onSuccess {
    case Udp.Bound(address) ⇒
      println("\nBound metric-server to " + address)

    case Udp.CommandFailed(c) ⇒ {
      system.log.error("Unable to start " + c.failureMessage.toString)
      //      system.shutdown()
    }
  }

  def listMetrics: Future[Iterable[Metric]] = (repo ? MetricListQuery).map(_.asInstanceOf[MetricListResponse].metrics)

}

/**
 * Listen to UDP messages and fed them to the decoding actors
 */
class MetricUdpListener(val handler: ActorRef) extends Actor with ActorLogging {

  import RawMetricHandler.MetricRawString

  def receive = {
    // transform the UDP payload to an UTF-8 String and send it to the handler
    case Udp.Received(data, send) ⇒ log.debug("received {} from {}", data.utf8String, send.getAddress.toString); handler ! MetricRawString(data.utf8String)
  }
}

object MetricUdpListener {
  def props(ref: ActorRef): Props = Props(new MetricUdpListener(ref))
}

object Main extends App with MetricServer with MetricServerConfiguration with ActorSystemProvider {
  override def actorSystem: ActorSystem = ActorSystem("Metric-Server")

  override def port: Int = 12344

  override def localAddress = Some("localhost")
}

