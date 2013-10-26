

import Messages._
import MetricOperation.{ Flush, SetValue, Increment }
import MetricStyle.{ Distinct, Gauge, Timing, Counter }
import akka.actor.ActorSystem
import akka.util.Timeout
import org.scalatest.FunSpec
import akka.testkit.{ ImplicitSender, TestKit, TestActorRef }
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
      val ref = TestActorRef(new MessageSplitterActor)
      ref ! PossiblyMultipleMetricRawString("aaa\nbbb")
      expectMsgAllConformingOf(1 second, classOf[SingleMetricRawString], classOf[SingleMetricRawString]) must be(Seq(SingleMetricRawString("aaa"), SingleMetricRawString("bbb")))

    }

    it("should handle a single metric  string and emit one SingleMetricRawString") {
      val ref = TestActorRef(new MessageSplitterActor)
      val future = ref ? PossiblyMultipleMetricRawString("aaa")
      val Success(v: SingleMetricRawString) = future.value.get
      v must be(SingleMetricRawString("aaa"))
    }

  }

  val b = SimpleBucket("test.bucket")
  describe("Raw metric Decoder actor") {
    val ref = TestActorRef(new MetricDecoderActor)

    it("should decode counter metrics") {
      ref ! SingleMetricRawString("test.bucket:1|c")
      expectMsg(10 millis, new Increment(b, 1) with Counter)
    }

    it("should decode timing metrics") {
      ref ! SingleMetricRawString("test.bucket:100|ms")
      expectMsg(10 millis, new SetValue(b, 100) with Timing)

    }

    it("should decode gauge setting metrics") {
      ref ! SingleMetricRawString("test.bucket:100|g")
      expectMsg(10 millis, new SetValue(b, 100) with Gauge)

    }

    it("should decode Gauge updating metrics (dec)") {
      ref ! SingleMetricRawString("test.bucket:-100|g")
      expectMsg(10 millis, new Increment(b, -100) with Gauge)
    }

    it("should decode Gauge updating metrics (inc)") {
      ref ! SingleMetricRawString("test.bucket:+100|g")
      expectMsg(10 millis, new Increment(b, 100) with Gauge)
    }
    it("should decode Distinct setting metrics") {
      ref ! SingleMetricRawString("test.bucket:100|s")
      expectMsg(10 millis, new SetValue(b, 100) with Distinct)

    }

  }
  val flushOp: Flush = Flush(b, System.currentTimeMillis)

  describe("Gauge Style Worker actor ") {
    it("should set the gauge with a SetValue") {
      val ref = TestActorRef(new GaugeAggregatorWorkerActor)
      ref ! new SetValue(b, 42)
      ref.underlyingActor.value must be(42)
    }
    it("should increment the gauge with a Increment") {
      val ref = TestActorRef(new GaugeAggregatorWorkerActor)
      ref ! new Increment(b, 1)
      ref.underlyingActor.value must be(1)
    }

    it("should not reset the value on flush") {
      val ref = TestActorRef(new GaugeAggregatorWorkerActor)
      ref ! new Increment(b, 1)
      ref.underlyingActor.value must be(1)
      ref ! flushOp
      ref.underlyingActor.value must be(1)

    }
  }

  describe("Counter Style Worker actor ") {
    it("should NOT set the counter with a SetValue") {
      val ref = TestActorRef(new CounterAggregatorWorkerActor)
      ref ! new SetValue(b, 42)
      ref.underlyingActor.value must be(0)
    }
    it("should increment the gauge with a Increment") {
      val ref = TestActorRef(new CounterAggregatorWorkerActor)
      ref ! new Increment(b, 42)
      ref.underlyingActor.value must be(42)
    }
    it("should reset the value on flush") {
      val ref = TestActorRef(new CounterAggregatorWorkerActor)
      ref ! new Increment(b, 1)
      ref.underlyingActor.value must be(1)
      ref ! flushOp
      ref.underlyingActor.value must be(0)

    }

  }

  describe("Timing Style Worker actor ") {
    it("should set the timing  with a SetValue") {
      val ref = TestActorRef(new TimingAggregatorWorkerActor)
      ref ! new SetValue(b, 42)
      ref.underlyingActor.value must be(42)
    }
    it("should NOT increment the timing with a Increment") {
      val ref = TestActorRef(new TimingAggregatorWorkerActor)
      ref ! new Increment(b, 42)
      ref.underlyingActor.value must be(0)
    }
    it("should reset the value on flush") {
      val ref = TestActorRef(new TimingAggregatorWorkerActor)
      ref ! new SetValue(b, 1)
      ref.underlyingActor.value must be(1)
      ref ! flushOp
      ref.underlyingActor.value must be(0)

    }
  }

  describe("Distinct Style Worker actor ") {
    it("should set the distinct  with a SetValue") {
      val ref = TestActorRef(new DistinctAggregatorWorkerActor)
      ref ! new SetValue(b, 42)
      ref.underlyingActor.value must be(1)
      ref ! new SetValue(b, 42)
      ref.underlyingActor.value must be(1)
      ref ! new SetValue(b, 123)
      ref.underlyingActor.value must be(2)

    }
    it("should NOT increment the gauge with a Increment") {
      val ref = TestActorRef(new DistinctAggregatorWorkerActor)
      ref ! new Increment(b, 42)
      ref.underlyingActor.value must be(0)
    }
    it("should reset the value on flush") {
      val ref = TestActorRef(new DistinctAggregatorWorkerActor)
      ref ! new SetValue(b, 1)
      ref.underlyingActor.value must be(1)
      ref ! flushOp
      ref.underlyingActor.value must be(0)

    }

  }

  describe("Metric Aggregator Actor") {
    it("should start with an empty list of active buckets") {
      val ref = TestActorRef(new MetricAggregatorActor)
      val future = ref ? Messages.BucketListQuery()
      val Success(v: Messages.BucketListResponse) = future.value.get
      v.buckets must have size (0)
    }

    it("should maintain a list of active buckets") {
      val ref = TestActorRef(new MetricAggregatorActor)
      ref ! new SetValue(b, 1) with Gauge
      val future = ref ? Messages.BucketListQuery()
      val Success(v: Messages.BucketListResponse) = future.value.get
      v.buckets must have size (1)
      v.buckets must contain("G." + b.name)
    }
  }

}
