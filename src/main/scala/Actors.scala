import akka.actor.{Props, ActorLogging, Actor}
import akka.io.Udp
import akka.routing.RoundRobinRouter
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

//Messages
case class PossiblyMultipleMetricRawString(s: String)

case class SingleMetricRawString(s: String)

/**
 * Listen to UDP messages and fed them to the decoding actors
 */
class MetricServerActor extends Actor with ActorLogging {
  // Metric Handler
  val handler = context.actorOf(Props[MetricCoordinatorActor])

  def receive = {
    // transform the UDP payload to an UTF-8 String and send it to a decoder
    case Udp.Received(data, _) ⇒ handler ! PossiblyMultipleMetricRawString((data.decodeString("UTF-8")))
  }

}

/**
 * Coordination actors, that routes messages to the various Actors
 */
class MetricCoordinatorActor extends Actor with ActorLogging {

  val splitter = context.actorOf(Props[MessageSplitterActor].withRouter(RoundRobinRouter(5)))
  val decoder = context.actorOf(Props[MetricDecoderActor].withRouter(RoundRobinRouter(5)))
  val aggregator = context.actorOf(Props[MetricAggregatorActor].withRouter(RoundRobinRouter(5)))

  def receive = {
    case m: PossiblyMultipleMetricRawString ⇒ splitter ! m
    case m: SingleMetricRawString ⇒ decoder ! m
    case m: MetricOp with MetricStyle  ⇒ aggregator !m
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

/**
 * Dispatch all Metric operation to the corresponding aggregator (creates it if needed)
 */
class MetricAggregatorActor extends Actor {

  def receive = {
    case m: MetricOp with MetricStyle ⇒ {
      val name: String = metricActorName(m)
      val child = context.child(name).getOrElse(context.actorOf(Props(buildActor(m)), name))
      child ! m
    }
  }

  private def metricActorName(m: MetricOp with MetricStyle) = m.styleTag + "#" + m.bucket.name

  /**
   * Build the AggregatorWorkerActor corresponding to the metric style
   * @param s the metric style
   * @return the corresponding actor
   */
  private def buildActor(s: MetricStyle): Actor = s match {
    case _: CounterStyle ⇒ new CounterAggregatorWorkerActor
    case _: TimingStyle ⇒ new TimingAggregatorWorkerActor
    case _: GaugeStyle ⇒ new GaugeAggregatorWorkerActor
    case _: DistinctStyle ⇒ new DistinctAggregatorWorkerActor
  }
}

/**
 * This trait maintains a long variable and defines partial Receive function to act on that value
 */
trait LongValueAggregator {
  self: MetricStore[Long] with Actor =>
  protected[this] var _value: Long = 0

  def value = _value

  private[this] def reset = _value = 0

  def incrementIt: Actor.Receive = {
    case IncOp(_, v) ⇒ _value = _value + v
  }

  def setIt: Actor.Receive = {
    case SetOp(_, v) ⇒ _value = v
  }

  def storeAndResetIt: Actor.Receive = {
    case FlushOp(_, t) => {
      store(t, _value);
      reset
    }
  }

  def storeIt: Actor.Receive = {
    case FlushOp(_, t) => store(t, _value)
  }
}


/**
 * Stores a metric along with it's timestamp
 * @tparam T the type of the metric
 */
trait MetricStore[T] {
  private[this] var _store = Vector[(Long, T)]()

  def store(time: Long, value: T) = {
    _store = (time, value) +: _store
  }
}


class CounterAggregatorWorkerActor extends Actor with LongValueAggregator with MetricStore[Long] {
  def receive = incrementIt orElse storeAndResetIt
}

class GaugeAggregatorWorkerActor extends Actor with LongValueAggregator with MetricStore[Long] {
  def receive = incrementIt orElse setIt orElse storeIt

}

class TimingAggregatorWorkerActor extends Actor with LongValueAggregator with MetricStore[Long] {
  def receive = setIt orElse storeAndResetIt
}

class DistinctAggregatorWorkerActor extends Actor with MetricStore[Long] {
  private[this] var set = Set[Int]()

  def value = set.size: Long

  def receive = {
    case SetOp(_, v) ⇒ set = set + v
    case FlushOp(_, t) => {
      store(t, value);
      set = Set()
    }

  }
}

