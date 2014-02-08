package gj

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
sealed abstract trait MetricStyle {

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

trait MetricType {
  type Value
}

trait Metric extends MetricStyle with MetricType {
  /**
   * the bucket
   */
  val bucket: Bucket
}

trait LongMetricType extends MetricType {
  type Value = Long
}

sealed case class LongGauge(bucket: Bucket) extends Metric with Gauge with LongMetricType

sealed case class LongCounter(bucket: Bucket) extends Metric with Counter with LongMetricType

sealed case class LongTiming(bucket: Bucket) extends Metric with Timing with LongMetricType

sealed case class LongDistinct(bucket: Bucket) extends Metric with Distinct with LongMetricType

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
sealed abstract class MetricOperation[+T <: Metric] {
  val metric: T
}

/**
 * Increment Operation increase (or decrease the value of metric)
 * @param metric
 * @param increment
 */
case class Increment[T <: Metric](metric: T, increment: T#Value) extends MetricOperation[T] {
  //  def   apply[T <: Metric : ClassTag] (b:Bucket, increment:T#Value) = new Increment[T](implicitly[ClassTag[T]].runtimeClass.getConstructor(classOf[Bucket]).newInstance(b), increment )
}

/**
 * SetValue operation sets the value of a Metric
 * @param metric
 * @param value
 */
case class SetValue[+T <: Metric](metric: T, value: T#Value) extends MetricOperation[T]

/**
 * Flush the current Metric value :
 * push the current value to the aggregate history
 * and reset the value to the default according to the metric style
 *
 * @param metric
 * @param millis timestamp of the flush op
 */
case class Flush[+T <: Metric](metric: T, millis: Long) extends MetricOperation[T]
