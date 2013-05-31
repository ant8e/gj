


trait Bucket {
  def name: String
}

case class SimpleBucket(name: String) extends Bucket

sealed trait MetricOp {
  val b: Bucket
}

case class IncOp(val b: Bucket, increment: Int) extends MetricOp

case class SetOp(val b: Bucket, value: Int) extends MetricOp

sealed trait MetricStyle

trait GaugeStyle extends MetricStyle

trait CounterStyle extends MetricStyle

trait TimingStyle extends MetricStyle

trait DistinctStyle extends MetricStyle





