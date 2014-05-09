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

import gj.metric._
import com.github.bytecask.Bytecask

/**
 * ByteCask Store
 */
trait ByteCaskMetricStore[T <: Metric] extends MetricStore[T] {
  protected[this] lazy val bcstore = new Bytecask(metric.bucket.name + ".mst")

  import com.github.bytecask.Bytes._

  def store(time: Long, value: T#Value) = {
    bcstore.put(time.toString, metric.valueByteEncoder(value.asInstanceOf[metric.Value]))
  }

  override def fetch(from: Option[Long], to: Option[Long]): Seq[(Long, T#Value)] = {
    val keys = bcstore.keys().map(_.asString.toLong)
      .filter(e ⇒ isInRange(from, to)(e))
      .toList
      .sorted(Ordering.Long.reverse)

    (for (
      k ← keys;
      v ← bcstore.get(k.toString)
    ) yield (k -> metric.valueByteDecoder(v))).toSeq
  }

}
