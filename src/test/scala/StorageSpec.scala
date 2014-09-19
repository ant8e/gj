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

import gj.metric.{ SimpleBucket, LongCounter, _ }
import org.scalatest.{ Matchers, FunSpec }
import scodec.Codec
import scodec.bits.BitVector
import storage.TempFuDB.Record
import storage.{ MemoryMetricStore, MetricStore }

import scalaz.{ \/-, \/ }

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

class TempFuCodecSpec extends FunSpec with Matchers {
  import storage.TempFuDB.TempFuCodec._
  describe("the codec ") {
    it("should encode records") {
      val encode: \/[String, BitVector] = Codec.encode(Record(0, 0))
      encode shouldBe ('right)
      val vector: BitVector = encode.toOption.get

      vector shouldBe BitVector.fromHex("0000000000000000").get

    }
  }
}