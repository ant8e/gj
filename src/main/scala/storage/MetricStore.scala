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
