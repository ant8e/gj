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

