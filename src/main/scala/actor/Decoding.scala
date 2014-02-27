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

package gj.actor

import akka.actor._
import akka.io.Udp
import akka.routing.RoundRobinRouter
import scala.util.Try
import scala.language.postfixOps
import gj.metric._
import scala.util.Failure
import scala.util.Success

/**
 * Actor that handles raw metric message, convert them,
 * and feed them to the repository
 */

class RawMetricHandler(repo: ActorRef) extends Actor with ActorLogging {

  import context._
  import scala.concurrent.duration._
  import RawMetricHandler._

  val decoder = actorOf(MetricDecoder.props)
  val splitter = actorOf(RawMetricSplitter.props(decoder))
  val valueProvider = actorOf(ValuesProvider.props(repo))

  val tick = system.scheduler.schedule(500.millis, 1.seconds, self, Tick)

  def receive = {
    case m: MetricRawString ⇒ splitter ! m
    case m: SingleMetricRawString ⇒ decoder ! m
    case m: MetricOperation[_] ⇒ repo ! m
    case Tick ⇒ repo ! MetricRepository.FlushAll
  }

  override def postStop() = tick.cancel()
}

object RawMetricHandler {

  case class MetricRawString(s: String, ts: Long = System.currentTimeMillis())

  case class SingleMetricRawString(s: String, ts: Long)

  object Tick

  def props(repo: ActorRef) = Props(new RawMetricHandler(repo))
}

/**
 * This Actor splits incoming message into their single form
 */
object RawMetricSplitter {
  def props(decoder: ActorRef): Props = Props(classOf[RawMetricSplitter], decoder).withRouter(RoundRobinRouter(5))
}

class RawMetricSplitter(decoder: ActorRef) extends Actor {

  import RawMetricHandler._

  def receive = {
    case MetricRawString(s, ts) ⇒ s.lines.foreach {
      decoder forward SingleMetricRawString(_, ts)
    }
  }
}

/**
 * This actor decodes a raw metric representation into a MetricOP with an associated MetricStyle
 */
object MetricDecoder {
  def props: Props = Props[MetricDecoder].withRouter(RoundRobinRouter(5))
}

class MetricDecoder extends Actor with ActorLogging {

  import RawMetricHandler.SingleMetricRawString

  def receive = {
    case SingleMetricRawString(m, ts) ⇒ parse(m, ts) match {
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
  private def parse(rawString: String, ts: Long): Try[MetricOperation[_]] = Try {
    //TODO support counter sampling param
    val ParsingRegExp(bucket, value, style, _) = rawString.trim
    val b = SimpleBucket(bucket)
    val v: Long = if (value(0) == '+')
      value.drop(1).toLong // Java 1.6 does not handle the + sign, later version do
    else value.toLong

    style match {
      case "c" ⇒ Increment[LongCounter](LongCounter(b), v, ts)
      case "ms" ⇒ SetValue[LongTiming](LongTiming(b), v, ts)
      case "g" ⇒ value match {
        case SignedDigit() ⇒ Increment[LongGauge](LongGauge(b), v, ts)
        case _ ⇒ new SetValue[LongGauge](LongGauge(b), v, ts)
      }
      case "s" ⇒ new SetValue[LongDistinct](LongDistinct(b), v, ts) with Distinct

    }
  }
}

