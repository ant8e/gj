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

case class NodeBucket(leafName: String, parent: Option[HierarchicalBucket], children: Seq[HierarchicalBucket]) extends HierarchicalBucket {
}

/**
 * Operations on Metrics
 */
sealed trait MetricOperation {
  /**
   * A Operation is always on a specific bucket
   */
  val bucket: Bucket
}

object MetricOperation {

  /**
   * Increment Operation increase (or decrease the value of metric)
   * @param bucket
   * @param increment
   */
  case class Increment(bucket: Bucket, increment: Int) extends MetricOperation

  /**
   * SetValue operation sets the value of a Metric
   * @param bucket
   * @param value
   */
  case class SetValue(bucket: Bucket, value: Int) extends MetricOperation

  /**
   * Flush the current Metric value :
   * push the current value to the aggregate history
   * and reset the value to the default according to the metric style
   *
   * @param bucket
   * @param millis timestamp of the flush op
   */
  case class Flush(bucket: Bucket, millis: Long) extends MetricOperation

}

/**
 * Metric style indicate the meaning of a metric
 */
sealed trait MetricStyle {
  /**
   * the bucket
   */
  val bucket: Bucket
  /**
   * a tag for the Metric style (unique)
   */
  val styleTag: String
}

object MetricStyle {

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

}