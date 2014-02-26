package storage

import gj.metric.Metric
import com.github.bytecask.Bytecask


/**
 * ByteCask Store
 */
trait ByteCaskMetricStore[T <: Metric] extends MetricStore[T] {
  protected[this] lazy val bcstore = new Bytecask(metric.bucket.name + ".mst")

  import com.github.bytecask.Bytes._

  def store(time: Long, value: T#Value) = {
    bcstore.put(time.toString, metric.valueByteEncoder(value.asInstanceOf[metric.Value]))
  }



  override def fetch(from: Option[Long], to: Option[Long]): Seq[(Long, T#Value)] = {
    val keys = bcstore.keys().map(_.asString.toLong)
      .filter(e => isInRange(from, to)(e))
      .toList
      .sorted(Ordering.Long.reverse)

    (for (k <- keys;
          v <- bcstore.get(k.toString))
    yield (k -> metric.valueByteDecoder(v))
      ).toSeq
  }





}
