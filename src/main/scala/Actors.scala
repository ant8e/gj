import akka.actor.{ActorLogging, Actor}
import scala.util.{Failure, Success, Try}


case class PossiblyMultipleMetricRawString(s: String)

case class SingleMetricRawString(s: String)

/**
 * Actor used to split incoming message into their single form
 */
class MessageSplitterActor extends Actor {

  def receive = {
    case PossiblyMultipleMetricRawString(s) => s.lines.foreach(sm => sender ! SingleMetricRawString(sm))
  }
}


/**
 * This actor decode a raw metric representation into a MetricOP with it's associated MetricStyle message
 */


class MetricDecoderActor extends Actor with ActorLogging {

  def receive = {
    case SingleMetricRawString(m) => parse(m) match {
      case Success(op) => sender ! op
      case Failure(e) => log.debug("unable to parse message {} because {}", m, e)
    }
  }

  val ParsingRegExp = """(.*):([+-]?\d+)\|([cgs]|ms)(|@[.\d]+)?""".r
  val SignedDigit = "[+-]\\d+".r

  /**
   * Parse the raw metric
   * Currently ignoring the sampling of counters
   * @param rawString the metric string
   * @return  Success if everything went right Failure instead
   */
  private def parse(rawString: String): Try[MetricOp with MetricStyle] = Try {
    //TODO support
    val ParsingRegExp(bucket, value, style, rest) = rawString.trim
    val b = SimpleBucket(bucket)
    val v: Int = value.toInt

    style match {
      case "c" => new IncOp(b, v) with CounterStyle
      case "ms" => new SetOp(b, v) with TimingStyle
      case "g" => value match {
        case SignedDigit() => new IncOp(b, v) with GaugeStyle
        case _ => new SetOp(b, v) with GaugeStyle
      }
      case "s" => new SetOp(b, v) with DistinctStyle

    }
  }


}