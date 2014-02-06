package gj

import Messages._
import Messages.BucketListQuery
import Messages.BucketListResponse
import Messages.MetricRawString
import Messages.SingleMetricRawString
import akka.actor._
import akka.io.Udp
import akka.routing.RoundRobinRouter
import scala.util.Try
import scala.language.postfixOps
import akka.event.{LookupClassification, ActorEventBus}
import scala.util.Failure
import scala.util.Success

/**
 * Messages
 */
object Messages {


  case class MetricRawString(s: String)

  case class SingleMetricRawString(s: String)

  object BucketListQuery

  case class BucketListResponse(buckets: Iterable[String])

  object Tick

  object FlushAll

  case class StartPublish(metric: Metric)

  case class StopPublish(metric: Metric)

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
    case m: MetricOperation[_] ⇒ aggregator ! m
    case Tick ⇒ aggregator ! FlushAll
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
  private def parse(rawString: String): Try[MetricOperation[_]] = Try {
    //TODO support counter sampling param
    val ParsingRegExp(bucket, value, style, _) = rawString.trim
    val b = SimpleBucket(bucket)
    val v: Long = if (value(0) == '+')
      value.drop(1).toLong // Java 1.6 does not handle the + sign, later version do
    else value.toLong

    style match {
      case "c" ⇒ Increment[LongCounter](LongCounter(b), v)
      case "ms" ⇒ SetValue[LongTiming](LongTiming(b), v)
      case "g" ⇒ value match {
        case SignedDigit() ⇒ Increment[LongGauge](LongGauge(b), v)
        case _ ⇒ new SetValue[LongGauge](LongGauge(b), v)
      }
      case "s" ⇒ new SetValue[LongDistinct](LongDistinct(b), v) with Distinct

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

  private var metricActors = Map[Metric, ActorRef]()

  def receive = {
    case m: MetricOperation[_] ⇒ {
      val child = {
        metricActors.get(m.metric) match {
          case Some(c) => c
          case None => {
            val ret =actorOf(buildActor(m.metric), metricActorName(m.metric))
            metricActors = metricActors + (m.metric ->ret)
            ret
          }
        }
      }

      child ! m
    }
    case BucketListQuery ⇒ sender ! BucketListResponse(metricActors.keys.map(metricActorName(_)))
    case sp@StartPublish(m) => metricActors.get(m).foreach(_ forward sp)
    case sp@StopPublish(m) => metricActors.get(m).foreach(_ forward sp)
    case FlushAll => metricActors.foreach( p=> p._2 ! Flush(p._1,0) )
  }


  private def metricActorName(m: Metric) = s"${m.styleTag}.${m.bucket.name}"

  /**
   * Build the AggregatorWorkerActor corresponding to the metric style
   * @param s the metric style
   * @return the corresponding actor
   */
  private def buildActor(s: MetricStyle): Props = s match {
    case _: Counter ⇒ Props(classOf[CounterAggregatorWorkerActor], s)
    case _: Timing ⇒ Props(classOf[TimingAggregatorWorkerActor], s)
    case _: Gauge ⇒ Props(classOf[GaugeAggregatorWorkerActor], s)
    case _: Distinct ⇒ Props(classOf[DistinctAggregatorWorkerActor], s)
  }
}

/**
 * This trait maintains a long variable and defines a partial Receive function to act on that value
 */
trait LongValueAggregator[T <: Metric] {
  self: MetricStore[T#Value] with Actor ⇒
  def metric: T

  def resetValue: T#Value

  def plus(v1: T#Value, v2: T#Value): T#Value

  protected[this] var _value: T#Value = resetValue

  def value = _value

  private[this] def reset = _value = resetValue

  def incrementIt: Receive = {
    case Increment(_, v: T#Value) ⇒ _value = plus(_value, v)
  }

  def setIt: Receive = {
    case SetValue(_, v: T#Value) ⇒ _value = v
  }

  def storeAndResetIt: Receive = {
    case Flush(_, t) ⇒
      store(t, _value)
      publish
      reset

  }


  def storeIt: Receive = {
    case Flush(_, t) ⇒
      store(t, _value)
      publish

  }

  var pub: Option[ActorRef] = None

  def publishIt: Receive = {
    case m: StartPublish => pub = Some(sender)
    case m: StopPublish => pub = None
  }

  def publish = pub.foreach(_ ! MetricValueAt[T](metric, 0, _value))
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

class CounterAggregatorWorkerActor(val metric: LongCounter) extends Actor with LongValueAggregator[LongCounter] with MetricStore[LongCounter#Value] {
  def receive = incrementIt orElse storeAndResetIt orElse publishIt

  def resetValue: LongCounter#Value = 0

  def plus(v1: LongCounter#Value, v2: LongCounter#Value): LongCounter#Value = v1 + v2
}

class GaugeAggregatorWorkerActor(val metric: LongGauge) extends Actor with LongValueAggregator[LongGauge] with MetricStore[LongGauge#Value] {
  def receive = incrementIt orElse setIt orElse storeIt orElse publishIt

  def resetValue: LongGauge#Value = 0

  def plus(v1: LongGauge#Value, v2: LongGauge#Value): LongGauge#Value = v1 + v2
}

class TimingAggregatorWorkerActor(val metric: LongTiming) extends Actor with LongValueAggregator[LongTiming] with MetricStore[LongTiming#Value] {
  def receive = setIt orElse storeAndResetIt orElse publishIt

  def resetValue: LongTiming#Value = 0

  def plus(v1: LongTiming#Value, v2: LongTiming#Value): LongTiming#Value = v1 + v2
}

class DistinctAggregatorWorkerActor(val metric: LongDistinct) extends Actor with MetricStore[LongDistinct#Value] {
  private[this] var set = Set[Any]()

  def value = set.size

  def receive = {
    case SetValue(_, v) ⇒ set = set + v
    case Flush(_, t) ⇒ {
      store(t, value);
      set = Set()
    }

  }
}

case class toto(s: String)

class MyEventBus extends ActorEventBus with LookupClassification {
  type Event = toto
  type Classifier = String

  protected def classify(event: Event): Classifier = event.s


  protected def mapSize(): Int = 36

  protected def publish(event: Event, subscriber: Subscriber): Unit = subscriber ! event
}
