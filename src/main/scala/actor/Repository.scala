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

import akka.actor.{ ActorLogging, ActorRef, Actor, Props }
import gj.metric._
import scala.Some
import storage.{ MemoryMetricStore, MetricStore }

/**
 * Handle the metric operation, store and aggregate all the metrics
 */
class MetricRepository extends Actor with ActorLogging {

  import context._
  import MetricRepository._

  private var metricActors = Map[Metric, ActorRef]()

  val messageCountMetric = LongCounter(SimpleBucket("gj.messages"))

  val messageCountActor = actorFor(messageCountMetric)

  def receive = {
    case m: MetricOperation[_] ⇒ {
      actorFor(m.metric) ! m
      messageCountActor ! Increment[messageCountMetric.type](messageCountMetric, 1, m.ts)
    }
    case BucketListQuery ⇒ sender ! BucketListResponse(metricActors.keys.toSeq map (metricActorName))
    case MetricListQuery ⇒ sender ! MetricListResponse(metricActors.keys.toSeq)
    case sp @ StartPublish(m) ⇒ metricActors.get(m).foreach(_ forward sp)
    case sp @ StopPublish(m) ⇒ metricActors.get(m).foreach(_ forward sp)
    case FlushAll ⇒ metricActors.foreach(p ⇒ p._2 ! Flush(p._1, 0))

  }

  def actorFor(m: Metric): ActorRef = metricActors.get(m) match {
    case Some(c) ⇒ c
    case None ⇒ {
      val ret = actorOf(buildMetricActor(m), metricActorName(m))
      metricActors = metricActors + (m -> ret)
      ret
    }
  }

  private def metricActorName(m: Metric) = s"${m.styleTag}.${m.bucket.name}"

  /**
   * Build the AggregatorWorkerActor corresponding to the metric style
   * @param s the metric style
   * @return the corresponding actor
   */
  private def buildMetricActor(s: MetricStyle): Props = s match {
    case _: Counter ⇒ Props(classOf[CounterAggregatorWorkerActor], s)
    case _: Timing ⇒ Props(classOf[TimingAggregatorWorkerActor], s)
    case _: Gauge ⇒ Props(classOf[GaugeAggregatorWorkerActor], s)
    case _: Distinct ⇒ Props(classOf[DistinctAggregatorWorkerActor], s)
  }
}

object MetricRepository {

  object BucketListQuery

  case class BucketListResponse(buckets: Seq[String])

  object MetricListQuery

  case class MetricListResponse(metrics: Seq[Metric])

  object FlushAll

  case class StartPublish(metric: Metric)

  case class StopPublish(metric: Metric)

  def props: Props = Props[MetricRepository]
}

/**
 * This trait maintains a long variable and defines a partial Receive function to act on that value
 */
trait ValueAggregator[T <: Metric] {
  self: MemoryMetricStore[T] with Actor ⇒

  import MetricRepository._

  def metric: T

  def resetValue: T#Value

  def plus(v1: T#Value, v2: T#Value): T#Value

  protected[this] var _value: T#Value = resetValue

  def value = _value

  private[this] def reset = _value = resetValue

  def incrementIt: Receive = {
    case Increment(_, v: T#Value, _) ⇒ _value = plus(_value, v)
  }

  def setIt: Receive = {
    case SetValue(_, v: T#Value, _) ⇒ _value = v
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
    case m: StartPublish ⇒ pub = Some(sender)
    case m: StopPublish ⇒ pub = None
  }

  def publish = pub.foreach(_ ! MetricValueAt[T](metric, 0, _value))
}

class CounterAggregatorWorkerActor(val metric: LongCounter) extends Actor with ValueAggregator[LongCounter] with MemoryMetricStore[LongCounter] {
  def receive = incrementIt orElse storeAndResetIt orElse publishIt

  def resetValue: LongCounter#Value = 0

  def plus(v1: LongCounter#Value, v2: LongCounter#Value): LongCounter#Value = v1 + v2
}

class GaugeAggregatorWorkerActor(val metric: LongGauge) extends Actor with ValueAggregator[LongGauge] with MemoryMetricStore[LongGauge] {
  def receive = incrementIt orElse setIt orElse storeIt orElse publishIt

  def resetValue: LongGauge#Value = 0

  def plus(v1: LongGauge#Value, v2: LongGauge#Value): LongGauge#Value = v1 + v2
}

class TimingAggregatorWorkerActor(val metric: LongTiming) extends Actor with ValueAggregator[LongTiming] with MemoryMetricStore[LongTiming] {
  def receive = setIt orElse storeAndResetIt orElse publishIt

  def resetValue: LongTiming#Value = 0

  def plus(v1: LongTiming#Value, v2: LongTiming#Value): LongTiming#Value = v1 + v2
}

class DistinctAggregatorWorkerActor(val metric: LongDistinct) extends Actor with MemoryMetricStore[LongDistinct] {
  private[this] var set = Set[Any]()

  def value = set.size

  def receive = {
    case SetValue(_, v, _) ⇒ set = set + v
    case Flush(_, t) ⇒ {
      store(t, value);
      set = Set()
    }

  }
}

