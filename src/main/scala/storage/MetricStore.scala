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

import gj.metric.Metric

/**
 * Stores a metric along with it's timestamp
 * @tparam T the type of the metric
 */
trait MetricStore[T <:Metric] {
  val metric: T

  /**
   * Store a value
   * @param time
   * @param value
   */
  def store(time: Long, value: T#Value): Unit

  def fetch(from: Option[Long], to: Option[Long] = None): Seq[(Long,T#Value)]

  protected def isInRange(from: Option[Long], to: Option[Long])(t: Long) = isAfter(t, from) && isBefore(t, to)

  protected def isAfter(v: Long, b: Option[Long]) = if (b.isDefined) v > b.get else true

  protected def isBefore(v: Long, b: Option[Long]) = if (b.isDefined) v < b.get else true
}
