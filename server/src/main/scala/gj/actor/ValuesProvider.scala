/*
 *  Copyright © 2015 Antoine Comte
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package gj.actor

import akka.actor.{ Actor, ActorRef, Props }
import akka.event.{ ActorEventBus, LookupClassification }
import gj.actor.ValuesProvider.{ Subscribe, UnSubscribe }
import gj.metric.{ MetricId, MetricValueAt, _ }

/**
 * Actor that provides the values stream
 */
object ValuesProvider {

  case class Subscribe(m: Metric)

  case class UnSubscribe(m: Metric)

  def props(metricActor: ActorRef) = Props(new ValuesProvider(metricActor))
}

class ValuesProvider(val metricActor: ActorRef) extends Actor with ValuesEventBus {
  MetricRepository
  override def receive: Actor.Receive = {
    case Subscribe(m) ⇒
      metricActor ! MetricRepository.StartPublish(m)
      subscribe(sender, m)
    case UnSubscribe(m) ⇒
      unsubscribe(sender, m)
      if (subscriberCount(m) == 0) metricActor ! MetricRepository.StopPublish(m)
    case x: MetricValueAt[_] ⇒ publish(x)
  }
}

trait ValuesEventBus extends ActorEventBus with LookupClassification {
  type Event = MetricValueAt[_ <: Metric]
  type Classifier = MetricId

  protected def classify(event: Event): Classifier = event.metric

  protected def mapSize(): Int = 36

  protected def publish(event: Event, subscriber: Subscriber): Unit = subscriber ! event

  def subscriberCount(c: Classifier) = subscribers.valueIterator(c).size
}
