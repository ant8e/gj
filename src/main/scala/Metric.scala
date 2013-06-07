

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
  val styleTag: String
}

trait GaugeStyle extends MetricStyle {
  val styleTag = "G"
}

trait CounterStyle extends MetricStyle {
  val styleTag = "C"
}

trait TimingStyle extends MetricStyle {
  val styleTag = "T"
}

trait DistinctStyle extends MetricStyle {
  val styleTag = "D"
}
