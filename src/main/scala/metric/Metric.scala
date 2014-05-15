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

package gj.metric

import java.nio.ByteBuffer
import scala.reflect.ClassTag
import java.lang.reflect.Constructor


object `package` {
  /**
   * A Metric is a combination of a identifier, a style, a and valuetype
   */
  type Metric = MetricId with MetricType with MetricStyle
}


/**
 * A Metric bucket
 */
trait Bucket {
  def name: String
}

case class SimpleBucket(name: String) extends Bucket

sealed trait HierarchicalBucket

case class LeafBucket(leafName: String, parent: HierarchicalBucket) extends HierarchicalBucket

case class NodeBucket(leafName: String, parent: Option[HierarchicalBucket], children: Seq[HierarchicalBucket]) extends HierarchicalBucket

/**
 * Metric style indicate the meaning of a metric
 */
sealed trait MetricStyle {

  /**
   * a tag for the Metric style (unique)
   */
  val styleTag: String

}

/**
 * A gauge is a value that changes over time,
 * it's value is not affected by a Flush
 */
trait Gauge extends MetricStyle {
  val styleTag = "G"
}

/**
 * Counter
 * It's value increase or decrease between flush and reset to zero
 */
trait Counter extends MetricStyle {
  val styleTag = "C"
}

/**
 * Timing,
 * it's value change between flush, while recording of mean / average / 90th percentile over the period
 * reset to 0
 */
trait Timing extends MetricStyle {
  val styleTag = "T"
}

/**
 * Distinct (or SetValue), records over time the number of different values
 * Reset to zero at flush
 */
trait Distinct extends MetricStyle {
  val styleTag = "D"
}

trait MetricId  {
  /**
   * the bucket
   */
  def bucket: Bucket
}

trait MetricType {
  type Value

  def valueByteEncoder(x: Value): Array[Byte]

  def valueByteDecoder(x: Array[Byte]): Value

}

trait LongMetricType extends MetricType {
  type Value = Long

  def valueByteEncoder(x: LongMetricType#Value): Array[Byte] = ByteBuffer.allocate(8).putLong(x).array()

  def valueByteDecoder(x: Array[Byte]): LongMetricType#Value = ByteBuffer.wrap(x).getLong

}

sealed case class LongGauge(bucket: Bucket) extends MetricId with Gauge with LongMetricType

sealed case class LongCounter(bucket: Bucket) extends MetricId with Counter with LongMetricType

sealed case class LongTiming(bucket: Bucket) extends MetricId with Timing with LongMetricType

sealed case class LongDistinct(bucket: Bucket) extends MetricId with Distinct with LongMetricType

/**
 * A Metric value as some point in time
 * @param metric
 * @param timestamp
 * @param value
 * @tparam M
 */
sealed case class MetricValueAt[M <: Metric](metric: M, timestamp: Long, value: M#Value)

/**
 * Operations on Metrics
 */
sealed trait MetricOperation[T <: Metric] {
  val metric: T
  val ts: Long
}

/**
 * Increment Operation increase (or decrease the value of metric)
 * @param metric
 * @param increment
 */
case class Increment[T <: Metric](metric: T, increment: T#Value, ts: Long) extends MetricOperation[T]

/**
 * SetValue operation sets the value of a Metric
 * @param metric
 * @param value
 */
case class SetValue[T <: Metric](metric: T, value: T#Value, ts: Long) extends MetricOperation[T]

/**
 * Flush the current Metric value :
 * push the current value to the aggregate history
 * and reset the value to the default according to the metric style
 *
 * @param metric
 * @param millis timestamp of the flush op
 */
case class Flush[T <: Metric](metric: T, ts: Long) extends MetricOperation[T]
