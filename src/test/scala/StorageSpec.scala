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

import gj.metric.{ SimpleBucket, LongCounter, Metric }
import org.scalatest.FunSpec
import storage.{ ByteCaskMetricStore, MemoryMetricStore, MetricStore }

/**
 *
 */
trait StorageSpec {
  self: FunSpec ⇒

  def storage[T <: Metric](s: ⇒ MetricStore[T], value: T#Value) = {
    it("should store value ") {
      s.store(0, value)
    }
    it("should fetch values") {
      s.store(10, value)
      assert(s.fetch(None) === Seq((10, value), (0, value)))
    }
    it("should fetch values ordered by timestamp") {
      s.store(5, value)
      assert(s.fetch(None) === Seq((10, value), (5, value), (0, value)))
    }

  }

}

class MemoryStoreSpec extends FunSpec with StorageSpec {

  describe("A MemoryStore") {
    val store = new MemoryMetricStore[LongCounter] {
      override lazy val metric: LongCounter = new LongCounter(SimpleBucket("test.bucket"))
    }
    it should behave like storage[LongCounter](store, 42L)
  }
}
class ByteCaskStoreSpec extends FunSpec with StorageSpec {

  describe("A ByteCaskStore ") {
    val store = new ByteCaskMetricStore[LongCounter] {
      override val metric: LongCounter = new LongCounter(SimpleBucket("test.bucket"))
      bcstore.keys().foreach(bcstore.delete(_))
    }
    it should behave like storage[LongCounter](store, 42L)
  }
}