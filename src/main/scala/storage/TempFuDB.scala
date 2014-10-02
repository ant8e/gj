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

import java.io.File

import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.util.Try

object TempFuDB {

  case class Record(timeStamp: Long, value: Long)

  case class Header(retention: Duration, interval: Duration, offset: Long)

  def apply(file: File) = new TempFuDB(file)

  def newdb(file: File, retention: Duration): Try[TempFuDB] = Try {
    import java.io.RandomAccessFile
    // Create an empty file of the desired size
    val f = new RandomAccessFile(file, "rw")
    f.setLength(TempFuCodec.headerByteLength + retention.toSeconds * TempFuCodec.recordByteLength)
    f.close()

    //Create and initialize the Db
    val dB: TempFuDB = TempFuDB(file)
    dB.writeHeader(Header(retention, 1.second, 0))
    dB
  }

  /**
   * Binary codecs for TempFuDb
   */
  object TempFuCodec {

    import scodec.Codec
    import scodec.bits.BitVector
    import scodec.codecs._

    val recordCodec = {
      ("time_stamp" | int64) ::
        ("value" | int64)
    }.as[Record]

    val recordByteLength: Long = recordCodec.encodeValid(Record(0, 0)).size

    val durationCodec = Codec[Duration](
      (d: Duration) ⇒ uint32.encode(d.toSeconds),
      (bv: BitVector) ⇒ uint32.decode(bv).map {
        case (rest, l) ⇒ (rest, Duration(l, SECONDS))
      })

    val headerCodec = {
      ("retention" | durationCodec) ::
        ("interval" | durationCodec) ::
        ("offset" | uint32)
    }.as[Header]

    val headerByteLength: Long = headerCodec.encodeValid(Header(1.second, 1.second, 0)).size / 8
  }

}

/**
 * Support the Tempus Fugit file format
 *
 * Warning : Not ThreadSafe
 */
final class TempFuDB(file: File) {

  import java.io.RandomAccessFile
  import java.nio.channels.FileChannel

  import TempFuDB.TempFuCodec._
  import scodec.bits.BitVector
  import storage.TempFuDB.{Header, Record}

  private val raf = new RandomAccessFile(file, "rw")
  private val channel: FileChannel = raf.getChannel
  private val bitVector: BitVector = BitVector.fromMmap(channel)

  private var currentHeader = readHeader
  private var tsAtCurrentOffset = readRecordAt(currentHeader.offset) map {
    _.timeStamp
  }

  private def readHeader = headerCodec.decodeValidValue(bitVector.take(headerByteLength * 8))

  private def writeHeader(h: Header): Unit = {
    val headerBits: BitVector = headerCodec.encodeValid(h)
    channel.write(headerBits.toByteBuffer, 0)
    currentHeader = h
  }

  private def readRecordAt(offset: Long): Option[Record] = {
    channel.position(recordPositionInByte(offset))
    val t = recordCodec.decodeValidValue(BitVector.fromChannel(channel).take(recordByteLength * 8))
    // Todo refactor codec to handle this case
    if (t.timeStamp > 0)
      Some(t)
    else
      None
  }

  def readRecord(ts: Long): Option[Record] = {
    val readPos = tsAtCurrentOffset.map { ts0 ⇒ (ts - ts0) / currentHeader.interval.toMillis}
    //TODO handle cases where the ts fall outside of the file range
    for (p <- readPos;
      r <- readRecordAt(p))
    yield r
  }

  private def writeRecordAt(r: Record, recordPos: Long): Unit = {
    val headerBits: BitVector = recordCodec.encodeValid(r)
    channel.write(headerBits.toByteBuffer, recordPositionInByte(recordPos))
  }

  private def recordPositionInByte(offset: Long): Long = headerByteLength + offset * recordByteLength

  def push(ts: Long, v: Long) = {
    val writepos = tsAtCurrentOffset.map { ts0 ⇒ (ts - ts0) / currentHeader.interval.toMillis}
    writepos match {
      case Some(x) if x >= 0 ⇒
        writeRecordAt(Record(ts, v), x)
        tsAtCurrentOffset = Some(ts)

      case None ⇒
        writeRecordAt(Record(ts, v), 0)
        currentHeader = currentHeader.copy(offset = 0)
        writeHeader(currentHeader)
        tsAtCurrentOffset = Some(ts)

      case _ ⇒
    }
  }

  def close() = {
    channel.close()
    raf.close()
  }

}