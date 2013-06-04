

trait Bucket {
  def name: String
}

case class SimpleBucket(name: String) extends Bucket

sealed trait MetricOp {
  val bucket: Bucket
}

case class IncOp(bucket: Bucket, increment: Int) extends MetricOp

case class SetOp(bucket: Bucket, value: Int) extends MetricOp

sealed trait MetricStyle {
  val bucket: Bucket
}

trait GaugeStyle extends MetricStyle

trait CounterStyle extends MetricStyle

trait TimingStyle extends MetricStyle

trait DistinctStyle extends MetricStyle

