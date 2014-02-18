import gj.metric.{SimpleBucket, LongCounter, Metric}
import org.scalatest.FunSpec
import storage.{ByteCaskMetricStore, MemoryMetricStore, MetricStore}

/**
 *
 */
trait StorageSpec {
  self: FunSpec =>

  def storage[T <: Metric](s: => MetricStore[T], value: T#Value) = {
    it("should store value ") {
      s.store(0, value)
    }
    it("should fetch values") {
      s.store(10, value)
      assert(s.fetch(None) === Seq((10, value), (0, value)))
    }
    it("should fetch values ordered by timestamp") {
      s.store(5, value)
      assert(s.fetch(None) === Seq((10, value),(5, value), (0, value)))
    }


  }

}

class MemoryStoreSpec extends FunSpec with StorageSpec {

  describe("A MemoryStore") {
    val store = new MemoryMetricStore[LongCounter] {
      override lazy val metric : LongCounter = new LongCounter(SimpleBucket("test.bucket"))
    }
    it should behave like storage[LongCounter](store, 42L)
  }
}
class ByteCaskStoreSpec extends FunSpec with StorageSpec {

  describe("A ByteCaskStore ") {
    val store = new ByteCaskMetricStore[LongCounter] {
      override val metric: LongCounter = new LongCounter(SimpleBucket("test.bucket"))
      bcstore.keys().foreach(bcstore.delete (_))
    }
    it should behave like storage[LongCounter](store, 42L)
  }
}