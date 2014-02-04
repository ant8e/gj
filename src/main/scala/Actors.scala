package gj

import Messages._
import Messages.BucketListQuery
import Messages.BucketListResponse
import Messages.MetricRawString
import Messages.SingleMetricRawString
import MetricOperation.{SetValue, Increment, Flush}
import MetricStyle.{Distinct, Gauge, Timing, Counter}
import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import akka.io.Udp
import akka.routing.RoundRobinRouter
import scala.util.{Failure, Success, Try}
import scala.language.postfixOps

/**
 * Messages
 */
object Messages {

  type MetricMessage = MetricOperation with MetricStyle

  case class MetricRawString(s: String)

  case class SingleMetricRawString(s: String)

  object BucketListQuery

  case class BucketListResponse(buckets: Iterable[String])

  object Tick

}

/**
 * Listen to UDP messages and fed them to the decoding actors
 */
class MetricUdpListener extends Actor with ActorLogging {
  // Metric Handler
  val handler = context.actorOf(MetricCoordinatorActor.props)

  def receive = {
    // transform the UDP payload to an UTF-8 String and send it to a decoder
    case Udp.Received(data, send) ⇒ log.debug("received {} from {}", data.utf8String, send.getAddress.toString); handler ! MetricRawString((data.utf8String))
  }

}

/**
 * Coordination actors, that routes messages to the various Actors
 */
object MetricCoordinatorActor {
  def props = Props[MetricCoordinatorActor]
}

class MetricCoordinatorActor extends Actor with ActorLogging {

  import context._
  import scala.concurrent.duration._

  val decoder = actorOf(MetricDecoderActor.props)
  val splitter = actorOf(MessageSplitterActor.props(decoder))
  val aggregator = actorOf(MetricAggregatorActor.props)

  val tick = system.scheduler.schedule(500.millis, 1.seconds, self, Messages.Tick)

  def receive = {
    case m: MetricRawString ⇒ splitter ! m
    case m: SingleMetricRawString ⇒ log.debug("received {}", m.s); decoder ! m
    case m: MetricMessage ⇒ aggregator ! m
    case Tick ⇒ aggregator ! MetricOperation.Flush
  }

  override def postStop() = tick.cancel()
}

/**
 * This Actor splits incoming message into their single form
 */
object MessageSplitterActor {
  def props(decoder: ActorRef): Props = Props(classOf[MessageSplitterActor], decoder).withRouter(RoundRobinRouter(5))
}

class MessageSplitterActor(decoder: ActorRef) extends Actor {

  def receive = {
    case MetricRawString(s) ⇒ s.lines.foreach {
      decoder ! SingleMetricRawString(_)
    }
  }
}

/**
 * This actor decodes a raw metric representation into a MetricOP with an associated MetricStyle
 */
object MetricDecoderActor {
  def props: Props = Props[MetricDecoderActor].withRouter(RoundRobinRouter(5))
}

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
  private def parse(rawString: String): Try[MetricMessage] = Try {
    //TODO support counter sampling param
    val ParsingRegExp(bucket, value, style, _) = rawString.trim
    val b = SimpleBucket(bucket)
    val v: Int = if (value(0) == '+')
      value.drop(1).toInt // Java 1.6 does not handle the + sign, later version do
    else value.toInt

    style match {
      case "c" ⇒ new Increment(b, v) with Counter
      case "ms" ⇒ new SetValue(b, v) with Timing
      case "g" ⇒ value match {
        case SignedDigit() ⇒ new Increment(b, v) with Gauge
        case _ ⇒ new SetValue(b, v) with Gauge
      }
      case "s" ⇒ new SetValue(b, v) with Distinct

    }
  }
}

/**
 * Dispatch all Metric operation to the corresponding aggregator (creates it if needed)
 */
object MetricAggregatorActor {
  def props: Props = Props[MetricAggregatorActor].withRouter(RoundRobinRouter(5))
}

class MetricAggregatorActor extends Actor {

  import context._

  private var buckets = Set[String]()

  def receive = {
    case m: MetricMessage ⇒ {
      val name: String = metricActorName(m)
      buckets = buckets + name
      val child = context.child(name).getOrElse(actorOf(buildActor(m), name))
      child ! m
    }
    case BucketListQuery ⇒ sender ! BucketListResponse(buckets)
  }


  private def metricActorName(m: MetricMessage) = s"${m.styleTag}.${m.bucket.name}"

  /**
   * Build the AggregatorWorkerActor corresponding to the metric style
   * @param s the metric style
   * @return the corresponding actor
   */
  private def buildActor(s: MetricStyle): Props = s match {
    case _: Counter ⇒ Props[CounterAggregatorWorkerActor]
    case _: Timing ⇒ Props[TimingAggregatorWorkerActor]
    case _: Gauge ⇒ Props[GaugeAggregatorWorkerActor]
    case _: Distinct ⇒ Props[DistinctAggregatorWorkerActor]
  }
}

/**
 * This trait maintains a long variable and defines a partial Receive function to act on that value
 */
trait LongValueAggregator {
  self: MetricStore[Long] with Actor ⇒
  protected[this] var _value: Long = 0

  def value = _value

  private[this] def reset = _value = 0

  def incrementIt: Receive = {
    case Increment(_, v) ⇒ _value = _value + v
  }

  def setIt: Receive = {
    case SetValue(_, v) ⇒ _value = v
  }

  def storeAndResetIt: Receive = {
    case Flush(_, t) ⇒ {
      store(t, _value);
      reset
    }
  }

  def storeIt: Receive = {
    case Flush(_, t) ⇒ store(t, _value)
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
    case SetValue(_, v) ⇒ set = set + v
    case Flush(_, t) ⇒ {
      store(t, value);
      set = Set()
    }

  }
}

