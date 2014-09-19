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

import gj.metric._

import scala.concurrent.duration.Duration

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


// Tempus Fugit time series database
class TempFuDB(file: File) {

}

object TempFuDB {
  def apply(file: File) = new TempFuDB(file)

  def newdb(file: File, retention: Duration) = {

  }

  case class Record(timeStamp: Long, value: Long)

  case class Header(retention: Duration, offset: Long)

  object TempFuCodec {

    import scodec.codecs._

    implicit val record = {
      ("time_stamp" | uint32) ::
        ("value" | uint32)
    }.as[Record]
  }

}