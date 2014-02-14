package storage

import gj.metric.Metric


/**
 * In memory metric store
 */
trait MemoryMetricStore[T <: Metric] extends MetricStore[T] {
  private[this] var memstore = Vector[(Long, T#Value)]()

  def store(time: Long, value: T#Value) = {
    memstore = (time, value) +: memstore
  }


  override def fetch(from: Option[Long], to: Option[Long]): Seq[(Long, T#Value)] =
    memstore
      .filter(e => isInRange(from, to)(e._1))
      .sortBy(_._1)(Ordering.Long.reverse)

}
