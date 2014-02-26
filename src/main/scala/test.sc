/**
 * Created by ant on 23.02.14.
 */

class ObjectId(s: String) {

}

object ObjectId {
  def isValid(s: String) = true
}



val params = Map.empty[String, String]

val imageId2: Option[ObjectId] = params.get("imageId") match {
  case None => None
  case Some(x) => {
    if (ObjectId.isValid(x)) {
      Some(new ObjectId(x))
    } else {
      None
    }
  }
}

val imageId: Option[ObjectId] = for (x <- params.get("imageId")
                                     if ObjectId.isValid(x))
yield new ObjectId(x)

def convert(o: Option[String]): Option[String] = for (x <- o if !x.isEmpty) yield x

convert (Some(""))

convert (Some("xx"))