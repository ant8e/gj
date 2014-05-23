
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

package gj

import gj.actor._
import ValuesProvider.{UnSubscribe, Subscribe}
import akka.actor.ActorSystem
import akka.util.Timeout
import org.scalatest.FunSpec
import akka.testkit.{TestProbe, ImplicitSender, TestKit, TestActorRef}
import akka.pattern.ask
import org.scalatest.matchers.MustMatchers
import scala.concurrent.duration._
import language.postfixOps

import MetricRepository._
import RawMetricHandler._
import metric._
import scala.util.Success
import ui.ValueStreamBridge
import ui.ServerSideEventsDirectives.{Message, RegisterClosedHandler}
import ui.ValueStreamBridge.RegisterStopHandler

class ActorsSpec(_system: ActorSystem) extends TestKit(_system) with FunSpec with ImplicitSender with MustMatchers {
  def this() = this(ActorSystem("ActorsSpec"))

  implicit val tout = Timeout(1 second)
  describe("Message splitter actor") {
    it("should split a multiple metric  string and emit several SingleMetricRawString") {
      val ref = TestActorRef(new RawMetricSplitter(self))
      ref ! MetricRawString("aaa\nbbb", 0)
      expectMsgAllConformingOf(1 second, classOf[SingleMetricRawString], classOf[SingleMetricRawString]) must be(Seq(SingleMetricRawString("aaa", 0), SingleMetricRawString("bbb", 0)))

    }

    it("should handle a single metric  string and emit one SingleMetricRawString") {
      val ref = TestActorRef(new RawMetricSplitter(self))
      ref ! MetricRawString("aaa", 0)
      expectMsg(SingleMetricRawString("aaa", 0))
    }

  }

  val b = SimpleBucket("test.bucket")
  val counter: LongCounter = LongCounter(b)
  val timing: LongTiming = LongTiming(b)
  val gauge: LongGauge = LongGauge(b)
  val distinct: LongDistinct = LongDistinct(b)

  describe("Raw metric Decoder actor") {
    val ref = TestActorRef(new MetricDecoder)

    it("should decode counter metrics") {
      ref ! SingleMetricRawString("test.bucket:1|c", 0)
      expectMsg(10 millis, Increment[LongCounter](counter, 1, 0))
    }

    it("should decode timing metrics") {
      ref ! SingleMetricRawString("test.bucket:100|ms", 0)
      expectMsg(10 millis, SetValue[LongTiming](timing, 100, 0))

    }

    it("should decode gauge setting metrics") {
      ref ! SingleMetricRawString("test.bucket:100|g", 0)
      expectMsg(10 millis, SetValue[LongGauge](gauge, 100, 0))

    }

    it("should decode Gauge updating metrics (dec)") {
      ref ! SingleMetricRawString("test.bucket:-100|g", 0)
      expectMsg(10 millis, Increment[LongGauge](gauge, -100, 0))
    }

    it("should decode Gauge updating metrics (inc)") {
      ref ! SingleMetricRawString("test.bucket:+100|g", 0)
      expectMsg(10 millis, Increment[LongGauge](gauge, 100, 0))
    }
    it("should decode Distinct setting metrics") {
      ref ! SingleMetricRawString("test.bucket:100|s", 0)
      expectMsg(10 millis, SetValue[LongDistinct](distinct, 100, 0))

    }

  }
  val flushOp = Flush[LongGauge](gauge, System.currentTimeMillis)

  describe("Gauge Style Worker actor ") {
    it("should set the gauge with a SetValue") {
      val ref = TestActorRef(new GaugeAggregatorWorkerActor(gauge))
      ref ! SetValue[LongGauge](gauge, 42, 0)
      ref.underlyingActor.value must be(42)
    }
    it("should increment the gauge with a Increment") {
      val ref = TestActorRef(new GaugeAggregatorWorkerActor(gauge))
      ref ! Increment[LongGauge](gauge, 1, 0)
      ref.underlyingActor.value must be(1)
    }

    it("should not reset the value on flush") {
      val ref = TestActorRef(new GaugeAggregatorWorkerActor(gauge))
      ref ! Increment[LongGauge](gauge, 1, 0)
      ref.underlyingActor.value must be(1)
      ref ! flushOp
      ref.underlyingActor.value must be(1)

    }
  }

  describe("Counter Style Worker actor ") {
    it("should NOT set the counter with a SetValue") {
      val ref = TestActorRef(new CounterAggregatorWorkerActor(counter))
      ref ! new SetValue[LongCounter](counter, 42, 0)
      ref.underlyingActor.value must be(0)
    }
    it("should increment the gauge with a Increment") {
      val ref = TestActorRef(new CounterAggregatorWorkerActor(counter))
      ref ! new Increment[LongCounter](counter, 42, 0)
      ref.underlyingActor.value must be(42)
    }
    it("should reset the value on flush") {
      val ref = TestActorRef(new CounterAggregatorWorkerActor(counter))
      ref ! new Increment[LongCounter](counter, 1, 0)
      ref.underlyingActor.value must be(1)
      ref ! flushOp
      ref.underlyingActor.value must be(0)

    }

  }

  describe("Timing Style Worker actor ") {
    it("should set the timing  with a SetValue") {
      val ref = TestActorRef(new TimingAggregatorWorkerActor(timing))
      ref ! new SetValue[LongTiming](timing, 42, 0)
      ref.underlyingActor.value must be(42)
    }
    it("should NOT increment the timing with a Increment") {
      val ref = TestActorRef(new TimingAggregatorWorkerActor(timing))
      ref ! new Increment[LongTiming](timing, 42, 0)
      ref.underlyingActor.value must be(0)
    }
    it("should reset the value on flush") {
      val ref = TestActorRef(new TimingAggregatorWorkerActor(timing))
      ref ! new SetValue[LongTiming](timing, 1, 0)
      ref.underlyingActor.value must be(1)
      ref ! flushOp
      ref.underlyingActor.value must be(0)

    }
  }

  describe("Distinct Style Worker actor ") {
    it("should set the distinct  with a SetValue") {
      val ref = TestActorRef(new DistinctAggregatorWorkerActor(distinct))
      ref ! SetValue[LongDistinct](distinct, 42, 0)
      ref.underlyingActor.value must be(1)
      ref ! SetValue[LongDistinct](distinct, 42, 0)
      ref.underlyingActor.value must be(1)
      ref ! new SetValue[LongDistinct](distinct, 24, 0)
      ref.underlyingActor.value must be(2)

    }
    it("should NOT increment the distinct with a Increment") {
      val ref = TestActorRef(new DistinctAggregatorWorkerActor(distinct))
      ref ! new Increment[LongDistinct](distinct, 42, 0)
      ref.underlyingActor.value must be(0)
    }
    it("should reset the value on flush") {
      val ref = TestActorRef(new DistinctAggregatorWorkerActor(distinct))
      ref ! new SetValue[distinct.type](distinct, 1, 0)
      ref.underlyingActor.value must be(1)
      ref ! flushOp
      ref.underlyingActor.value must be(0)

    }

  }

  describe("Metric repository Actor") {
    it("should start with one size list of active buckets") {
      val ref = TestActorRef(new MetricRepository)
      val future = ref ? BucketListQuery
      val Success(v: BucketListResponse) = future.value.get
      v.buckets must have size (1)
    }

    it("should maintain a list of active buckets") {
      val ref = TestActorRef(new MetricRepository)
      ref ! SetValue[LongGauge](gauge, 1, 0)
      val future = ref ? BucketListQuery
      val Success(v: BucketListResponse) = future.value.get
      v.buckets must have size (2)
      v.buckets must contain("G." + b.name)
    }
    it("should maintain a list of active metric") {
      val ref = TestActorRef(new MetricRepository)
      ref ! SetValue[LongGauge](gauge, 1, 0)
      val future = ref ? MetricListQuery
      val Success(v: MetricListResponse) = future.value.get
      v.metrics must have size (2)
      v.metrics must contain(gauge.asInstanceOf[MetricId])
    }

    it("should handle a start publish event") {
      val ref = TestActorRef(new MetricRepository)
      ref ! SetValue[LongGauge](gauge, 1, 0)
      ref ! StartPublish(gauge)
      ref ! FlushAll
      expectMsgPF(100.millisecond) {
        case MetricValueAt(`gauge`, _, 1) ⇒ ()
      }

    }

    it("should handle a stop publish event") {
      val ref = TestActorRef(new MetricRepository)
      ref ! SetValue[LongGauge](gauge, 1, 0)
      ref ! StartPublish(gauge)
      ref ! FlushAll
      expectMsgPF(100.millisecond) {
        case MetricValueAt(`gauge`, _, 1) ⇒ ()
      }
      ref ! StopPublish(gauge)
      ref ! FlushAll
      expectNoMsg(100.millisecond)
    }

  }

  describe("Metric Values provider Actor") {
    it("should start publishing on Subsribe") {

      val agreg = TestActorRef(new MetricRepository)
      val ref = TestActorRef(ValuesProvider.props(agreg))
      agreg ! SetValue[LongGauge](gauge, 1, 0)
      ref ! Subscribe(gauge)
      agreg ! FlushAll
      expectMsgPF(100.millisecond) {
        case MetricValueAt(`gauge`, _, 1) ⇒ ()
      }
    }
    it("should stop publishing on UnSubsribe") {

      val agreg = TestActorRef(new MetricRepository)
      val ref = TestActorRef(ValuesProvider.props(agreg))
      agreg ! SetValue[LongGauge](gauge, 1, 0)
      ref ! Subscribe(gauge)
      agreg ! FlushAll
      expectMsgPF(100.millisecond) {
        case MetricValueAt(`gauge`, _, 1) ⇒ ()
      }
      ref ! UnSubscribe(gauge)
      agreg ! FlushAll
      expectNoMsg(100.millisecond)
    }
  }

  describe("Value Stream Bridge") {
    it("should register a close handler") {
      TestActorRef(new ValueStreamBridge(self, counter))
      expectMsgClass(classOf[RegisterClosedHandler])
    }
    it("should format a value") {
      val vba = TestActorRef(new ValueStreamBridge(self, counter))
      expectMsgClass(classOf[RegisterClosedHandler])
      vba ! MetricValueAt[counter.type](counter, 0, 1)
      expectMsg(Message( """{"metric":"test.bucket","value":1,"ts":0}"""))
    }
    it("should stop correctly") {
      val vba = TestActorRef(new ValueStreamBridge(self, counter))
      val probe = TestProbe()
      probe watch vba
      val rch = expectMsgClass(classOf[RegisterClosedHandler])
      vba ! RegisterStopHandler(() => {
        self ! "Yeah"
      })
      rch.handler()
      expectMsg("Yeah")
      probe.expectTerminated(vba)
    }
  }
}
