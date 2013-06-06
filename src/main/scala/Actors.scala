import akka.actor.{Props, ActorLogging, Actor}
import akka.io.Udp
import akka.routing.RoundRobinRouter
import scala.util.{Failure, Success, Try}


//Messages
case class PossiblyMultipleMetricRawString(s: String)

case class SingleMetricRawString(s: String)


class MetricServerActor extends Actor with ActorLogging {
  // Metric Handler
  val handler = context.actorOf(Props[MetricCoordinatorActor])

  def receive = {
    // transform the UDP payload to an UTF-8 String and send it to a decoder
    case Udp.Received(data, _) ⇒ handler ! PossiblyMultipleMetricRawString((data.decodeString("UTF-8")))
  }

}

class MetricCoordinatorActor extends Actor with ActorLogging {

  val splitter = context.actorOf(Props[MessageSplitterActor].withRouter(RoundRobinRouter(5)))
  val decoder = context.actorOf(Props[MetricDecoderActor].withRouter(RoundRobinRouter(5)))

  def receive = {
    case m: PossiblyMultipleMetricRawString ⇒ splitter ! m
    case m: SingleMetricRawString ⇒ decoder ! m
    case m: MetricStyle ⇒ log.info("received metric for " + m.bucket.name)
  }
}

/**
 * Actor used to split incoming message into their single form
 */
class MessageSplitterActor extends Actor {

  def receive = {
    case PossiblyMultipleMetricRawString(s) ⇒ s.lines.foreach(sm ⇒ sender ! SingleMetricRawString(sm))
  }
}

/**
 * This actor decodes a raw metric representation into a MetricOP with an associated MetricStyle
 */
class MetricDecoderActor extends Actor with ActorLogging {

  def receive = {
    case SingleMetricRawString(m) ⇒ parse(m) match {
      case Success(op) ⇒ sender ! op
      case Failure(e) ⇒ log.debug("unable to parse message {} because {}", m, e)
    }
  }
  //Metric parsing regex
  val ParsingRegExp = """(.*):([+-]?\d+)\|([cgs]|ms)(|@[.\d]+)?""".r
  val SignedDigit = "[+-]\\d+".r

  /**
   * Parse the raw metric
   * Currently ignoring the sampling of counters
   * @param rawString the metric string
   * @return  Success if everything went right Failure instead
   */
  private def parse(rawString: String): Try[MetricOp with MetricStyle] = Try {
    //TODO support counter sampling param
    val ParsingRegExp(bucket, value, style, _) = rawString.trim
    val b = SimpleBucket(bucket)
    val v: Int = value.toInt

    style match {
      case "c" ⇒ new IncOp(b, v) with CounterStyle
      case "ms" ⇒ new SetOp(b, v) with TimingStyle
      case "g" ⇒ value match {
        case SignedDigit() ⇒ new IncOp(b, v) with GaugeStyle
        case _ ⇒ new SetOp(b, v) with GaugeStyle
      }
      case "s" ⇒ new SetOp(b, v) with DistinctStyle

    }
  }

}