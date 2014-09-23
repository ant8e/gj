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
import storage.{ TempFuDB, MemoryMetricStore, MetricStore }

import scala.util.Try
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

  describe("A Record codec") {

    it("should encode records") {
      val encode: \/[String, BitVector] = Codec.encode(Record(0, 0))
      encode shouldBe ('right)
      encode.toOption.get shouldBe BitVector.fromHex("0000000000000000").get
      Codec.encode(Record(1, 2)).toOption.get shouldBe BitVector.fromHex("0000000100000002").get
    }

    it("should decode records") {
      val dec: \/[String, (BitVector, Record)] = Codec.decode(BitVector.fromHex("0000000000000000").get)
      dec shouldBe ('right)

      val (b1, r1) = dec.toOption.get
      b1 shouldBe BitVector.empty
      r1 shouldBe Record(0, 0)

      val (b2, r2) = Codec.decode[Record](BitVector.fromHex("0000000100000002").get).toOption.get
      b2 shouldBe BitVector.empty
      r2 shouldBe Record(1, 2)
    }
  }
}

class TempFuDBSpec extends FunSpec with Matchers {

  import java.io.File
  import java.io.RandomAccessFile
  import storage.TempFuDB.TempFuCodec.headerCodec

  describe("A TempfuDB") {

    it("should be created") {

      import scala.concurrent.duration._
      val f = new File("test1.db")
      val db: Try[TempFuDB] = TempFuDB.newdb(f, 1.day)

      db.isSuccess shouldBe true

      for (d ← db) {
        d.close()
        val header = headerCodec.decodeValidValue(BitVector.fromChannel(new RandomAccessFile(f, "r").getChannel))
        new RandomAccessFile(f, "r").close()
        f.delete()
        header shouldBe TempFuDB.Header(1.day, 1.second, 0)

      }

    }
    it("should store records") {
      pending
    }
    it("should retrieve records") {
      pending
    }
    it("should roll overs records") {
      pending
    }

  }
}