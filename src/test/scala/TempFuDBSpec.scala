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

import org.scalatest.{ Matchers, FunSpec }

class TempFuDBSpec extends FunSpec with Matchers {

  import java.io.{ File, RandomAccessFile }

  import storage.TempFuDB.TempFuCodec.headerCodec

  import storage.TempFuDB
  import scala.concurrent.duration._
  import scala.util.Try
  import scodec.bits.BitVector

  describe("A TempfuDB") {

    it("should be created") {

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

class TempFuCodecSpec extends FunSpec with Matchers {

  import storage.TempFuDB.TempFuCodec._
  import scodec.bits._
  import storage.TempFuDB.Record
  import scalaz.\/

  describe("A Record codec") {

    it("should encode records") {
      import storage.TempFuDB.Record

      val encode: \/[String, BitVector] = recordCodec.encode(Record(0, 0))
      encode shouldBe 'right
      encode.toOption.get shouldBe hex"0000000000000000".toBitVector
      recordCodec.encodeValid(Record(1, 2)) shouldBe hex"0000000100000002".toBitVector
    }

    it("should decode records") {

      import scalaz.\/
      val dec: \/[String, (BitVector, Record)] = recordCodec.decode(hex"0000000000000000".toBitVector)
      dec shouldBe 'right

      val (b1, r1) = dec.toOption.get
      b1 shouldBe BitVector.empty
      r1 shouldBe Record(0, 0)

      val (b2, r2) = recordCodec.decode(hex"0000000100000002".toBitVector).toOption.get
      b2 shouldBe BitVector.empty
      r2 shouldBe Record(1, 2)
    }
  }
  describe("A Header codec") {

    import storage.TempFuDB.Header
    import scala.concurrent.duration._
    it("should encode headers") {
      val encode: \/[String, BitVector] = headerCodec.encode(Header(10.second, 1.second, 2))
      encode shouldBe 'right
      encode.toOption.get shouldBe hex"0000000A0000000100000002".toBitVector
    }

    it("should decode records") {
      val dec: \/[String, (BitVector, Header)] = headerCodec.decode(hex"000000000000000000000000".toBitVector)
      dec shouldBe 'right

      val (b1, r1) = dec.toOption.get
      b1 shouldBe BitVector.empty
      r1 shouldBe Header(0.second, 0.second, 0)

      val (b2, r2) = headerCodec.decode(hex"000000010000000200000003".toBitVector).toOption.get
      b2 shouldBe BitVector.empty
      r2 shouldBe Header(1.second, 2.second, 3)
    }
  }
}

