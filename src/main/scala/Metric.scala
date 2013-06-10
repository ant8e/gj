
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
sealed trait MetricOp {
  /**
   * A Operation is always on a specific bucket
   */
  val bucket: Bucket
}

/**
 * Increment Operation increase (or decrease the value of metric)
 * @param bucket
 * @param increment
 */
case class IncOp(bucket: Bucket, increment: Int) extends MetricOp

/**
 * Set operation sets the value of a Metric
 * @param bucket
 * @param value
 */
case class SetOp(bucket: Bucket, value: Int) extends MetricOp

/**
 * Flush the current Metric value :
 * push the current value to the aggregate history
 * and reset the value to the default according to the metric style
 *
 * @param bucket
 * @param millis timestamp of the flush op
 */
case class FlushOp(bucket: Bucket, millis: Long) extends MetricOp


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

/**
 * A gauge is a value that changes over time,
 * it's value is not affected by a FlushOp
 */
trait GaugeStyle extends MetricStyle {
  val styleTag = "G"
}

/**
 * Counter
 * It's value increase or decrease between flush and reset to zero
 */
trait CounterStyle extends MetricStyle {
  val styleTag = "C"
}

/**
 * Timing,
 * it's value change between flush, while recording of mean / average / 90th percentile over the period
 * reset to 0
 */
trait TimingStyle extends MetricStyle {
  val styleTag = "T"
}

/**
 * Distinct (or Set), records over time the number of different values
 * Reset to zero at flush
 */
trait DistinctStyle extends MetricStyle {
  val styleTag = "D"
}
