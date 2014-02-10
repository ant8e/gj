package gj.actor

import akka.event.{ LookupClassification, ActorEventBus }
import ValuesProvider.{ UnSubscribe, Subscribe }
import akka.actor.{ Props, ActorRef, Actor }
import gj.metric.{MetricValueAt, Metric}

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
  type Classifier = Metric

  protected def classify(event: Event): Classifier = event.metric

  protected def mapSize(): Int = 36

  protected def publish(event: Event, subscriber: Subscriber): Unit = subscriber ! event

  def subscriberCount(c: Classifier) = subscribers.valueIterator(c).size
}
