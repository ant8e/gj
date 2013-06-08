import akka.actor.{ Props, ActorLogging, Actor }
import akka.io.Udp
import akka.routing.RoundRobinRouter
import scala.reflect.ClassTag
import scala.util.{ Failure, Success, Try }

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
    val v: Int = if (value(0) == '+')
      value.drop(1).toInt // Java 1.6 does not handle the + sign, later version do
    else value.toInt

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

class MetricAggregatorActor extends Actor {
  def receive = {
    case m: MetricOp with MetricStyle ⇒ {
      val child = context.child(metricActorName(m))
      child match {
        case Some(ref) ⇒ ref ! m
        case None ⇒ context.actorOf(Props(buildActor(m)), metricActorName(m)) ! m
      }
    }
  }

  private def metricActorName(m: MetricOp with MetricStyle) = m.styleTag + "#" + m.bucket.name

  private def buildActor(s: MetricStyle): Actor = s match {
    case _: CounterStyle ⇒ new CounterAggregatorWorkerActor()
    case _: TimingStyle ⇒ new TimingAggregatorWorkerActor()
    case _: GaugeStyle ⇒ new GaugeAggregatorWorkerActor()
    case _: DistinctStyle ⇒ new DistinctAggregatorWorkerActor()
  }
}

trait LongValueAggregator {
  protected[this] var _value: Long = 0
  def value = _value
}

class CounterAggregatorWorkerActor extends Actor with LongValueAggregator {
  def receive = {
    case IncOp(_, v) ⇒ _value = _value + v
  }
}

class GaugeAggregatorWorkerActor extends Actor with LongValueAggregator {
  def receive = {
    case IncOp(_, v) ⇒ _value = _value + v
    case SetOp(_, v) ⇒ _value = v
  }

}

class TimingAggregatorWorkerActor extends Actor with LongValueAggregator {

  def receive = {
    case SetOp(_, v) ⇒ _value = v
  }
}

class DistinctAggregatorWorkerActor extends Actor {
  private[this] var set = Set[Int]()
  def value = set.size: Long

  def receive = {
    case SetOp(_, v) ⇒ set = set + v
  }
}

