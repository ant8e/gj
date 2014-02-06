
package gj

import Messages._
import akka.actor.ActorSystem
import akka.util.Timeout
import org.scalatest.FunSpec
import akka.testkit.{ImplicitSender, TestKit, TestActorRef}
import akka.pattern.ask
import org.scalatest.matchers.MustMatchers
import scala.concurrent.duration._
import scala.util.Success
import language.postfixOps

class ActorsSpec(_system: ActorSystem) extends TestKit(_system) with FunSpec with ImplicitSender with MustMatchers {
  def this() = this(ActorSystem("ActorsSpec"))

  implicit val tout = Timeout(1 second)
  describe("Message splitter actor") {
    it("should split a multiple metric  string and emit several SingleMetricRawString") {
      val ref = TestActorRef(new MessageSplitterActor(self))
      ref ! MetricRawString("aaa\nbbb")
      expectMsgAllConformingOf(1 second, classOf[SingleMetricRawString], classOf[SingleMetricRawString]) must be(Seq(SingleMetricRawString("aaa"), SingleMetricRawString("bbb")))

    }

    it("should handle a single metric  string and emit one SingleMetricRawString") {
      val ref = TestActorRef(new MessageSplitterActor(self))
      ref ! MetricRawString("aaa")
      expectMsg(SingleMetricRawString("aaa"))
    }

  }

  val b = SimpleBucket("test.bucket")
  val counter: LongCounter = LongCounter(b)
  val timing: LongTiming = LongTiming(b)
  val gauge: LongGauge = LongGauge(b)
  val distinct: LongDistinct = LongDistinct(b)

  describe("Raw metric Decoder actor") {
    val ref = TestActorRef(new MetricDecoderActor)

    it("should decode counter metrics") {
      ref ! SingleMetricRawString("test.bucket:1|c")
      expectMsg(10 millis, Increment[LongCounter](counter, 1L))
    }

    it("should decode timing metrics") {
      ref ! SingleMetricRawString("test.bucket:100|ms")
      expectMsg(10 millis, SetValue[LongTiming](timing, 100L))

    }

    it("should decode gauge setting metrics") {
      ref ! SingleMetricRawString("test.bucket:100|g")
      expectMsg(10 millis, SetValue[LongGauge](gauge, 100L))

    }

    it("should decode Gauge updating metrics (dec)") {
      ref ! SingleMetricRawString("test.bucket:-100|g")
      expectMsg(10 millis, new Increment[LongGauge](gauge, -100))
    }

    it("should decode Gauge updating metrics (inc)") {
      ref ! SingleMetricRawString("test.bucket:+100|g")
      expectMsg(10 millis, Increment[LongGauge](gauge, 100))
    }
    it("should decode Distinct setting metrics") {
      ref ! SingleMetricRawString("test.bucket:100|s")
      expectMsg(10 millis, SetValue[LongDistinct](distinct, 100))

    }

  }
  val flushOp = Flush[LongGauge](gauge, System.currentTimeMillis)

  describe("Gauge Style Worker actor ") {
    it("should set the gauge with a SetValue") {
      val ref = TestActorRef(new GaugeAggregatorWorkerActor(gauge))
      ref ! SetValue[LongGauge](gauge, 42L)
      ref.underlyingActor.value must be(42L)
    }
    it("should increment the gauge with a Increment") {
      val ref = TestActorRef(new GaugeAggregatorWorkerActor(gauge))
      ref ! new Increment[LongGauge](gauge, 1L)
      ref.underlyingActor.value must be(1L)
    }

    it("should not reset the value on flush") {
      val ref = TestActorRef(new GaugeAggregatorWorkerActor(gauge))
      ref ! new Increment[LongGauge](gauge, 1L)
      ref.underlyingActor.value must be(1L)
      ref ! flushOp
      ref.underlyingActor.value must be(1L)

    }
  }

  describe("Counter Style Worker actor ") {
    it("should NOT set the counter with a SetValue") {
      val ref = TestActorRef(new CounterAggregatorWorkerActor(counter))
      ref ! new SetValue[LongCounter](counter, 42)
      ref.underlyingActor.value must be(0)
    }
    it("should increment the gauge with a Increment") {
      val ref = TestActorRef(new CounterAggregatorWorkerActor(counter))
      ref ! new Increment[LongCounter](counter, 42L)
      ref.underlyingActor.value must be(42L)
    }
    it("should reset the value on flush") {
      val ref = TestActorRef(new CounterAggregatorWorkerActor(counter))
      ref ! new Increment[LongCounter](counter, 1L)
      ref.underlyingActor.value must be(1L)
      ref ! flushOp
      ref.underlyingActor.value must be(0L)

    }

  }

  describe("Timing Style Worker actor ") {
    it("should set the timing  with a SetValue") {
      val ref = TestActorRef(new TimingAggregatorWorkerActor(timing))
      ref ! new SetValue[LongTiming](timing, 42L)
      ref.underlyingActor.value must be(42L)
    }
    it("should NOT increment the timing with a Increment") {
      val ref = TestActorRef(new TimingAggregatorWorkerActor(timing))
      ref ! new Increment[LongTiming](timing, 42L)
      ref.underlyingActor.value must be(0)
    }
    it("should reset the value on flush") {
      val ref = TestActorRef(new TimingAggregatorWorkerActor(timing))
      ref ! new SetValue[LongTiming](timing, 1L)
      ref.underlyingActor.value must be(1L)
      ref ! flushOp
      ref.underlyingActor.value must be(0L)

    }
  }

  describe("Distinct Style Worker actor ") {
    it("should set the distinct  with a SetValue") {
      val ref = TestActorRef(new DistinctAggregatorWorkerActor(distinct))
      ref ! SetValue[LongDistinct](distinct, 42)
      ref.underlyingActor.value must be(1)
      ref ! SetValue[LongDistinct](distinct, 42)
      ref.underlyingActor.value must be(1)
      ref ! new SetValue[LongDistinct](distinct, 24)
      ref.underlyingActor.value must be(2)

    }
    it("should NOT increment the distinct with a Increment") {
      val ref = TestActorRef(new DistinctAggregatorWorkerActor(distinct))
      ref ! new Increment[LongDistinct](distinct, 42)
      ref.underlyingActor.value must be(0)
    }
    it("should reset the value on flush") {
      val ref = TestActorRef(new DistinctAggregatorWorkerActor(distinct))
      ref ! new SetValue[distinct.type](distinct, 1)
      ref.underlyingActor.value must be(1)
      ref ! flushOp
      ref.underlyingActor.value must be(0)

    }

  }

  describe("Metric Aggregator Actor") {
    it("should start with an empty list of active buckets") {
      val ref = TestActorRef(new MetricAggregatorActor)
      val future = ref ? Messages.BucketListQuery
      val Success(v: Messages.BucketListResponse) = future.value.get
      v.buckets must have size (0)
    }

    it("should maintain a list of active buckets") {
      val ref = TestActorRef(new MetricAggregatorActor)
      ref ! SetValue[LongGauge](gauge, 1)
      val future = ref ? Messages.BucketListQuery
      val Success(v: Messages.BucketListResponse) = future.value.get
      v.buckets must have size (1)
      v.buckets must contain("G." + b.name)
    }

    it("should handle a start publish event") {
      val ref = TestActorRef(new MetricAggregatorActor)
      ref ! SetValue[LongGauge](gauge, 1)
      ref ! StartPublish(gauge)
      ref ! FlushAll
      expectMsgPF(100.millisecond){case  MetricValueAt(`gauge`,_,1) => ()}

    }

    it("should handle a stop publish event") {
      val ref = TestActorRef(new MetricAggregatorActor)
      ref ! SetValue[LongGauge](gauge, 1)
      ref ! StartPublish(gauge)
      ref ! FlushAll
      expectMsgPF(100.millisecond){case  MetricValueAt(`gauge`,_,1) => ()}
      ref ! StopPublish(gauge)
      ref ! FlushAll
      expectNoMsg (100.millisecond)
    }

  }

}
