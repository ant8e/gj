package net.antoinecomte.gj
import java.net.InetSocketAddress
import scala.concurrent.duration._
import akka.pattern.ask
import akka.io.{ Udp, IO }
import akka.util.Timeout
import akka.actor._

class Server {

}

object Main extends App {
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("metric-server")

  // and our actual server "service" actor
  val server = system.actorOf(Props[MetricServerActor], name = "metric-server")

  // we bind the server to a port on localhost and hook
  // in a continuation that informs us when bound
  val endpoint = new InetSocketAddress("localhost", 12344)
  implicit val bindingTimeout = Timeout(1.second)

  import system.dispatcher

  // execution context for the future

  val boundFuture = IO(Udp) ? Udp.Bind(server, endpoint)

  boundFuture.onSuccess {
    case Udp.Bound(address) ⇒
      println("\nBound echo-server to " + address)

    case Udp.CommandFailed(c) ⇒ {
      system.log.error("Unable to start " + c.failureMessage.toString)
      system.shutdown()
    }
  }

}

