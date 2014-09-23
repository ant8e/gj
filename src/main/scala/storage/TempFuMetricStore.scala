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

package storage

import java.io.{ RandomAccessFile, File }
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

import gj.metric._
import scodec.Codec
import scodec.bits.BitVector
import storage.TempFuDB.Header

import scala.concurrent.duration._
import scala.concurrent.duration.Duration
import scala.util.Try

/**
 * In memory metric store
 */
trait TempFuMetricStore[T <: Metric] extends MetricStore[T] {
  private[this] var memstore = Vector[(Long, T#Value)]()

  def store(time: Long, value: T#Value) = {
    memstore = (time, value) +: memstore
  }

  override def fetch(from: Option[Long], to: Option[Long]): Seq[(Long, T#Value)] =
    memstore
      .filter(e ⇒ isInRange(from, to)(e._1))
      .sortBy(_._1)(Ordering.Long.reverse)

}

/**
 * Support the Tempus Fugit file format
 *
 * Warning : Not ThreadSafe
 */
class TempFuDB(file: File) {

  private val channel: FileChannel = new RandomAccessFile(file, "rw").getChannel
  private val bitVector: BitVector = BitVector.fromMmap(channel)

  import TempFuDB.TempFuCodec._

  def readHeader = headerCodec.decodeValidValue(bitVector.take(headerByteLength * 8))
  def writeHeader(h: Header): Unit = {
    val headerBits: BitVector = headerCodec.encodeValid(h)
    channel.write(headerBits.toByteBuffer, 0)
  }

  def close() = channel.close()

}

object TempFuDB {

  def apply(file: File) = new TempFuDB(file)

  def newdb(file: File, retention: Duration): Try[TempFuDB] = Try {

    val f = new RandomAccessFile(file, "rw")
    f.setLength(TempFuCodec.headerByteLength + retention.toSeconds * TempFuCodec.recordByteLength)
    f.close()

    val dB: TempFuDB = TempFuDB(file)
    dB.writeHeader(Header(retention, 1.second, 0))
    dB
  }

  case class Record(timeStamp: Long, value: Long)

  case class Header(retention: Duration, interval: Duration, offset: Long)

  object TempFuCodec {

    import scodec.codecs._

    implicit val recordCodec = {
      ("time_stamp" | uint32) ::
        ("value" | uint32)
    }.as[Record]

    val recordByteLength: Long = recordCodec.encode(Record(0, 0)).toOption.get.size

    val durationCodec = Codec[Duration](
      (d: Duration) ⇒ uint32.encode(d.toSeconds),
      (bv: BitVector) ⇒ uint32.decode(bv).map {
        case (rest, l) ⇒ (rest, Duration(l, SECONDS))
      })

    implicit val headerCodec = {
      ("retention" | durationCodec) ::
        ("interval" | durationCodec) ::
        ("offset" | uint32)
    }.as[Header]

    val headerByteLength: Long = headerCodec.encode(Header(1.second, 1.second, 0)).toOption.get.size / 8
  }

}