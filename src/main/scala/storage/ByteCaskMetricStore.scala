package storage

import gj.metric.Metric
import com.github.bytecask.Bytecask


/**
 * In memory metric store
 */
trait ByteCaskMetricStore[T <: Metric] extends MetricStore[T] {
  private[this] var bcstore = new Bytecask(metric.bucket.name + ".mst")

  import com.github.bytecask.Bytes._

  def store(time: Long, value: T#Value) = {
    // FIXME non optimal storage as string. We need to define a proper serialisation scheme
    bcstore.put(time.toString, value.toString)
  }


  override def fetch(from: Option[Long], to: Option[Long]): Seq[(Long, T#Value)] = {
    val keys = bcstore.keys().map(_.asString.toLong)
      .filter(e => isInRange(from, to)(e))
      .toList
      .sorted(Ordering.Long.reverse)

    (for (k <- keys;
           v <- bcstore.get(k.toString))
      yield (k ->tos(v.asString))
      ).toSeq
  }

   def tos(v: String): T#Value = ???

}
