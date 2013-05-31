
import akka.actor.ActorSystem
import akka.util.Timeout
import org.scalatest.FunSpec
import akka.testkit.{ImplicitSender, TestKit, TestActorRef}
import akka.pattern.ask
import org.scalatest.matchers.MustMatchers
import scala.concurrent.duration._
import scala.util.Success


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


  describe("Raw metric Decoder actor") {
    val ref = TestActorRef(new MetricDecoderActor)
    val b = SimpleBucket("toto")

    it("should decode counter metrics") {
      ref ! SingleMetricRawString("toto:1|c")
      expectMsg(10 millis, new IncOp(b, 1) with CounterStyle)
    }

    it("should decode timing metrics") {
      ref ! SingleMetricRawString("toto:100|ms")
      expectMsg(10 millis, new SetOp(b, 100) with TimingStyle)

    }

    it("should decode gauge setting metrics") {
      ref ! SingleMetricRawString("toto:100|g")
      expectMsg(10 millis, new SetOp(b, 100) with GaugeStyle)

    }

    it("should decode Gauge updating metrics (dec)") {
      ref ! SingleMetricRawString("toto:-100|g")
      expectMsg(10 millis, new IncOp(b, -100) with GaugeStyle)
    }

    it("should decode Gauge updating metrics (inc)") {
      ref ! SingleMetricRawString("toto:+100|g")
      expectMsg(10 millis, new IncOp(b, 100) with GaugeStyle)
    }
    it("should decode Distinct setting metrics") {
      ref ! SingleMetricRawString("toto:100|s")
      expectMsg(10 millis, new SetOp(b, 100) with DistinctStyle)

    }


  }
}
