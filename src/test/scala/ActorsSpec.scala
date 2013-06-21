

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
      expectMsg(10 millis, new IncOp(b, 1) with CounterStyle)
    }

    it("should decode timing metrics") {
      ref ! SingleMetricRawString("test.bucket:100|ms")
      expectMsg(10 millis, new SetOp(b, 100) with TimingStyle)

    }

    it("should decode gauge setting metrics") {
      ref ! SingleMetricRawString("test.bucket:100|g")
      expectMsg(10 millis, new SetOp(b, 100) with GaugeStyle)

    }

    it("should decode Gauge updating metrics (dec)") {
      ref ! SingleMetricRawString("test.bucket:-100|g")
      expectMsg(10 millis, new IncOp(b, -100) with GaugeStyle)
    }

    it("should decode Gauge updating metrics (inc)") {
      ref ! SingleMetricRawString("test.bucket:+100|g")
      expectMsg(10 millis, new IncOp(b, 100) with GaugeStyle)
    }
    it("should decode Distinct setting metrics") {
      ref ! SingleMetricRawString("test.bucket:100|s")
      expectMsg(10 millis, new SetOp(b, 100) with DistinctStyle)

    }

  }
  val flushOp: FlushOp = FlushOp(b, System.currentTimeMillis)

  describe("Gauge Style Worker actor ") {
    it("should set the gauge with a SetOp") {
      val ref = TestActorRef(new GaugeAggregatorWorkerActor)
      ref ! new SetOp(b, 42)
      ref.underlyingActor.value must be(42)
    }
    it("should increment the gauge with a IncOp") {
      val ref = TestActorRef(new GaugeAggregatorWorkerActor)
      ref ! new IncOp(b, 1)
      ref.underlyingActor.value must be(1)
    }

    it ("should not reset the value on flush"){
      val ref = TestActorRef(new GaugeAggregatorWorkerActor)
      ref ! new IncOp(b, 1)
      ref.underlyingActor.value must be(1)
      ref !   flushOp
      ref.underlyingActor.value must be(1)

    }
  }

  describe("Counter Style Worker actor ") {
    it("should NOT set the counter with a SetOp") {
      val ref = TestActorRef(new CounterAggregatorWorkerActor)
      ref ! new SetOp(b, 42)
      ref.underlyingActor.value must be(0)
    }
    it("should increment the gauge with a IncOp") {
      val ref = TestActorRef(new CounterAggregatorWorkerActor)
      ref ! new IncOp(b, 42)
      ref.underlyingActor.value must be(42)
    }
    it ("should reset the value on flush"){
      val ref = TestActorRef(new CounterAggregatorWorkerActor)
      ref ! new IncOp(b, 1)
      ref.underlyingActor.value must be(1)
      ref !   flushOp
      ref.underlyingActor.value must be(0)

    }

  }

  describe("Timing Style Worker actor ") {
    it("should set the timing  with a SetOp") {
      val ref = TestActorRef(new TimingAggregatorWorkerActor)
      ref ! new SetOp(b, 42)
      ref.underlyingActor.value must be(42)
    }
    it("should NOT increment the timing with a IncOp") {
      val ref = TestActorRef(new TimingAggregatorWorkerActor)
      ref ! new IncOp(b, 42)
      ref.underlyingActor.value must be(0)
    }
    it ("should reset the value on flush"){
      val ref = TestActorRef(new TimingAggregatorWorkerActor)
      ref ! new SetOp(b, 1)
      ref.underlyingActor.value must be(1)
      ref !   flushOp
      ref.underlyingActor.value must be(0)

    }
  }

  describe("Distinct Style Worker actor ") {
    it("should set the distinct  with a SetOp") {
      val ref = TestActorRef(new DistinctAggregatorWorkerActor)
      ref ! new SetOp(b, 42)
      ref.underlyingActor.value must be(1)
      ref ! new SetOp(b, 42)
      ref.underlyingActor.value must be(1)
      ref ! new SetOp(b, 123)
      ref.underlyingActor.value must be(2)

    }
    it("should NOT increment the gauge with a IncOp") {
      val ref = TestActorRef(new DistinctAggregatorWorkerActor)
      ref ! new IncOp(b, 42)
      ref.underlyingActor.value must be(0)
    }
    it ("should reset the value on flush"){
      val ref = TestActorRef(new DistinctAggregatorWorkerActor)
      ref ! new SetOp(b, 1)
      ref.underlyingActor.value must be(1)
      ref !   flushOp
      ref.underlyingActor.value must be(0)

    }

  }

}
